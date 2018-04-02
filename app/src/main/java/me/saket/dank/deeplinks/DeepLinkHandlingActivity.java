package me.saket.dank.deeplinks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.airbnb.deeplinkdispatch.DeepLinkResult;

import javax.inject.Inject;

import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.UrlParser;

@DeepLinkHandler(AppDeepLinkModule.class)
public class DeepLinkHandlingActivity extends DankActivity {

  private static final String KEY_IS_APP_SHORTCUT_INTENT = "isAppShortcutIntent";

  @Inject UrlParser urlParser;
  @Inject UrlRouter urlRouter;

  public static Intent appShortcutIntent(Context context, RedditSubredditLink subredditLink) {
    return new Intent(Intent.ACTION_VIEW, Uri.parse(subredditLink.unparsedUrl()))
        .putExtra(KEY_IS_APP_SHORTCUT_INTENT, true)
        .setPackage(context.getPackageName());
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // DeepLinkDelegate and AppDeepLinkModuleLoader are generated at compile-time.
    DeepLinkDelegate deepLinkDelegate = new DeepLinkDelegate(new AppDeepLinkModuleLoader());
    DeepLinkResult result = deepLinkDelegate.dispatchFrom(this);

    if (!result.isSuccessful()) {
      Dank.dependencyInjector().inject(this);

      boolean isAppShortcutIntent = getIntent().getBooleanExtra(KEY_IS_APP_SHORTCUT_INTENT, false);
      if (isAppShortcutIntent) {
        //noinspection ConstantConditions
        String subredditUrl = getIntent().getData().toString();
        RedditSubredditLink subredditLink = ((RedditSubredditLink) urlParser.parse(subredditUrl));
        startActivity(SubredditActivity.intent(this, subredditLink));

      } else {
        Uri uri = getIntent().getData();
        if (uri != null) {
          Link parsedLink = urlParser.parse(uri.toString());
          urlRouter.forLink(parsedLink)
              .expandFromBelowToolbar()
              .open(this);
        }
      }
    }

    finish();
  }
}
