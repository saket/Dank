package me.saket.dank.ui.submission;


import static me.saket.dank.utils.Commons.toImmutable;

import android.support.annotation.CheckResult;

import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Collections;
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

  private Set<String> collapsedCommentNodeIds = new HashSet<>();        // Comments that are collapsed.
  private Set<String> loadingMoreCommentNodeIds = new HashSet<>();      // Comments for which more replies are being fetched.
  private Set<String> replyActiveForCommentNodeIds = new HashSet<>();   // Comments for which reply fields are active.
  private Relay<Object> changesRequiredStream = PublishRelay.create();
  private CommentNode rootCommentNode;
  private Map<String, List<PendingSyncReply>> pendingReplyMap = new HashMap<>();  // Key: comment full-name.

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
    collapsedCommentNodeIds.clear();
    replyActiveForCommentNodeIds.clear();
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
  public void toggleCollapse(CommentNode nodeToCollapse) {
    if (isCollapsed(nodeToCollapse)) {
      collapsedCommentNodeIds.remove(nodeToCollapse.getComment().getId());
    } else {
      collapsedCommentNodeIds.add(nodeToCollapse.getComment().getId());
    }
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  public boolean isCollapsed(String commentId) {
    return collapsedCommentNodeIds.contains(commentId);
  }

  public boolean isCollapsed(CommentNode commentNode) {
    return isCollapsed(commentNode.getComment().getId());
  }

  /**
   * Enable "loading more…" progress indicator.
   */
  public Consumer<CommentNode> setMoreCommentsLoading(boolean loading) {
    return commentNode -> {
      if (loading) {
        loadingMoreCommentNodeIds.add(commentNode.getComment().getId());
      } else {
        loadingMoreCommentNodeIds.remove(commentNode.getComment().getId());
      }
      changesRequiredStream.accept(Notification.INSTANCE);
    };
  }

  public boolean areMoreCommentsLoadingFor(CommentNode commentNode) {
    return loadingMoreCommentNodeIds.contains(commentNode.getComment().getId());
  }

  /**
   * Show/hide reply field for a comment.
   */
  public void hideReply(CommentNode nodeToReply) {
    replyActiveForCommentNodeIds.remove(nodeToReply.getComment().getId());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  /**
   * Show/hide reply field for a comment.
   */
  public void showReplyAndExpandComments(CommentNode nodeToReply) {
    replyActiveForCommentNodeIds.add(nodeToReply.getComment().getId());
    collapsedCommentNodeIds.remove(nodeToReply.getComment().getId());
    changesRequiredStream.accept(Notification.INSTANCE);
  }

  public boolean isReplyActiveFor(CommentNode commentNode) {
    return replyActiveForCommentNodeIds.contains(commentNode.getComment().getId());
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionCommentRow> constructComments() {
    if (rootCommentNode == null) {
      return Collections.emptyList();
    }
    //Timber.d("-----------------------------------------------");
    return constructComments(new ArrayList<>(rootCommentNode.getTotalSize()), rootCommentNode);
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
      //Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
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
        boolean isPendingReplyItemCollapsed = false;  // TODO: Solve this.
        int depth = nextNode.getDepth() + 1;
        flattenComments.add(CommentPendingSyncReplyItem.create(nextNode, pendingSyncReply, depth, isPendingReplyItemCollapsed));
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
          //    indentation, nextNode.getComment().getId(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
          //);
          //Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
          flattenComments.add(LoadMoreCommentItem.create(nextNode, areMoreCommentsLoadingFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }
}
