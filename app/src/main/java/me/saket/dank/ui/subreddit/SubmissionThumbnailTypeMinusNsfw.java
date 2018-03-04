package me.saket.dank.ui.subreddit;

import net.dean.jraw.models.Submission;

public enum SubmissionThumbnailTypeMinusNsfw {

  URL_STATIC_ICON,
  URL_REMOTE_THUMBNAIL,
  SELF_POST,
  NONE,
  UNKNOWN;

  /**
   * Like {@link Submission#getThumbnailType()}, but doesn't consider if a submission is NSFW, assuming that NSFW content is allowed.
   */
  public static SubmissionThumbnailTypeMinusNsfw parse(Submission submission) {
    //Timber.d("-------------------");
    //Timber.i("%s", submission.getTitle());
    //Timber.i("Thumb type: %s", submission.getThumbnailType());
    //Timber.i("Thumbnail: %s", submission.getThumbnail());
    //Timber.i("Thumbnails: %s", submission.getThumbnails());

    switch (submission.getThumbnailType()) {
      case NSFW:
        if (submission.isSelfPost()) {
          return SELF_POST;
        } else if (submission.getThumbnails() == null) {
          return URL_STATIC_ICON;
        } else {
          return URL_REMOTE_THUMBNAIL;
        }

      case DEFAULT:
        if (submission.getThumbnail() == null) {
          return NONE;
        } else {
          throw new AssertionError("Submission with DEFAULT thumbnail type and non-null thumbnail URL");
        }

      case SELF:
        return SELF_POST;

      case URL:
        if (submission.getThumbnails() == null) {
          return URL_STATIC_ICON;
        } else {
          return URL_REMOTE_THUMBNAIL;
        }

      case NONE:
        return NONE;

      default:
        return UNKNOWN;
    }
  }
}
