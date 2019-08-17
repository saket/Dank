package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import androidx.annotation.Nullable;

import me.saket.dank.R;
import me.saket.dank.urlparser.RedditSubredditLink;

/**
 * Android doesn't like activities that have transparent background, especially if it's the launcher activity.
 * Strange behaviors noticed so far were:
 * <p>
 * - Double tapping on a URL would take user to the home-screen.
 * - Opening a chrome custom tab would leak the home-screen during its entry animation.
 * <p>
 * This Activity simply exists so that a different theme can be used in the manifest.
 */
public class SubredditActivityWithTransparentWindowBackground extends SubredditActivity {

  /**
   * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
   */
  public static Intent intent(Context context, RedditSubredditLink subredditLink, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, SubredditActivityWithTransparentWindowBackground.class);
    addStartExtrasToIntent(subredditLink, expandFromShape, intent);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    contentPage.setBackgroundResource(R.color.window_background);
  }
}
