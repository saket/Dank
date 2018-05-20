package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentSort;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class SubmissionCommentSortChanged implements UiEvent {

  public abstract CommentSort selectedSort();

  public static SubmissionCommentSortChanged create(CommentSort selectedSort) {
    return new AutoValue_SubmissionCommentSortChanged(selectedSort);
  }
}
