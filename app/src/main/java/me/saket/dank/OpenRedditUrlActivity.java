package me.saket.dank;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;

import me.saket.dank.data.RedditUrl;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.subreddits.SubredditActivity;
import timber.log.Timber;

public class OpenRedditUrlActivity extends DankActivity {

    private static final String KEY_REDDIT_LINK = "redditLink";

    /**
     * @param expandFromShape The initial shape of the target Activity from where it will begin its entry expand animation.
     */
    public static void handle(Context context, RedditUrl redditUrl, Rect expandFromShape) {
        Intent intent = new Intent(context, OpenRedditUrlActivity.class);
        intent.putExtra(KEY_REDDIT_LINK, redditUrl);
        intent.putExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE, expandFromShape);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RedditUrl redditUrl = getIntent().getParcelableExtra(KEY_REDDIT_LINK);
        Rect expandFromShape = getIntent().getParcelableExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE);
        Timber.i("%s", redditUrl);

        if (redditUrl instanceof RedditUrl.Subreddit) {
            SubredditActivity.start(this, ((RedditUrl.Subreddit) redditUrl).name(), expandFromShape);
        }

        finish();
    }

}
