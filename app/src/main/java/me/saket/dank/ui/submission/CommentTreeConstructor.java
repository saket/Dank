package me.saket.dank.ui.submission;


import static junit.framework.Assert.assertNotNull;
import static me.saket.dank.utils.Arrays2.immutable;

import android.support.annotation.CheckResult;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import me.saket.dank.data.PostedOrInFlightContribution;
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

  /** Contribution IDs for which inline replies are active. */
  static class ActiveReplyIds extends RxHashSet<String> {
    boolean isActive(Contribution parentContribution) {
      return contains(idForTogglingCollapse(parentContribution));
    }

    public boolean isActive(PostedOrInFlightContribution contribution) {
      return contains(contribution.idForTogglingCollapse());
    }

    public void showFor(PostedOrInFlightContribution parentContribution) {
      add(parentContribution.idForTogglingCollapse());
    }

    public void hideFor(PostedOrInFlightContribution parentContribution) {
      remove(parentContribution.idForTogglingCollapse());
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

    public boolean isCollapsed(String commentFullName) {
      return contains(commentFullName);
    }

    public boolean isCollapsed(Comment comment) {
      return isCollapsed(idForTogglingCollapse(comment));
    }

    public boolean isCollapsed(PostedOrInFlightContribution contribution) {
      return isCollapsed(contribution.idForTogglingCollapse());
    }

    public void expand(PostedOrInFlightContribution contribution) {
      boolean removed = remove(contribution.idForTogglingCollapse());
      if (!removed) {
        throw new AssertionError("This contribution isn't collapsed: " + contribution);
      }
    }

    public void collapse(PostedOrInFlightContribution contribution) {
      add(contribution.idForTogglingCollapse());
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
      return contains(idForTogglingCollapse(commentNode.getComment()));
    }

    public void showLoadMoreFor(PostedOrInFlightContribution parentContribution) {
      add(parentContribution.idForTogglingCollapse());
    }

    public void hideLoadMoreFor(PostedOrInFlightContribution parentContribution) {
      boolean removed = remove(parentContribution.idForTogglingCollapse());
      if (!removed) {
        throw new AssertionError("More comments weren't in flight for: " + parentContribution);
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
  public ObservableTransformer<Submission, List<SubmissionCommentRow>> stream() {
    return submissions -> {
      Observable<PendingSyncRepliesMap> pendingSyncRepliesMaps = submissions
          .distinctUntilChanged()
          .flatMap(submission -> replyRepository.streamPendingSyncReplies(ParentThread.of(submission)))
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
              rowVisibilityChanges,
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
  void toggleCollapse(PostedOrInFlightContribution contribution) {
    assertNotNull(contribution.idForTogglingCollapse());

    if (COLLAPSED_COMMENT_IDS.isCollapsed(contribution)) {
      COLLAPSED_COMMENT_IDS.expand(contribution);
    } else {
      COLLAPSED_COMMENT_IDS.collapse(contribution);
    }
  }

  boolean isCollapsed(PostedOrInFlightContribution contribution) {
    return COLLAPSED_COMMENT_IDS.isCollapsed(contribution);
  }

  /**
   * Enable "loading more…" progress indicator.
   *
   * @param parentContribution for which more child nodes are being fetched.
   */
  void setMoreCommentsLoading(PostedOrInFlightContribution parentContribution, boolean loading) {
    if (parentContribution.idForTogglingCollapse() == null) {
      throw new AssertionError();
    }

    if (loading) {
      IN_FLIGHT_LOAD_MORE_IDS.showLoadMoreFor(parentContribution);
    } else {
      IN_FLIGHT_LOAD_MORE_IDS.hideLoadMoreFor(parentContribution);
    }
  }

  /**
   * See {@link PostedOrInFlightContribution.ContributionFetchedFromRemote#idForTogglingCollapse()}
   */
  private static String idForTogglingCollapse(Contribution contribution) {
    return contribution.getFullName();
  }

  /**
   * Show reply field for a comment and also expand any hidden comments.
   */
  void showReplyAndExpandComments(PostedOrInFlightContribution parentContribution) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(parentContribution)) {
      COLLAPSED_COMMENT_IDS.pauseChangeEvents();
      COLLAPSED_COMMENT_IDS.expand(parentContribution);
      COLLAPSED_COMMENT_IDS.resumeChangeEvents();
    }

    ACTIVE_REPLY_IDS.showFor(parentContribution);
  }

  /**
   * Show reply field for the submission or a comment.
   */
  void showReply(PostedOrInFlightContribution parentContribution) {
    ACTIVE_REPLY_IDS.showFor(parentContribution);
  }

  /**
   * Hide reply field for a comment.
   */
  void hideReply(PostedOrInFlightContribution parentContribution) {
    ACTIVE_REPLY_IDS.hideFor(parentContribution);
  }

  boolean isReplyActiveFor(PostedOrInFlightContribution contribution) {
    return ACTIVE_REPLY_IDS.isActive(contribution);
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
        PendingSyncReply pendingSyncReply = pendingSyncReplies.get(i);
        String replyFullName = PostedOrInFlightContribution.idForTogglingCollapseForLocallyPostedReply(
            nextNode.getComment().getFullName(),
            pendingSyncReply.createdTimeMillis()
        );
        boolean isReplyCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(replyFullName);
        int depth = nextNode.getDepth() + 1;
        flattenComments.add(CommentPendingSyncReplyItem.create(nextNode.getComment(), replyFullName, pendingSyncReply, isReplyCollapsed, depth));
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
