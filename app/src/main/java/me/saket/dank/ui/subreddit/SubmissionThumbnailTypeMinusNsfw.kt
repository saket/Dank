package me.saket.dank.ui.subreddit

import net.dean.jraw.models.Submission
import net.dean.jraw.models.ThumbnailType

enum class SubmissionThumbnailTypeMinusNsfw {

  URL_STATIC_ICON,
  URL_REMOTE_THUMBNAIL,
  SELF_POST,
  NONE,
  UNKNOWN;

  companion object {
    /**
     * Like [Submission.getThumbnailType], but doesn't consider if a submission is NSFW, assuming that NSFW content is allowed.
     */
    fun parse(submission: Submission): SubmissionThumbnailTypeMinusNsfw {
      //Timber.d("-------------------");
      //Timber.i("%s", submission.getTitle());
      //Timber.i("Thumb type: %s", submission.getThumbnailType());
      //Timber.i("Thumbnail: %s", submission.getThumbnail());
      //Timber.i("Thumbnails: %s", submission.getThumbnails());

      return when (submission.thumbnailType) {
        ThumbnailType.NSFW -> when {
          submission.isSelfPost -> SELF_POST
          submission.preview == null -> URL_STATIC_ICON
          else -> URL_REMOTE_THUMBNAIL
        }
        ThumbnailType.DEFAULT -> NONE
        ThumbnailType.SELF -> SELF_POST
        ThumbnailType.URL -> when (submission.preview) {
          null -> URL_STATIC_ICON
          else -> URL_REMOTE_THUMBNAIL
        }
        ThumbnailType.NONE -> NONE
        else -> UNKNOWN
      }
    }
  }
}
