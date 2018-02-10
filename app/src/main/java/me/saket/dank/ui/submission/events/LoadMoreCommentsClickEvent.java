package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;

import me.saket.dank.data.CommentNodeEqualsBandAid;

@AutoValue
public abstract class LoadMoreCommentsClickEvent {

  /**
   * Clicked item's View.
   */
  public abstract View loadMoreItemView();

  abstract CommentNodeEqualsBandAid parentCommentNodeEqualsBandAid();

  public Comment parentComment() {
    return parentCommentNode().getComment();
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
}
