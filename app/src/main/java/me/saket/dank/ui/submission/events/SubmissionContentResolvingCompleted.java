package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.UiEvent;
import me.saket.dank.urlparser.Link;

@AutoValue
public abstract class SubmissionContentResolvingCompleted implements UiEvent {

  public abstract Link resolvedLink();

  public static SubmissionContentResolvingCompleted create(Link resolvedLink) {
    return new AutoValue_SubmissionContentResolvingCompleted(resolvedLink);
  }

  public boolean willBeDisplayedAsAContentLink() {
    switch (resolvedLink().type()) {
      case REDDIT_PAGE:
      case MEDIA_ALBUM:
      case EXTERNAL:
        return true;

      case SINGLE_IMAGE:
      case SINGLE_GIF:
      case SINGLE_VIDEO:
        return false;

      default:
        throw new AssertionError("Unknown link type: " + resolvedLink());
    }
  }
}
