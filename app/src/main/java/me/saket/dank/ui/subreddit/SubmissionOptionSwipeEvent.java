package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class SubmissionOptionSwipeEvent {

  public abstract Submission submission();

  public abstract SwipeableLayout itemView();

  public static SubmissionOptionSwipeEvent create(Submission submission, SwipeableLayout itemView) {
    return new AutoValue_SubmissionOptionSwipeEvent(submission, itemView);
  }
}
