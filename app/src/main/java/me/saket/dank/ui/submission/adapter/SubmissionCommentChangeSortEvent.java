package me.saket.dank.ui.submission.adapter;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubmissionCommentChangeSortEvent implements UiEvent {

  public abstract View sortButton();

  public static SubmissionCommentChangeSortEvent create(View sortButton) {
    return new AutoValue_SubmissionCommentChangeSortEvent(sortButton);
  }
}
