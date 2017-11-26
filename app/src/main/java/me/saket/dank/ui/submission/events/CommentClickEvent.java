package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.submission.SubmissionCommentRow;

@AutoValue
public abstract class CommentClickEvent {

  public abstract SubmissionCommentRow commentRow();

  public abstract int commentRowPosition();

  public abstract View commentItemView();

  public abstract boolean willCollapseOnClick();

  public static CommentClickEvent create(SubmissionCommentRow commentRow, int commentRowPosition, View commentItemView, boolean willCollapseOnClick) {
    return new AutoValue_CommentClickEvent(commentRow, commentRowPosition, commentItemView, willCollapseOnClick);
  }
}
