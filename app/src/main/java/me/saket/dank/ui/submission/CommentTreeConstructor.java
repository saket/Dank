package me.saket.dank.ui.submission;


import static me.saket.dank.utils.Commons.toImmutable;

import android.support.annotation.CheckResult;

import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Constructs comments to show in a submission. Ignores collapsed comments + adds reply fields + adds "load more" & "continue thread ->" items.
 */
public class CommentTreeConstructor {

  private Set<String> collapsedCommentNodeFullNames = new HashSet<>();        // Comments that are collapsed.
  private Set<String> loadingMoreCommentNodeFullNames = new HashSet<>();      // Comments for which more replies are being fetched.
  private Set<String> replyActiveForCommentNodeFullNames = new HashSet<>();   // Comments for which reply fields are active.
  private Relay<Object> changesRequiredStream = PublishRelay.create();
  private Map<String, List<PendingSyncReply>> pendingReplyMap = new HashMap<>();  // Key: comment full-name.
  private CommentNode rootCommentNode;
  private boolean replyActiveForSubmission;

  public CommentTreeConstructor() {}

  /**
   * Set the root comment of a submission.
   */
  public void setCommentsAndPendingReplies(Submission submissionWithComments, List<PendingSyncReply> pendingReplies) {
    rootCommentNode = submissionWithComments.getComments();

    // TODO: test performance.
    pendingReplyMap.clear();
    for (PendingSyncReply pendingSyncReply : pendingReplies) {
      String parentCommentFullName = pendingSyncReply.parentCommentFullName();

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
    changesRequiredStream.accept(Notification.INSTANCE);
    rootCommentNode = null;
    collapsedCommentNodeFullNames.clear();
    replyActiveForCommentNodeFullNames.clear();
  }

  @CheckResult
  public Observable<List<SubmissionCommentRow>> streamTreeUpdates() {
    return changesRequiredStream
        .observeOn(Schedulers.io())
        .map(o -> constructComments())
        .map(toImmutable());
  }

  /**
   * Collapse/expand a comment.
   */
  public void toggleCollapse(SubmissionCommentRow commentRow) {
    if (isCollapsed(commentRow)) {
      collapsedCommentNodeFullNames.remove(commentRow.fullName());
    } else {
      collapsedCommentNodeFullNames.add(commentRow.fullName());
    }
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  private boolean isCollapsed(String commentRowFullName) {
    return collapsedCommentNodeFullNames.contains(commentRowFullName);
  }

  public boolean isCollapsed(SubmissionCommentRow commentRow) {
    return isCollapsed(commentRow.fullName());
  }

  public boolean isCollapsed(CommentNode commentNode) {
    return isCollapsed(commentNode.getComment().getFullName());
  }

  /**
   * Enable "loading more…" progress indicator.
   */
  public Consumer<CommentNode> setMoreCommentsLoading(boolean loading) {
    return commentNode -> {
      if (loading) {
        loadingMoreCommentNodeFullNames.add(commentNode.getComment().getFullName());
      } else {
        loadingMoreCommentNodeFullNames.remove(commentNode.getComment().getFullName());
      }
      changesRequiredStream.accept(Notification.INSTANCE);
    };
  }

  public boolean areMoreCommentsLoadingFor(CommentNode commentNode) {
    return loadingMoreCommentNodeFullNames.contains(commentNode.getComment().getFullName());
  }

  /**
   * Show reply field for a comment.
   */
  public void showReplyAndExpandComments(CommentNode nodeToReply) {
    replyActiveForCommentNodeFullNames.add(nodeToReply.getComment().getFullName());
    collapsedCommentNodeFullNames.remove(nodeToReply.getComment().getFullName());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  /**
   * Hide reply field for a comment.
   */
  public void hideReply(CommentNode nodeToReply) {
    replyActiveForCommentNodeFullNames.remove(nodeToReply.getComment().getFullName());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  /**
   * Show/hide reply field for the submission. We're not using the root CommentNode so that reply
   * can be used even when the comments haven't been fetched.
   */
  public void toggleReplyForSubmission() {
    replyActiveForSubmission = !replyActiveForSubmission;
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  public boolean isReplyActiveFor(CommentNode commentNode) {
    return replyActiveForCommentNodeFullNames.contains(commentNode.getComment().getFullName());
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments() {
    //Timber.d("-----------------------------------------------");
    ArrayList<SubmissionCommentRow> flattenComments = new ArrayList<>(rootCommentNode.getTotalSize() + (replyActiveForSubmission ? 1 : 0));
    if (replyActiveForSubmission) {

    }

    if (rootCommentNode == null) {
      return flattenComments;
    }

    return constructComments(flattenComments, rootCommentNode);
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

    boolean isCommentNodeCollapsed = isCollapsed(nextNode);
    boolean isReplyActive = isReplyActiveFor(nextNode);

    if (nextNode.getDepth() != 0) {
      //Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
      flattenComments.add(DankCommentNode.create(nextNode, isCommentNodeCollapsed));
    }

    // Reply box.
    if (isReplyActive && !isCommentNodeCollapsed) {
      flattenComments.add(CommentInlineReplyItem.create(nextNode));
    }

    // Pending-sync replies.
    String commentFullName = nextNode.getComment().getFullName();
    if (!isCommentNodeCollapsed && pendingReplyMap.containsKey(commentFullName)) {
      List<PendingSyncReply> pendingSyncReplies = pendingReplyMap.get(commentFullName);
      for (int i = 0; i < pendingSyncReplies.size(); i++) {     // Intentionally avoiding thrashing Iterator objects.
        PendingSyncReply pendingSyncReply = pendingSyncReplies.get(i);
        String replyFullName = nextNode.getComment().getId() + "_reply_ " + pendingSyncReply.createdTimeMillis();
        boolean isReplyCollapsed = isCollapsed(replyFullName);
        flattenComments.add(CommentPendingSyncReplyItem.create(nextNode, replyFullName, pendingSyncReply, isReplyCollapsed));
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
          flattenComments.add(LoadMoreCommentItem.create(nextNode, areMoreCommentsLoadingFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }
}
