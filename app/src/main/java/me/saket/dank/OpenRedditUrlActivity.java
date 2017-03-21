package me.saket.dank;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;

import me.saket.dank.data.RedditLink;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.submission.SubmissionFragmentActivity;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.ui.user.UserProfileActivity;

public class OpenRedditUrlActivity extends DankActivity {

    private static final String KEY_REDDIT_LINK = "redditLink";

    /**
     * @param expandFromShape The initial shape of the target Activity from where it will begin its entry expand animation.
     */
    public static void handle(Context context, RedditLink redditLink, Rect expandFromShape) {
        Intent intent = new Intent(context, OpenRedditUrlActivity.class);
        intent.putExtra(KEY_REDDIT_LINK, redditLink);
        intent.putExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE, expandFromShape);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RedditLink redditLink = getIntent().getParcelableExtra(KEY_REDDIT_LINK);
        Rect expandFromShape = getIntent().getParcelableExtra(DankPullCollapsibleActivity.KEY_EXPAND_FROM_SHAPE);
        //Timber.i("%s", redditLink);

        if (redditLink instanceof RedditLink.Subreddit) {
            SubredditActivity.start(this, (RedditLink.Subreddit) redditLink, expandFromShape);

        } else if (redditLink instanceof RedditLink.Submission) {
            SubmissionFragmentActivity.start(this, (RedditLink.Submission) redditLink, expandFromShape);

        } else if (redditLink instanceof RedditLink.User) {
            UserProfileActivity.start(this, ((RedditLink.User) redditLink), expandFromShape);
        }

        finish();
    }

}
