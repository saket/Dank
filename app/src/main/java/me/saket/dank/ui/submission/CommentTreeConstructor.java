package me.saket.dank.ui.submission;


import static me.saket.dank.utils.Arrays2.immutable;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.data.LocallyPostedComment;
import me.saket.dank.utils.Preconditions;
import me.saket.dank.utils.RxHashSet;
import timber.log.Timber;

/**
 * Constructs comments to show in a submission. Ignores collapsed comments + adds reply fields + adds "load more"
 * & "continue thread ->" items.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class CommentTreeConstructor {

  private static final ActiveReplyIds ACTIVE_REPLY_IDS = new ActiveReplyIds();
  private static final CollapsedCommentIds COLLAPSED_COMMENT_IDS = new CollapsedCommentIds(50);
  private static final InFlightLoadMoreIds IN_FLIGHT_LOAD_MORE_IDS = new InFlightLoadMoreIds();

  private final ReplyRepository replyRepository;

  private static String keyFor(Contribution contribution) {
    if (contribution instanceof Submission) {
      return contribution.getFullName();
    }
    if (contribution instanceof ContributionFullNameWrapper) {
      Timber.i("Huh?");
      return contribution.getFullName();
    }
    if (contribution instanceof LocallyPostedComment) {
      String key = ((LocallyPostedComment) contribution).getPostingStatusIndependentId();
      return Preconditions.checkNotNull(key, "LocallyPostedComment#getPostingStatusIndependentId()");
    }
    if (contribution instanceof Comment) {
      return contribution.getFullName();
    }
    throw new UnsupportedOperationException("Unknown contribution: " + contribution);
  }

  /** Contribution IDs for which inline replies are active. */
  static class ActiveReplyIds extends RxHashSet<String> {
    public boolean isActive(Contribution parentContribution) {
      return contains(keyFor(parentContribution));
    }

    public void showFor(Contribution parentContribution) {
      add(keyFor(parentContribution));
    }

    public void hideFor(Contribution parentContribution) {
      remove(keyFor(parentContribution));
    }
  }

  /** Comment IDs that are collapsed. */
  static class CollapsedCommentIds extends RxHashSet<String> {
    private boolean changeEventsEnabled = true;

    public CollapsedCommentIds(int initialCapacity) {
      super(initialCapacity);
    }

    @Override
    public Observable<Integer> changes() {
      return super
          .changes()
          .filter(o -> {
            if (!changeEventsEnabled) {
              Timber.w("Filtering collapse change event");
            }
            return changeEventsEnabled;
          });
    }

    public boolean isCollapsed(Comment comment) {
      return contains(keyFor(comment));
    }

    public void expand(Comment comment) {
      boolean removed = remove(keyFor(comment));
      if (!removed) {
        throw new AssertionError("This comment isn't collapsed: " + comment);
      }
    }

    public void collapse(Comment comment) {
      add(keyFor(comment));
    }

    public void pauseChangeEvents() {
      changeEventsEnabled = false;
    }

    public void resumeChangeEvents() {
      changeEventsEnabled = true;
    }
  }

  /** Comment IDs for which more child comments are being fetched. */
  static class InFlightLoadMoreIds extends RxHashSet<String> {
    public boolean isInFlightFor(CommentNode commentNode) {
      return contains(keyFor(commentNode.getComment()));
    }

    public void showLoadMoreFor(Comment parentComment) {
      add(keyFor(parentComment));
    }

    public void hideLoadMoreFor(Comment parentComment) {
      boolean removed = remove(keyFor(parentComment));
      if (!removed) {
        throw new AssertionError("More comments weren't in flight for: " + parentComment);
      }
    }
  }

  class PendingSyncRepliesMap extends HashMap<String, List<PendingSyncReply>> {
    @Override
    public List<PendingSyncReply> put(String key, List<PendingSyncReply> value) {
      return super.put(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
      return super.remove(key, value);
    }

    public boolean hasForParent(Comment parentComment) {
      return containsKey(parentComment.getFullName());
    }

    public List<PendingSyncReply> getForParent(Comment parentComment) {
      return get(parentComment.getFullName());
    }
  }

  @Inject
  public CommentTreeConstructor(ReplyRepository replyRepository) {
    this.replyRepository = replyRepository;
  }

  @CheckResult
  public ObservableTransformer<Submission, List<SubmissionCommentRow>> stream(Scheduler scheduler) {
    return submissions -> {
      Observable<PendingSyncRepliesMap> pendingSyncRepliesMaps = submissions
          .distinctUntilChanged()
          .flatMap(submission -> replyRepository.streamPendingSyncReplies(ParentThread.of(submission)))
          // I've seen this stream very occasionally take a second to emit, so starting with a default value.
          .startWith(Collections.<PendingSyncReply>emptyList())
          .map(replyList -> createPendingSyncReplyMap(replyList));

      Observable<?> rowVisibilityChanges = Observable.merge(
          ACTIVE_REPLY_IDS.changes(),
          COLLAPSED_COMMENT_IDS.changes(),
          IN_FLIGHT_LOAD_MORE_IDS.changes()
      );

      return Observable
          .combineLatest(
              submissions,
              pendingSyncRepliesMaps,
              rowVisibilityChanges.observeOn(scheduler),    // observeOn() because they emit on the main thread.
              (submission, pendingSyncRepliesMap, o) -> constructComments(submission, pendingSyncRepliesMap))
          .as(immutable());
    };
  }

  // Key: comment full-name.
  private PendingSyncRepliesMap createPendingSyncReplyMap(List<PendingSyncReply> pendingReplies) {
    PendingSyncRepliesMap pendingReplyMap = new PendingSyncRepliesMap();

    for (PendingSyncReply pendingSyncReply : pendingReplies) {
      String parentCommentFullName = pendingSyncReply.parentContributionFullName();

      if (!pendingReplyMap.containsKey(parentCommentFullName)) {
        pendingReplyMap.put(parentCommentFullName, new ArrayList<>(4));
      }
      List<PendingSyncReply> existingReplies = pendingReplyMap.get(parentCommentFullName);
      existingReplies.add(pendingSyncReply);
      pendingReplyMap.put(parentCommentFullName, existingReplies);
    }
    return pendingReplyMap;
  }

  /**
   * Collapse/expand a comment.
   */
  void toggleCollapse(Comment comment) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(comment)) {
      COLLAPSED_COMMENT_IDS.expand(comment);
    } else {
      COLLAPSED_COMMENT_IDS.collapse(comment);
    }
  }

  boolean isCollapsed(Comment contribution) {
    return COLLAPSED_COMMENT_IDS.isCollapsed(contribution);
  }

  /**
   * Enable "loading more…" progress indicator.
   *
   * @param parentComment for which more child nodes are being fetched.
   */
  void setMoreCommentsLoading(Comment parentComment, boolean loading) {
    if (loading) {
      IN_FLIGHT_LOAD_MORE_IDS.showLoadMoreFor(parentComment);
    } else {
      IN_FLIGHT_LOAD_MORE_IDS.hideLoadMoreFor(parentComment);
    }
  }

  /**
   * Show reply field for a comment and also expand any hidden comments.
   */
  void showReplyAndExpandComments(Comment parentComment) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(parentComment)) {
      COLLAPSED_COMMENT_IDS.pauseChangeEvents();
      COLLAPSED_COMMENT_IDS.expand(parentComment);
      COLLAPSED_COMMENT_IDS.resumeChangeEvents();
    }

    ACTIVE_REPLY_IDS.showFor(parentComment);
  }

  /**
   * Show reply field for the submission or a comment.
   */
  void showReply(Contribution parentContribution) {
    ACTIVE_REPLY_IDS.showFor(parentContribution);
  }

  /**
   * Hide reply field for a comment.
   */
  void hideReply(Contribution parentContribution) {
    ACTIVE_REPLY_IDS.hideFor(parentContribution);
  }

  boolean isReplyActiveFor(Contribution contribution) {
    return ACTIVE_REPLY_IDS.isActive(contribution);
  }

  public boolean isMoreCommentsInFlightFor(CommentNode commentNode) {
    return IN_FLIGHT_LOAD_MORE_IDS.isInFlightFor(commentNode);
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments(Submission submission, PendingSyncRepliesMap pendingSyncRepliesMap) {
    int totalRowsSize = 0;
    if (ACTIVE_REPLY_IDS.isActive(submission)) {
      totalRowsSize += 1;
    }

    CommentNode rootCommentNode = submission.getComments();
    if (rootCommentNode != null) {
      totalRowsSize += rootCommentNode.getTotalSize();
    }

    ArrayList<SubmissionCommentRow> flattenComments = new ArrayList<>(totalRowsSize);
    if (ACTIVE_REPLY_IDS.isActive(submission)) {
      flattenComments.add(CommentInlineReplyItem.create(submission, 0));
    }

    if (rootCommentNode == null) {
      return flattenComments;
    } else {
      return constructComments(flattenComments, rootCommentNode, submission, pendingSyncRepliesMap);
    }
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments(
      List<SubmissionCommentRow> flattenComments,
      CommentNode nextNode,
      Submission submission,
      PendingSyncRepliesMap pendingSyncRepliesMap)
  {
    //String indentation = "";
    //if (nextNode.getDepth() != 0) {
    //  for (int step = 0; step < nextNode.getDepth(); step++) {
    //    indentation += "  ";
    //  }
    //}

    boolean isCommentNodeCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(nextNode.getComment());
    boolean isReplyActive = ACTIVE_REPLY_IDS.isActive(nextNode.getComment());

    if (nextNode.getDepth() != 0) {
      //Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
      flattenComments.add(DankCommentNode.create(nextNode, isCommentNodeCollapsed));
    }

    // Reply box.
    // Skip for root-node because we already added a reply for the submission in constructComments().
    boolean isSubmission = nextNode.getComment().getFullName().equals(submission.getFullName());
    if (!isSubmission && isReplyActive && !isCommentNodeCollapsed) {
      flattenComments.add(CommentInlineReplyItem.create(nextNode.getComment(), nextNode.getDepth()));
    }

    // Pending-sync replies.
    if (!isCommentNodeCollapsed && pendingSyncRepliesMap.hasForParent(nextNode.getComment())) {
      List<PendingSyncReply> pendingSyncReplies = pendingSyncRepliesMap.getForParent(nextNode.getComment());
      for (int i = 0; i < pendingSyncReplies.size(); i++) {     // Intentionally avoiding thrashing Iterator objects.
        LocallyPostedComment locallyPostedComment = LocallyPostedComment.create(pendingSyncReplies.get(i));
        boolean isReplyCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(locallyPostedComment);
        int depth = nextNode.getDepth() + 1;
        flattenComments.add(DankLocallyPostedCommentItem.create(locallyPostedComment, isReplyCollapsed, depth));
      }
    }

    // Next, the child comment tree.
    if (nextNode.isEmpty() && !nextNode.hasMoreComments()) {
      return flattenComments;

    } else {
      // Ignore collapsed children.
      if (!isCommentNodeCollapsed) {
        List<CommentNode> childCommentsTree = nextNode.getChildren();
        for (int i = 0; i < childCommentsTree.size(); i++) {  // Intentionally avoiding thrashing Iterator objects.
          CommentNode node = childCommentsTree.get(i);
          constructComments(flattenComments, node, submission, pendingSyncRepliesMap);
        }

        if (nextNode.hasMoreComments()) {
          //Timber.d("%s(%s) %s has %d MORE ---------->",
          //    indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
          //);
          //Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
          String nextNodeFullname = nextNode.getComment().getFullName();
          flattenComments.add(LoadMoreCommentItem.create(nextNodeFullname, nextNode, IN_FLIGHT_LOAD_MORE_IDS.isInFlightFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }
}
