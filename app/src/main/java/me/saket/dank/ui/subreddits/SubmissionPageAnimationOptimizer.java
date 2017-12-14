package me.saket.dank.ui.subreddits;

import net.dean.jraw.models.Submission;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.links.Link;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.utils.UrlParser;
import timber.log.Timber;

/**
 * Animating {@link SubmissionFragment}'s entry while loading data at the same time is expensive.
 * It's UI is complex and causes stutters. This class delays the loading of data in some cases to
 * workaround this issue.
 */
public class SubmissionPageAnimationOptimizer {

  private boolean linkDetailsViewLaidOut;
  private boolean contentImageViewLaidOut;
  private boolean contentVideoViewLaidOut;

  public void trackSubmissionOpened(Submission submission) {
    Single.fromCallable(() -> {
      final long startTime = System.currentTimeMillis();
      Link link = UrlParser.parse(submission.getUrl());
      long l = System.currentTimeMillis() - startTime;
      Timber.i("Parsed url in: %sms", l);
      return link;
    })
        .subscribeOn(Schedulers.io())
        .subscribe(submissionContentLink -> trackSubmissionOpened(submissionContentLink));
  }

  public void trackSubmissionOpened(Link submissionContentLink) {
    if (!isOptimizationPending()) {
      return;
    }

    switch (submissionContentLink.type()) {
      case REDDIT_PAGE:
      case MEDIA_ALBUM:
      case EXTERNAL:
        linkDetailsViewLaidOut = true;
        break;

      case SINGLE_IMAGE_OR_GIF:
        contentImageViewLaidOut = true;
        break;

      case SINGLE_VIDEO:
        contentVideoViewLaidOut = true;
        break;

      default:
        throw new UnsupportedOperationException("Unknown type: " + submissionContentLink.type());
    }
  }

  public boolean shouldDelayLoad(Submission submission) {
    if (!isOptimizationPending()) {
      return false;
    }

    Link submissionContentLink = UrlParser.parse(submission.getUrl());
    boolean needsDelay;

    switch (submissionContentLink.type()) {
      case REDDIT_PAGE:
      case MEDIA_ALBUM:
      case EXTERNAL:
        needsDelay = !linkDetailsViewLaidOut;
        break;

      case SINGLE_IMAGE_OR_GIF:
        needsDelay = !contentImageViewLaidOut;
        break;

      case SINGLE_VIDEO:
        needsDelay = !contentVideoViewLaidOut;
        break;

      default:
        throw new UnsupportedOperationException("Unknown type: " + submissionContentLink.type());
    }

    return needsDelay;
  }

  public boolean isOptimizationPending() {
    return !linkDetailsViewLaidOut || !contentImageViewLaidOut || !contentVideoViewLaidOut;
  }
}
