package me.saket.dank.ui;

import static me.saket.dank.utils.RxUtils.logError;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import org.chromium.customtabsclient.CustomTabsActivityHelper;
import org.chromium.customtabsclient.CustomTabsHelper;

import java.util.concurrent.TimeUnit;

import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.RedditLink;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.user.UserProfileActivity;
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment;
import rx.Observable;
import timber.log.Timber;

public class OpenUrlActivity extends DankActivity {

    private static final String KEY_LINK = "link";
    private static final int REQUESTCODE_CHROME_CUSTOM_TAB = 100;

    /**
     * @param expandFromShape The initial shape of the target Activity from where it will begin its entry expand animation.
     */
    public static void handle(Context context, Link link, @Nullable Rect expandFromShape) {
        Intent intent = new Intent(context, OpenUrlActivity.class);
        intent.putExtra(KEY_LINK, link);
        intent.putExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE, expandFromShape);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Link link = (Link) getIntent().getSerializableExtra(KEY_LINK);
        Rect expandFromShape = getIntent().getParcelableExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE);
        Timber.i("%s", link);

        if (link instanceof RedditLink.Subreddit) {
            SubredditActivityWithTransparentWindowBackground.start(this, (RedditLink.Subreddit) link, expandFromShape);
            finish();

        } else if (link instanceof RedditLink.Submission) {
            SubmissionFragmentActivity.start(this, (RedditLink.Submission) link, expandFromShape);
            finish();

        } else if (link instanceof RedditLink.User) {
            UserProfileActivity.start(this, ((RedditLink.User) link), expandFromShape);
            finish();

        } else if (link.isExternal()) {
            CustomTabsHelperFragment.attachTo(this);
            openLinkInChromeCustomTab(((Link.External) link));

        } else {
            Toast.makeText(this, "TODO: " + link.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void openLinkInChromeCustomTab(Link.External link) {
        CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .enableUrlBarHiding()
                .setToolbarColor(ContextCompat.getColor(this, R.color.toolbar))
                .addDefaultShareMenuItem()
                .setShowTitle(true)
                .build();

        CustomTabsActivityHelper.CustomTabsFallback customTabsFallback = (activity, uri) -> {
            // TODO: 24/03/17
            Timber.w("Fallback");
        };
        Uri linkToOpen = Uri.parse(link.url);

        // If we cant find a package name, it means there's no browser that supports
        // Chrome Custom Tabs installed. So, we fallback to the WebView.
        String packageName = CustomTabsHelper.getPackageNameToUse(this);
        if (packageName == null) {
            customTabsFallback.openUri(this, linkToOpen);

        } else {
            Bundle startAnimationBundle = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.chromecustomtab_enter_from_bottom, R.anim.nothing).toBundle();

            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(
                    this, R.anim.nothing, R.anim.chromecustomtab_exit_to_bottom).toBundle();
            customTabsIntent.intent.putExtra(CustomTabsIntent.EXTRA_EXIT_ANIMATION_BUNDLE, bundle);

            customTabsIntent.intent.setPackage(packageName);
            customTabsIntent.intent.setData(linkToOpen);
            startActivityForResult(customTabsIntent.intent, REQUESTCODE_CHROME_CUSTOM_TAB, startAnimationBundle);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUESTCODE_CHROME_CUSTOM_TAB) {
            // Finish this Activity once Chrome custom tabs has finished its exit animation.
            unsubscribeOnDestroy(Observable.timer(getResources().getInteger(R.integer.chromecustomtabs_transition_animation_duration) / 2, TimeUnit.MILLISECONDS).subscribe(__ -> finish(), logError("Wut")));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
