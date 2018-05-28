package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

@AutoValue
public abstract class CommentClickEvent {

  public abstract Identifiable comment();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClickEvent create(Identifiable comment, int commentPosition, View commentItemView, boolean willCollapseOnClick) {
    return new AutoValue_CommentClickEvent(comment, commentPosition, commentItemView, willCollapseOnClick);
  }
}
