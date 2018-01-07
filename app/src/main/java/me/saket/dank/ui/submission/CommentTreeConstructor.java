package me.saket.dank.ui.submission;


import static junit.framework.Assert.assertNotNull;
import static me.saket.dank.utils.Commons.toImmutable;

import android.support.annotation.CheckResult;

import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.data.PostedOrInFlightContribution;

/**
 * Constructs comments to show in a submission. Ignores collapsed comments + adds reply fields + adds "load more"
 * & "continue thread ->" items.
 */
public class CommentTreeConstructor {

  private static final Set<String> collapsedContributionIds = new HashSet<>(50); // Comments that are collapsed.
  private static final Set<String> loadingMoreContributionIds = new HashSet<>();                // Comments for which more replies are being fetched.
  private static final Set<String> replyActiveForContributionIds = new HashSet<>();             // Comments for which reply fields are active.
  private final Map<String, List<PendingSyncReply>> pendingReplyMap = new HashMap<>();               // Key: comment full-name.
  private final Relay<Object> changesRequiredStream = PublishRelay.create();
  private CommentNode rootCommentNode;
  private Submission submission;

  @Inject
  public CommentTreeConstructor() {
  }

  public void update(Submission submission, CommentNode rootCommentNode, List<PendingSyncReply> pendingReplies) {
    this.submission = submission;
    this.rootCommentNode = rootCommentNode;

    // Build a map of pending replies so that they can later be accessed at o(1).
    pendingReplyMap.clear();
    for (PendingSyncReply pendingSyncReply : pendingReplies) {
      String parentCommentFullName = pendingSyncReply.parentContributionFullName();

      if (!pendingReplyMap.containsKey(parentCommentFullName)) {
        pendingReplyMap.put(parentCommentFullName, new ArrayList<>());
      }

      List<PendingSyncReply> existingReplies = pendingReplyMap.get(parentCommentFullName);
      existingReplies.add(pendingSyncReply);
      pendingReplyMap.put(parentCommentFullName, existingReplies);
    }

    changesRequiredStream.accept(Notification.INSTANCE);
  }

  public void reset() {
    if (rootCommentNode != null) {
      changesRequiredStream.accept(Notification.INSTANCE);
    }
    rootCommentNode = null;
  }

  @CheckResult
  public Observable<List<SubmissionCommentRow>> streamTreeUpdates() {
    return changesRequiredStream
        .map(o -> {
          if (submission == null) {
            throw new AssertionError("How did we even reach here??");
          }
          return o;
        })
        .map(o -> constructComments())
        .map(toImmutable());
  }

  /**
   * Collapse/expand a comment.
   */
  public void toggleCollapse(PostedOrInFlightContribution contribution) {
    assertNotNull(contribution.idForTogglingCollapse());

    if (isCollapsed(contribution)) {
      collapsedContributionIds.remove(contribution.idForTogglingCollapse());
    } else {
      collapsedContributionIds.add(contribution.idForTogglingCollapse());
    }
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  private boolean isCollapsed(String commentRowFullName) {
    return collapsedContributionIds.contains(commentRowFullName);
  }

  public boolean isCollapsed(PostedOrInFlightContribution contribution) {
    return isCollapsed(contribution.idForTogglingCollapse());
  }

  boolean isCollapsed(Contribution contribution) {
    return isCollapsed(idForTogglingCollapse(contribution));
  }

  /**
   * Enable "loading more…" progress indicator.
   *
   * @param parentContribution for which more child nodes are being fetched.
   */
  public void setMoreCommentsLoading(PostedOrInFlightContribution parentContribution, boolean loading) {
    if (parentContribution.idForTogglingCollapse() == null) {
      throw new AssertionError();
    }

    if (loading) {
      loadingMoreContributionIds.add(parentContribution.idForTogglingCollapse());
    } else {
      loadingMoreContributionIds.remove(parentContribution.idForTogglingCollapse());
    }
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  public boolean areMoreCommentsLoadingFor(CommentNode commentNode) {
    return loadingMoreContributionIds.contains(idForTogglingCollapse(commentNode));
  }

  /**
   * See {@link PostedOrInFlightContribution.ContributionFetchedFromRemote#idForTogglingCollapse()}
   */
  private String idForTogglingCollapse(CommentNode commentNode) {
    return commentNode.getComment().getFullName();
  }

  /**
   * See {@link PostedOrInFlightContribution.ContributionFetchedFromRemote#idForTogglingCollapse()}
   */
  private String idForTogglingCollapse(Contribution contribution) {
    return contribution.getFullName();
  }

  /**
   * Show reply field for a comment and also expand any hidden comments.
   */
  public void showReplyAndExpandComments(PostedOrInFlightContribution parentContribution) {
    replyActiveForContributionIds.add(parentContribution.idForTogglingCollapse());
    collapsedContributionIds.remove(parentContribution.idForTogglingCollapse());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  /**
   * Show reply field for the submission or a comment.
   */
  public void showReply(PostedOrInFlightContribution parentContribution) {
    replyActiveForContributionIds.add(parentContribution.idForTogglingCollapse());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  /**
   * Hide reply field for a comment.
   */
  public void hideReply(PostedOrInFlightContribution parentContribution) {
    replyActiveForContributionIds.remove(parentContribution.idForTogglingCollapse());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  boolean isReplyActiveFor(Contribution parentContribution) {
    return replyActiveForContributionIds.contains(idForTogglingCollapse(parentContribution));
  }

  public boolean isReplyActiveFor(PostedOrInFlightContribution contribution) {
    return replyActiveForContributionIds.contains(contribution.idForTogglingCollapse());
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments() {
    //Timber.d("-----------------------------------------------");
    int totalRowsSize = 0;
    if (isReplyActiveFor(submission)) {
      totalRowsSize += 1;
    }
    if (rootCommentNode != null) {
      totalRowsSize += rootCommentNode.getTotalSize();
    }

    ArrayList<SubmissionCommentRow> flattenComments = new ArrayList<>(totalRowsSize);
    if (isReplyActiveFor(submission)) {
      flattenComments.add(CommentInlineReplyItem.create(submission, 0));
    }

    if (rootCommentNode == null) {
      return flattenComments;
    } else {
      return constructComments(flattenComments, rootCommentNode);
    }
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments(List<SubmissionCommentRow> flattenComments, CommentNode nextNode) {
    //String indentation = "";
    //if (nextNode.getDepth() != 0) {
    //  for (int step = 0; step < nextNode.getDepth(); step++) {
    //    indentation += "  ";
    //  }
    //}

    boolean isCommentNodeCollapsed = isCollapsed(nextNode.getComment());
    boolean isReplyActive = isReplyActiveFor(nextNode.getComment());

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
    String commentFullName = nextNode.getComment().getFullName();
    if (!isCommentNodeCollapsed && pendingReplyMap.containsKey(commentFullName)) {
      List<PendingSyncReply> pendingSyncReplies = pendingReplyMap.get(commentFullName);
      for (int i = 0; i < pendingSyncReplies.size(); i++) {     // Intentionally avoiding thrashing Iterator objects.
        PendingSyncReply pendingSyncReply = pendingSyncReplies.get(i);
        String replyFullName = PostedOrInFlightContribution.idForTogglingCollapseForLocallyPostedReply(
            nextNode.getComment().getFullName(),
            pendingSyncReply.createdTimeMillis()
        );
        boolean isReplyCollapsed = isCollapsed(replyFullName);
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
          constructComments(flattenComments, node);
        }

        if (nextNode.hasMoreComments()) {
          //Timber.d("%s(%s) %s has %d MORE ---------->",
          //    indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
          //);
          //Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
          String nextNodeFullname = nextNode.getComment().getFullName();
          flattenComments.add(LoadMoreCommentItem.create(nextNodeFullname, nextNode, areMoreCommentsLoadingFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }
}
