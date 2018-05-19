package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class CommentsRefreshClicked implements UiEvent {

  public static CommentsRefreshClicked create() {
    return new AutoValue_CommentsRefreshClicked();
  }
}
