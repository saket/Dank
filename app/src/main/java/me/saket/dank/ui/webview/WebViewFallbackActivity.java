package me.saket.dank.ui.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * Fallback for Chrome custom tabs.
 */
public class WebViewFallbackActivity extends DankPullCollapsibleActivity {

    private static final String KEY_URL = "url";

    @BindView(R.id.webviewfallback_root) IndependentExpandablePageLayout activityContentPage;
    @BindView(R.id.webviewfallback_webview) WebView webView;
    @BindView(R.id.webviewfallback_progress) ProgressBar progressView;

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, WebViewFallbackActivity.class);
        intent.putExtra(KEY_URL, url);
        context.startActivity(intent);
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setPullToCollapseEnabled(true);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ButterKnife.bind(this);
        findAndSetupToolbar();

        String urlToLoad = getIntent().getStringExtra(KEY_URL);
        setTitle(urlToLoad);

        progressView.setIndeterminate(true);

        setupWebView(urlToLoad);
        setupContentExpandablePage(activityContentPage);
        expandFromBelowToolbar();
    }

    private void setupWebView(String urlToLoad) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
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

        webView.loadUrl(urlToLoad);
    }

    @Override
    protected void onDestroy() {
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
