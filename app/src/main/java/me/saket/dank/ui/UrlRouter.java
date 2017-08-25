package me.saket.dank.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import net.dean.jraw.models.Thumbnails;

import me.saket.dank.data.exceptions.SerializableThumbnails;
import me.saket.dank.data.links.ExternalLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.RedditSubmissionLink;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.ui.media.MediaAlbumViewerActivity;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.user.UserProfilePopup;
import me.saket.dank.ui.webview.WebViewActivity;
import timber.log.Timber;

public class UrlRouter {

  @Nullable private Point expandFromPoint;
  @Nullable private Rect expandFromRect;
  @Nullable private SerializableThumbnails redditSuppliedImages;

  private final Context context;

  public static UrlRouter with(Context context) {
    return new UrlRouter(context);
  }

  private UrlRouter(Context context) {
    this.context = context;
  }

  public UrlRouter expandFrom(Point expandFromPoint) {
    this.expandFromPoint = expandFromPoint;
    return this;
  }

  public UrlRouter expandFrom(Rect expandFromRect) {
    this.expandFromRect = expandFromRect;
    return this;
  }

  /**
   * Just makes the entry animation explicit for the code reader; nothing else.
   */
  public UrlRouter expandFromBelowToolbar() {
    return expandFrom((Rect) null);
  }

  public UrlRouter useRedditSuppliedImages(@Nullable Thumbnails redditSuppliedImages) {
    if (redditSuppliedImages != null) {
      this.redditSuppliedImages = new SerializableThumbnails(redditSuppliedImages.getDataNode());
    }
    return this;
  }

  public void resolveIntentAndOpen(Link link) {
    Timber.i("%s", link);
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
      MediaAlbumViewerActivity.start(context, ((MediaLink) link), redditSuppliedImages);

    } else if (link.isExternal()) {
      if (link instanceof ExternalLink) {
        WebViewActivity.start(context, link.unparsedUrl());

      } else {
        throw new UnsupportedOperationException("Unknown external link: " + link);
      }

    } else {
      Toast.makeText(context, "TODO: " + link.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
  }

  public void openUserProfile(RedditUserLink userLink, View anchorView) {
    UserProfilePopup userProfilePopup = new UserProfilePopup(anchorView.getContext());
    userProfilePopup.loadUserProfile(userLink);
    userProfilePopup.showAtLocation(anchorView, expandFromPoint);
  }
}
