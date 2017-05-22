package me.saket.dank.ui.authentication;

import static me.saket.dank.utils.RxUtils.applySchedulers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;

public class LoginActivity extends DankActivity {

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.login_webview) WebView webView;
  @BindView(R.id.login_progress) View progressView;

  private DankRedditClient.UserLoginHelper userLoginHelper;
  private boolean loggedIn;

  public static void startForResult(Activity activity, int requestCode) {
    activity.startActivityForResult(new Intent(activity, LoginActivity.class), requestCode);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    toolbar.setBackground(null);
    toolbar.setTitle(R.string.login);

    // Setup WebView.
    CookieManager.getInstance().removeAllCookies(null);
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        if (!loggedIn) {
          boolean shouldShowProgress = newProgress < 75;
          setProgressVisible(shouldShowProgress);
        }
      }
    });
    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (url.contains("code=")) {
          // We've detected the redirect URL.
          webView.stopLoading();
          loggedIn = true;
          handleOnPermissionGranted(url);

        } else if (url.contains("error=")) {
          Toast.makeText(LoginActivity.this, R.string.login_error_oauth_permission_rejected, Toast.LENGTH_LONG).show();
          webView.stopLoading();
        }
      }
    });

    userLoginHelper = Dank.reddit().userLoginHelper();
    webView.loadUrl(userLoginHelper.authorizationUrl());
  }

  private void handleOnPermissionGranted(String successUrl) {
    setProgressVisible(true);

    // TODO: 10/02/17 Test error cases here.
    userLoginHelper.parseOAuthSuccessUrl(successUrl)
        .map(__ -> Dank.reddit().loggedInUserName())
        .compose(applySchedulers())
        .subscribe(userName -> {
          Toast.makeText(LoginActivity.this, getString(R.string.login_welcome_user, userName), Toast.LENGTH_SHORT).show();
          setResult(RESULT_OK);
          finish();

        }, error -> {
          setProgressVisible(false);
          Toast.makeText(LoginActivity.this, R.string.login_error_oauth_failed, Toast.LENGTH_LONG).show();
        });
  }

  private void setProgressVisible(boolean shouldShowProgress) {
    progressView.setVisibility(shouldShowProgress ? View.VISIBLE : View.GONE);
    webView.setVisibility(shouldShowProgress ? View.INVISIBLE : View.VISIBLE);
  }

  @Override
  public void onBackPressed() {
    if (webView.canGoBack()) {
      webView.goBack();
    } else {
      super.onBackPressed();
    }
  }

}
