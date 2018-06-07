package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class CommentClicked implements UiEvent {

  public abstract Identifiable comment();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClicked create(Identifiable comment, int commentPosition, View commentItemView, boolean willCollapseOnClick) {
    return new AutoValue_CommentClicked(comment, commentPosition, commentItemView, willCollapseOnClick);
  }
}
