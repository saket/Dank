package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Comment;

@AutoValue
public abstract class CommentClickEvent {

  public abstract Comment comment();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClickEvent create(Comment comment, int commentPosition, View commentItemView, boolean willCollapseOnClick) {
    return new AutoValue_CommentClickEvent(comment, commentPosition, commentItemView, willCollapseOnClick);
  }
}
