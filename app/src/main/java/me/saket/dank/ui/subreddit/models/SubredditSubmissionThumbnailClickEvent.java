package me.saket.dank.ui.subreddit.models;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

@AutoValue
public abstract class SubredditSubmissionThumbnailClickEvent {

  public abstract Submission submission();

  public abstract View itemView();

  public abstract View thumbnailView();

  public static SubredditSubmissionThumbnailClickEvent create(Submission submission, View itemView, View thumbnailView) {
    return new AutoValue_SubredditSubmissionThumbnailClickEvent(submission, itemView, thumbnailView);
  }
}
