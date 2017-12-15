package me.saket.dank.ui.authentication;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import me.saket.dank.R;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.user.UserSessionRepository;
import timber.log.Timber;

public class LoginActivity extends DankActivity {

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.login_webview) WebView webView;
  @BindView(R.id.login_progress) View progressView;

  @Inject UserSessionRepository userSessionRepository;

  private DankRedditClient.UserLoginHelper userLoginHelper;
  private boolean loggedIn;

  public static void start(Context context) {
    context.startActivity(new Intent(context, LoginActivity.class));
  }

  @CheckResult
  public static Intent intent(Context context) {
    return new Intent(context, LoginActivity.class);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
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

    // Bug workaround: WebView crashes when dropdown is shown on
    // a Nougat emulator. Haven't tested on other devices.
    webView.clearFormData();
    webView.getSettings().setSaveFormData(false);

    userLoginHelper = Dank.reddit().createUserLoginHelper();
    webView.loadUrl(userLoginHelper.authorizationUrl());
  }

  private void handleOnPermissionGranted(String successUrl) {
    setProgressVisible(true);

    // TODO: 10/02/17 Test error cases here.
    userLoginHelper.parseOAuthSuccessUrl(successUrl)
        .compose(applySchedulersCompletable())
        .subscribe(() -> {
          String loggedInUserName = userSessionRepository.loggedInUserName();
          Toast.makeText(LoginActivity.this, getString(R.string.login_welcome_user, loggedInUserName), Toast.LENGTH_SHORT).show();
          setResult(RESULT_OK);
          finish();

        }, error -> {
          Timber.e(error);
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
