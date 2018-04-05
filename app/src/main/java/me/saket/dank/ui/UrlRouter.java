package me.saket.dank.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import net.dean.jraw.models.Thumbnails;

import javax.inject.Inject;
import javax.inject.Singleton;

import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.ui.media.MediaAlbumViewerActivity;
import me.saket.dank.ui.submission.SubmissionPageLayoutActivity;
import me.saket.dank.ui.subreddit.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.user.UserProfilePopup;
import me.saket.dank.ui.webview.WebViewActivity;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.RedditUserLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.Optional;
import timber.log.Timber;

@Singleton
public class UrlRouter {

  @Inject JacksonHelper jacksonHelper;

  @Inject
  protected UrlRouter() {
  }

  public UserProfilePopupRouter forLink(RedditUserLink redditUserLink) {
    return new UserProfilePopupRouter(redditUserLink);
  }

  /**
   * For cases where {@link MediaIntentRouter#withRedditSuppliedImages(Thumbnails)} cab be used.
   */
  public MediaIntentRouter forLink(MediaLink mediaLink) {
    return new MediaIntentRouter(mediaLink, jacksonHelper);
  }

  public IntentRouter forLink(Link link) {
    if (link instanceof RedditUserLink) {
      throw new UnsupportedOperationException("Use forLink(RedditUserLink) instead.");
    }
    if (link instanceof MediaLink && BuildConfig.DEBUG) {
      Timber.w("Consider using forLink(MediaLink) instead");
      new Exception().printStackTrace();
    }
    return new IntentRouter(link);
  }

  public static class IntentRouter extends UrlRouter {
    private Link link;

    @Nullable private Point expandFromPoint;
    @Nullable private Rect expandFromRect;

    public IntentRouter(Link link) {
      this.link = link;
    }

    /**
     * Just makes the entry animation explicit for the code reader; nothing else.
     */
    public IntentRouter expandFromBelowToolbar() {
      //noinspection ConstantConditions
      return expandFrom((Rect) null);
    }

    public IntentRouter expandFrom(Point expandFromPoint) {
      this.expandFromPoint = expandFromPoint;
      return this;
    }

    public IntentRouter expandFrom(Rect expandFromRect) {
      this.expandFromRect = expandFromRect;
      return this;
    }

    public Intent intent(Context context) {
      if (expandFromRect == null && expandFromPoint != null) {
        int deviceDisplayWidthPx = context.getResources().getDisplayMetrics().widthPixels;
        expandFromRect = new Rect(0, expandFromPoint.y, deviceDisplayWidthPx, expandFromPoint.y);
      }

      if (link instanceof RedditSubredditLink) {
        return SubredditActivityWithTransparentWindowBackground.intent(context, (RedditSubredditLink) link, expandFromRect);

      } else if (link instanceof RedditSubmissionLink) {
        return SubmissionPageLayoutActivity.intent(context, (RedditSubmissionLink) link, expandFromRect);

      } else if (link instanceof RedditUserLink) {
        throw new IllegalStateException("Use UserProfilePopup instead");

      } else if (link instanceof MediaLink) {
        return MediaAlbumViewerActivity.intent(context, ((MediaLink) link), null, jacksonHelper);

      } else if (link.isExternal()) {
        String url = link.unparsedUrl();
        boolean isWebUrl = Patterns.WEB_URL.matcher(url).matches();

        if (isWebUrl) {
          String packageNameForDeepLink = findAllowedPackageNameForDeepLink(url);
          if (packageNameForDeepLink != null && isPackageNameInstalled(context, packageNameForDeepLink)) {
            android.content.Intent openUrlIntent = Intents.createForOpeningUrl(url);
            openUrlIntent.setPackage(packageNameForDeepLink);
            return openUrlIntent;

          } else {
            return WebViewActivity.intent(context, url, expandFromRect);
          }
        } else {
          return Intents.createForOpeningUrl(url);
        }

      } else {
        throw new UnsupportedOperationException("Unknown external link: " + link);
      }
    }

    public void open(Context context) {
      Intent intent = intent(context);
      try {
        context.startActivity(intent);

      } catch (ActivityNotFoundException e) {
        String toastMessage = Optional.ofNullable(intent.getData())
            .map(data -> data.toString())
            .map(uri -> context.getString(R.string.common_error_no_app_found_to_handle_url, uri))
            .orElse(context.getString(R.string.common_error_no_app_found_to_handle_this_action));
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
      }
    }
  }

  public static class MediaIntentRouter extends UrlRouter {

    @Nullable private Thumbnails redditSuppliedImages;
    private final MediaLink link;
    private final JacksonHelper jacksonHelper;

    public MediaIntentRouter(MediaLink link, JacksonHelper jacksonHelper) {
      this.link = link;
      this.jacksonHelper = jacksonHelper;
    }

    /**
     * I have considered parsing reddit-supplied images and including them in {@link MediaLink}
     * as low-quality URL, but that wouldn't work. The logic for filtering reddit's images is
     * done based on the display width, which will change on config changes.
     */
    public MediaIntentRouter withRedditSuppliedImages(@Nullable Thumbnails redditSuppliedImages) {
      this.redditSuppliedImages = redditSuppliedImages;
      return this;
    }

    public void open(Context context) {
      context.startActivity(MediaAlbumViewerActivity.intent(context, link, redditSuppliedImages, jacksonHelper));
    }
  }

  public static class UserProfilePopupRouter extends UrlRouter {
    private RedditUserLink link;
    private Point expandFromPoint;

    public UserProfilePopupRouter(RedditUserLink link) {
      this.link = link;
    }

    public UserProfilePopupRouter expandFrom(Point expandFromPoint) {
      this.expandFromPoint = expandFromPoint;
      return this;
    }

    public void open(View anchorView) {
      UserProfilePopup userProfilePopup = new UserProfilePopup(anchorView.getContext());
      userProfilePopup.loadUserProfile(link);
      userProfilePopup.showAtLocation(anchorView, expandFromPoint);
    }
  }

  /**
   * I'd ideally like to send a deeplink intent if any app can open a URL, but I also don't
   * want web browsers to open a link because Dank already has an internal web browser. So
   * we're selectively checking if we want to let an app take over the URL.
   *
   * TODO: Add Twitter, Instagram, Spotify, Netflix, Medium, Nytimes, Wikipedia, Sound cloud, Google photos,
   */
  @Nullable
  public static String findAllowedPackageNameForDeepLink(String url) {
    Uri URI = Uri.parse(url);
    String urlHost = URI.getHost();

    if (urlHost == null) {
      return null;
    }

    if (urlHost.endsWith("youtube.com") || urlHost.endsWith("youtu.be")) {
      return "com.google.android.youtube";

    } else if (UrlParser.isGooglePlayUrl(urlHost, URI.getPath())) {
      return "com.android.vending";

    } else {
      return null;
    }
  }

  public static boolean isPackageNameInstalled(Context context, String packageName) {
    PackageManager packageManager = context.getPackageManager();
    try {
      packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
      return true;

    } catch (PackageManager.NameNotFoundException ignored) {
      return false;
    }
  }
}
