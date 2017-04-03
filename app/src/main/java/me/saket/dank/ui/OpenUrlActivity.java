package me.saket.dank.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;
import android.widget.Toast;

import me.saket.dank.data.Link;
import me.saket.dank.data.RedditLink;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivityWithTransparentWindowBackground;
import me.saket.dank.ui.user.UserProfileActivity;
import me.saket.dank.ui.webview.WebViewActivity;
import timber.log.Timber;

public class OpenUrlActivity extends DankActivity {

    private static final String KEY_LINK = "link";

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

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
            WebViewActivity.start(this, ((Link.External) link).url);
            finish();

        } else {
            Toast.makeText(this, "TODO: " + link.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

}
