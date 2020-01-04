package me.saket.dank.deeplinks;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.airbnb.deeplinkdispatch.DeepLinkResult;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.appshortcuts.AppShortcut;
import me.saket.dank.ui.preferences.DefaultWebBrowser;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.RedditUserLink;
import me.saket.dank.urlparser.UrlParser;

@DeepLinkHandler(AppDeepLinkModule.class)
public class DeepLinkHandlingActivity extends DankActivity {

  private static final String KEY_APP_SHORTCUT_ID = "appShortcutId";

  @Inject Lazy<UrlParser> urlParser;
  @Inject Lazy<UrlRouter> urlRouter;
  @Inject Lazy<ShortcutManager> shortcutManager;

  public static Intent appShortcutIntent(Context context, AppShortcut shortcut) {
    RedditSubredditLink subredditLink = RedditSubredditLink.create(shortcut.label());

    return new Intent(Intent.ACTION_VIEW, Uri.parse(subredditLink.unparsedUrl()))
        .putExtra(KEY_APP_SHORTCUT_ID, shortcut.id())
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

      if (getIntent().hasExtra(KEY_APP_SHORTCUT_ID)) {
        handleAppShortcutDeepLink();

      } else {
        Uri uri = getIntent().getData();
        if (uri != null) {
          Link parsedLink = urlParser.get().parse(uri.toString());
          if (parsedLink instanceof RedditUserLink) {
            Toast.makeText(this, "Viewing user profiles is currently unsupported", Toast.LENGTH_LONG).show();
            startActivity(DefaultWebBrowser.DANK_INTERNAL_BROWSER.intentForUrl(this, parsedLink.unparsedUrl(), null));
          } else {
            urlRouter.get().forLink(parsedLink)
                .expandFromBelowToolbar()
                .open(this);
          }
        }
      }
    }

    finish();
  }

  @TargetApi(Build.VERSION_CODES.N_MR1)
  private void handleAppShortcutDeepLink() {
    //noinspection ConstantConditions
    String subredditUrl = getIntent().getData().toString();
    RedditSubredditLink subredditLink = ((RedditSubredditLink) urlParser.get().parse(subredditUrl));
    startActivity(SubredditActivity.intent(this, subredditLink));

    String shortcutId = getIntent().getStringExtra(KEY_APP_SHORTCUT_ID);
    shortcutManager.get().reportShortcutUsed(shortcutId);
  }
}
