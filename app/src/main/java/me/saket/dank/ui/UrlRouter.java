package me.saket.dank.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.View;

import net.dean.jraw.models.Thumbnails;

import javax.inject.Inject;
import javax.inject.Singleton;

import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.RedditSubmissionLink;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.ui.media.MediaAlbumViewerActivity;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.webview.WebViewActivity;
import me.saket.dank.utils.JacksonHelper;

@Singleton
public class UrlRouter {

  @Inject JacksonHelper jacksonHelper;

  @Inject
  protected UrlRouter() {
  }

  public UrlRouter.UserProfilePopup forLink(RedditUserLink redditUserLink) {
    return new UserProfilePopup(redditUserLink);
  }

  /**
   * For cases where {@link UrlRouter.MediaIntent#withRedditSuppliedImages(Thumbnails)} cab be used.
   */
  public UrlRouter.MediaIntent forLink(MediaLink mediaLink) {
    return new MediaIntent(mediaLink, jacksonHelper);
  }

  public UrlRouter.Intent forLink(Link link) {
    if (link instanceof RedditUserLink) {
      throw new UnsupportedOperationException("Use forLink(RedditUserLink) instead.");
    }
    return new Intent(link);
  }

  public static class Intent extends UrlRouter {
    private Link link;

    @Nullable private Point expandFromPoint;
    @Nullable private Rect expandFromRect;

    public Intent(Link link) {
      this.link = link;
    }

    /**
     * Just makes the entry animation explicit for the code reader; nothing else.
     */
    public Intent expandFromBelowToolbar() {
      return expandFrom((Rect) null);
    }

    public Intent expandFrom(Point expandFromPoint) {
      this.expandFromPoint = expandFromPoint;
      return this;
    }

    public Intent expandFrom(Rect expandFromRect) {
      this.expandFromRect = expandFromRect;
      return this;
    }

    public void open(Context context) {
      if (expandFromRect == null && expandFromPoint != null) {
        int deviceDisplayWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        expandFromRect = new Rect(0, expandFromPoint.y, deviceDisplayWidthPx, expandFromPoint.y);
      }

      if (link instanceof RedditSubredditLink) {
        SubredditActivityWithTransparentWindowBackground.start(context, (RedditSubredditLink) link, expandFromRect);

      } else if (link instanceof RedditSubmissionLink) {
        SubmissionFragmentActivity.start(context, (RedditSubmissionLink) link, expandFromRect);

      } else if (link instanceof RedditUserLink) {
        throw new IllegalStateException("Use UserProfilePopup instead");

      } else if (link instanceof MediaLink) {
        MediaAlbumViewerActivity.start(context, ((MediaLink) link), null, jacksonHelper);

      } else if (link.isExternal()) {
        WebViewActivity.start(context, link.unparsedUrl());

      } else {
        throw new UnsupportedOperationException("Unknown external link: " + link);
      }
    }
  }

  public static class MediaIntent extends UrlRouter {
    @Nullable private Thumbnails redditSuppliedImages;
    private final MediaLink link;
    private final JacksonHelper jacksonHelper;

    public MediaIntent(MediaLink link, JacksonHelper jacksonHelper) {
      this.link = link;
      this.jacksonHelper = jacksonHelper;
    }

    public MediaIntent withRedditSuppliedImages(@Nullable Thumbnails redditSuppliedImages) {
      this.redditSuppliedImages = redditSuppliedImages;
      return this;
    }

    public void open(Context context) {
      MediaAlbumViewerActivity.start(context, link, redditSuppliedImages, jacksonHelper);
    }
  }

  public static class UserProfilePopup extends UrlRouter {
    private RedditUserLink link;
    @Nullable private Point expandFromPoint;

    public UserProfilePopup(RedditUserLink link) {
      this.link = link;
    }

    public UserProfilePopup expandFrom(Point expandFromPoint) {
      this.expandFromPoint = expandFromPoint;
      return this;
    }

    public void open(View anchorView) {
      me.saket.dank.ui.user.UserProfilePopup userProfilePopup = new me.saket.dank.ui.user.UserProfilePopup(anchorView.getContext());
      userProfilePopup.loadUserProfile(link);
      userProfilePopup.showAtLocation(anchorView, expandFromPoint);
    }
  }
}
