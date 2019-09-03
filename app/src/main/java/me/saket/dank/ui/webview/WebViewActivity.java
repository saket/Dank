package me.saket.dank.ui.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * Fallback for Chrome custom tabs.
 */
public class WebViewActivity extends DankPullCollapsibleActivity {

  private static final String KEY_URL = "url";

  @BindView(R.id.webviewfallback_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.webviewfallback_webview) WebView webView;
  @BindView(R.id.webviewfallback_progress) ProgressBar progressView;

  public static Intent intent(Context context, String url, Rect expandFromShape) {
    Intent intent = new Intent(context, WebViewActivity.class);
    intent.putExtra(KEY_URL, url);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    String urlToLoad = getIntent().getStringExtra(KEY_URL);
    setTitle(Urls.parseDomainName(urlToLoad));

    progressView.setIndeterminate(true);
    setupWebView(urlToLoad);

    setupContentExpandablePage();
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void setupWebView(String urlToLoad) {
    webView.setWebChromeClient(new WebChromeClient() {
      @Override
      public void onProgressChanged(WebView view, int newProgress) {
        progressView.setIndeterminate(false);
        progressView.setProgress(newProgress);
        progressView.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onReceivedTitle(WebView view, String title) {
        setTitle(title);
      }
    });
    webView.setWebViewClient(new WebViewClient());

    WebSettings webViewSettings = webView.getSettings();
    webViewSettings.setJavaScriptEnabled(true);
    webViewSettings.setDomStorageEnabled(true);
    webViewSettings.setDatabaseEnabled(true);
    webViewSettings.setSupportZoom(true);
    webViewSettings.setLoadWithOverviewMode(true);
    webViewSettings.setUseWideViewPort(true);

    webView.loadUrl(urlToLoad);
  }

  protected void setupContentExpandablePage() {
    super.setupContentExpandablePage(activityContentPage);

    // Trigger pull-to-collapse only if the page cannot be scrolled any further in the direction of scroll.
    activityContentPage.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(webView));
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_webview, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_refresh_webpage:
        webView.reload();
        return true;

      case R.id.action_share_webpage:
        startActivity(Intents.createForSharingUrl(this, getTitle().toString(), webView.getUrl()));
        return true;

      case R.id.action_open_in_external_browser:
        startActivity(Intents.createForOpeningUrl(webView.getUrl()));
        finish();
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onDestroy() {
    webView.setWebChromeClient(null);
    super.onDestroy();
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
