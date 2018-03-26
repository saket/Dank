package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import me.saket.dank.urlparser.Link;

@AutoValue
public abstract class SubmissionContentLinkClickEvent {

  public abstract View contentLinkView();

  public abstract Link link();

  public static SubmissionContentLinkClickEvent create(View contentLinkView, Link link) {
    return new AutoValue_SubmissionContentLinkClickEvent(contentLinkView, link);
  }
}
