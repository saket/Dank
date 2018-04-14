package me.saket.dank.ui.subreddit.events;

import android.graphics.Point;
import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.UrlRouter;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditLink;
import me.saket.dank.urlparser.UrlParser;

@AutoValue
public abstract class SubredditSubmissionThumbnailClickEvent {

  public abstract Submission submission();

  public abstract View itemView();

  public abstract View thumbnailView();

  public static SubredditSubmissionThumbnailClickEvent create(Submission submission, View itemView, View thumbnailView) {
    if (submission.isSelfPost()) {
      throw new AssertionError("Shouldn't happen");
    }
    return new AutoValue_SubredditSubmissionThumbnailClickEvent(submission, itemView, thumbnailView);
  }

  public void openContent(UrlParser urlParser, UrlRouter urlRouter) {
    Submission submission = submission();
    Link contentLink = urlParser.parse(submission.getUrl(), submission);

    switch (contentLink.type()) {
      case SINGLE_IMAGE:
      case SINGLE_GIF:
      case SINGLE_VIDEO:
      case MEDIA_ALBUM:
        urlRouter
            .forLink(((MediaLink) contentLink))
            .withRedditSuppliedImages(submission.getThumbnails())
            .open(itemView().getContext());
        break;

      case REDDIT_PAGE:
        switch (((RedditLink) contentLink).redditLinkType()) {
          case COMMENT:
          case SUBMISSION:
          case SUBREDDIT:
            urlRouter
                .forLink(contentLink)
                .expandFrom(new Point(0, itemView().getBottom()))
                .open(itemView().getContext());
            break;

          case USER:
            throw new AssertionError("Did not expect Reddit to create a thumbnail for user links");

          default:
            throw new AssertionError();
        }
        break;

      case EXTERNAL:
        urlRouter
            .forLink(contentLink)
            .expandFrom(new Point(0, itemView().getBottom()))
            .open(itemView().getContext());
        break;

      default:
        throw new AssertionError();
    }

  }
}
