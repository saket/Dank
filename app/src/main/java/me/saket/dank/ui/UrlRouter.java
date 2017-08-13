package me.saket.dank.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;
import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.RedditLink;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.user.UserProfilePopup;
import me.saket.dank.ui.webview.WebViewActivity;
import timber.log.Timber;

public class UrlRouter {

  public static void resolveAndOpen(Link link, Context context) {
    resolveAndOpen(link, context, (Rect) null);
  }

  public static void resolveAndOpen(Link link, Context context, Point expandFromPoint) {
    Rect expandFromShapeRect;
    if (expandFromPoint != null) {
      int deviceDisplayWidthPx = context.getResources().getDisplayMetrics().widthPixels;
      expandFromShapeRect = new Rect(0, expandFromPoint.y, deviceDisplayWidthPx, expandFromPoint.y);
    } else {
      expandFromShapeRect = null;
    }
    resolveAndOpen(link, context, expandFromShapeRect);
  }

  /**
   * @param expandFromShape The initial shape of the target Activity from where it will begin its entry expand animation.
   */
  public static void resolveAndOpen(Link link, Context context, @Nullable Rect expandFromShape) {
    Timber.i("%s", link);

    if (link instanceof RedditLink.Subreddit) {
      SubredditActivityWithTransparentWindowBackground.start(context, (RedditLink.Subreddit) link, expandFromShape);

    } else if (link instanceof RedditLink.Submission) {
      SubmissionFragmentActivity.start(context, (RedditLink.Submission) link, expandFromShape);

    } else if (link instanceof RedditLink.User) {
      throw new IllegalStateException("Use UserProfilePopup instead");

    } else if (link.isExternal()) {
      String url = link instanceof Link.External ? ((Link.External) link).url : ((MediaLink.ImgurAlbum) link).albumUrl();
      WebViewActivity.start(context, url);

    } else {
      Toast.makeText(context, "TODO: " + link.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
    }
  }

  public static void openUserProfilePopup(RedditLink.User link, View anchorView, Point clickedUrlCoordinates) {
    UserProfilePopup userProfilePopup = new UserProfilePopup(anchorView.getContext());
    userProfilePopup.loadUserProfile(link);
    userProfilePopup.showAtLocation(anchorView, clickedUrlCoordinates);
  }
}
