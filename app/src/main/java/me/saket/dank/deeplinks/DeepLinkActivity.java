package me.saket.dank.deeplinks;

import android.os.Bundle;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;

import me.saket.dank.ui.DankActivity;

@DeepLinkHandler(AppDeepLinkModule.class)
public class DeepLinkActivity extends DankActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // DeepLinkDelegate and AppDeepLinkModuleLoader are generated at compile-time.
    DeepLinkDelegate deepLinkDelegate = new DeepLinkDelegate(new AppDeepLinkModuleLoader());
    deepLinkDelegate.dispatchFrom(this);
    finish();
  }
}
