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

  public static RealSubmissionThumbnailType parse(Submission submission) {
    if (submission.isNsfw()) {
      return submission.isSelfPost() ? NSFW_SELF_POST : NSFW_LINK;

    } else {
      if (submission.getThumbnailType() == Submission.ThumbnailType.NONE) {
        return NONE;
      }

      switch (submission.getThumbnailType()) {
        case NSFW:
          throw new AssertionError("Submission#isNsfw() did not return true :/");

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
}
