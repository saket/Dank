package me.saket.dank.walkthrough;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionClickEvent;

@AutoValue
public abstract class SubmissionGestureWalkthroughProceedEvent implements UiEvent {

  public abstract View itemView();

  public abstract long itemId();

  public static SubmissionGestureWalkthroughProceedEvent create(View itemView, long itemId) {
    return new AutoValue_SubmissionGestureWalkthroughProceedEvent(itemView, itemId);
  }

  public SubredditSubmissionClickEvent toSubmissionClickEvent(Submission submission) {
    return SubredditSubmissionClickEvent.create(submission, itemView(), itemId());
  }
}
