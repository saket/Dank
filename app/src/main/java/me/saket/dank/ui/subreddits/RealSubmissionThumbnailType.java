package me.saket.dank.ui.subreddits;

import net.dean.jraw.models.Submission;

/**
 * This exists because {@link Submission#getThumbnailType()} is not realiable.
 */
public enum RealSubmissionThumbnailType {

  NSFW_SELF_POST,
  NSFW_LINK,
  URL_STATIC_ICON,
  URL_REMOTE_THUMBNAIL,
  SELF_POST,
  NONE;

  public static RealSubmissionThumbnailType parse(Submission submission, Boolean showNsfwThumbnails) {
    //Timber.d("-------------------");
    //Timber.i("%s", submission.getTitle());
    //Timber.i("Thumb type: %s", submission.getThumbnailType());
    //Timber.i("Thumbnail: %s", submission.getThumbnail());
    //Timber.i("Thumbnails: %s", submission.getThumbnails());

    if (submission.isNsfw() && !showNsfwThumbnails) {
      if (submission.getThumbnail() == null || submission.getThumbnailType() == Submission.ThumbnailType.NONE) {
        return NONE;
      } else {
        return submission.isSelfPost() ? NSFW_SELF_POST : NSFW_LINK;
      }
    }

    if (submission.getThumbnailType() == Submission.ThumbnailType.NONE) {
      return NONE;
    }

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
        throw new UnsupportedOperationException();
    }
  }
}
