package me.saket.dank.ui.submission;

import android.os.Bundle;
import android.support.annotation.Nullable;

import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.subreddits.SubredditActivity;

/**
 * An Activity that can only show a submission, unlike {@link SubredditActivity} which can shows
 * subreddit submissions as well as holds the submission fragment.
 * <p>
 * This Activity exists because we want to show a submission directly when a comment URL is clicked,
 * where pull-to-collapse will take the user back to the previous screen. This is unlike
 * {@link SubredditActivity}, which takes user back to the submission's subreddit.
 */
public class SubmissionFragmentActivity extends DankPullCollapsibleActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
