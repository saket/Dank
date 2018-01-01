package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

@AutoValue
public abstract class LoadMoreCommentsClickEvent {

  /**
   * Node whose more comments have to be fetched.
   */
  public abstract CommentNode parentCommentNode();

  /**
   * Clicked item's View.
   */
  public abstract View loadMoreItemView();

  public static LoadMoreCommentsClickEvent create(CommentNode parentNode, View loadMoreItemView) {
    return new AutoValue_LoadMoreCommentsClickEvent(parentNode, loadMoreItemView);
  }
}
