package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.PostedOrInFlightContribution;

@AutoValue
public abstract class CommentClickEvent {

  public abstract PostedOrInFlightContribution commentInfo();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClickEvent create(
      PostedOrInFlightContribution commentInfo,
      int commentPosition,
      View commentItemView,
      boolean willCollapseOnClick)
  {
    return new AutoValue_CommentClickEvent(commentInfo, commentPosition, commentItemView, willCollapseOnClick);
  }
}
