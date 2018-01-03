package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

import me.saket.dank.data.CommentNodeEqualsBandAid;

@AutoValue
public abstract class LoadMoreCommentsClickEvent {

  abstract CommentNodeEqualsBandAid parentCommentNodeEqualsBandAid();

  /**
   * Node whose more comments have to be fetched.
   */
  public CommentNode parentCommentNode() {
    return parentCommentNodeEqualsBandAid().get();
  }

  /**
   * Clicked item's View.
   */
  public abstract View loadMoreItemView();

  public static LoadMoreCommentsClickEvent create(CommentNode parentNode, View loadMoreItemView) {
    return new AutoValue_LoadMoreCommentsClickEvent(CommentNodeEqualsBandAid.create(parentNode), loadMoreItemView);
  }
}
