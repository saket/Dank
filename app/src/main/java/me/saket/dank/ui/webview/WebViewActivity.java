package me.saket.dank.ui.webview;

import static me.saket.dank.utils.Views.touchLiesOn;

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
import me.saket.dank.utils.Urls;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

/**
 * Fallback for Chrome custom tabs.
 */
public class WebViewActivity extends DankPullCollapsibleActivity {

    private static final String KEY_URL = "url";

    @BindView(R.id.webviewfallback_root) IndependentExpandablePageLayout activityContentPage;
    @BindView(R.id.webviewfallback_webview) WebView webView;
    @BindView(R.id.webviewfallback_progress) ProgressBar progressView;

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(KEY_URL, url);
        context.startActivity(intent);
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
        expandFromBelowToolbar();
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

        webView.loadUrl(urlToLoad);
    }

    protected void setupContentExpandablePage() {
        super.setupContentExpandablePage(activityContentPage);

        activityContentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
            if (touchLiesOn(webView, downX, downY)) {
                Timber.i("Can scroll -1? %s, +1? %s", webView.canScrollVertically(-1), webView.canScrollVertically(+1));
                return webView.canScrollVertically(upwardPagePull ? +1 : -1);

            } else {
                return false;
            }
        });
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
