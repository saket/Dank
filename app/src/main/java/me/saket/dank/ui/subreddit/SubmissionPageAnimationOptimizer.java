package me.saket.dank.ui.subreddit;

import net.dean.jraw.models.Submission;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.ui.submission.SubmissionPageLayout;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.UrlParser;

/**
 * Animating {@link SubmissionPageLayout}'s entry while loading data at the same time is expensive.
 * It's UI is complex and causes stutters. This class delays the loading of data in some cases to
 * workaround this issue.
 *
 * @deprecated Looks like this isn't needed anymore. RecyclerView is good at optimizing performance.
 */
public class SubmissionPageAnimationOptimizer {

  private final Lazy<UrlParser> urlParser;
  private boolean linkDetailsViewLaidOut;
  private boolean contentImageViewLaidOut;
  private boolean contentVideoViewLaidOut;

  @Inject
  public SubmissionPageAnimationOptimizer(Lazy<UrlParser> urlParser) {
    this.urlParser = urlParser;
  }

  public void trackSubmissionOpened(Submission submission) {
    Single.fromCallable(() -> urlParser.get().parse(submission.getUrl(), submission))
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

      case SINGLE_IMAGE:
      case SINGLE_GIF:
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

    Link submissionContentLink = urlParser.get().parse(submission.getUrl(), submission);
    boolean needsDelay;

    switch (submissionContentLink.type()) {
      case REDDIT_PAGE:
      case MEDIA_ALBUM:
      case EXTERNAL:
        needsDelay = !linkDetailsViewLaidOut;
        break;

      case SINGLE_IMAGE:
      case SINGLE_GIF:
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
