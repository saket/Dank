package me.saket.dank.ui.submission.events;

import android.graphics.Rect;
import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.tree.CommentNode;

import me.saket.dank.data.CommentNodeEqualsBandAid;
import me.saket.dank.ui.submission.SubmissionPageLayoutActivity;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Views;

@AutoValue
public abstract class LoadMoreCommentsClickEvent {

  /**
   * Clicked item's View.
   */
  public abstract View loadMoreItemView();

  abstract CommentNodeEqualsBandAid parentCommentNodeEqualsBandAid();

  public PublicContribution parentComment() {
    return parentCommentNode().getSubject();
  }

  /**
   * Node whose more comments have to be fetched.
   */
  public CommentNode parentCommentNode() {
    return parentCommentNodeEqualsBandAid().get();
  }

  public static LoadMoreCommentsClickEvent create(View loadMoreItemView, CommentNode parentNode) {
    return new AutoValue_LoadMoreCommentsClickEvent(loadMoreItemView, CommentNodeEqualsBandAid.create(parentNode));
  }

  public void openThreadContinuation(DankSubmissionRequest currentSubmissionRequest) {
    DankSubmissionRequest continueThreadRequest = currentSubmissionRequest.toBuilder()
        .focusCommentId(parentComment().getId())
        .build();
    Rect expandFromShape = Views.globalVisibleRect(loadMoreItemView());
    expandFromShape.top = expandFromShape.bottom;   // Because only expanding from a line is supported so far.
    SubmissionPageLayoutActivity.start(loadMoreItemView().getContext(), continueThreadRequest, expandFromShape);
  }
}
