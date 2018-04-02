package me.saket.dank.deeplinks;

import android.net.Uri;
import android.os.Bundle;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;
import com.airbnb.deeplinkdispatch.DeepLinkResult;

import javax.inject.Inject;

import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.UrlParser;

@DeepLinkHandler(AppDeepLinkModule.class)
public class DeepLinkHandlingActivity extends DankActivity {

  @Inject UrlParser urlParser;
  @Inject UrlRouter urlRouter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // DeepLinkDelegate and AppDeepLinkModuleLoader are generated at compile-time.
    DeepLinkDelegate deepLinkDelegate = new DeepLinkDelegate(new AppDeepLinkModuleLoader());
    DeepLinkResult result = deepLinkDelegate.dispatchFrom(this);

    if (!result.isSuccessful()) {
      Dank.dependencyInjector().inject(this);

      Uri uri = getIntent().getData();
      if (uri != null) {
        Link parsedLink = urlParser.parse(uri.toString());
        urlRouter.forLink(parsedLink)
            .expandFromBelowToolbar()
            .open(this);
      }
    }

    finish();
  }
}
