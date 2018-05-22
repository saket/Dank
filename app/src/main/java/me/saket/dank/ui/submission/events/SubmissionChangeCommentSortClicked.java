package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubmissionChangeCommentSortClicked implements UiEvent {

  public abstract View buttonView();

  public static SubmissionChangeCommentSortClicked create(View buttonView) {
    return new AutoValue_SubmissionChangeCommentSortClicked(buttonView);
  }
}
