package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.PostedOrInFlightContribution;

@AutoValue
public abstract class CommentClickEvent {

  public abstract PostedOrInFlightContribution commentRow();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClickEvent create(
      PostedOrInFlightContribution comment,
      int commentPosition,
      View commentItemView,
      boolean willCollapseOnClick)
  {
    return new AutoValue_CommentClickEvent(comment, commentPosition, commentItemView, willCollapseOnClick);
  }
}
