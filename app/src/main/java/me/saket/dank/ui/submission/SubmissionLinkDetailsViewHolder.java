package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;

import android.content.res.Resources;
import android.net.Uri;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.Link;
import me.saket.dank.data.RedditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.DankSubmissionRequest;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Used when a submission points to a Reddit hosted URL, which can be another submission or a subreddit or a user.
 * Manages Views for showing details of the linked URL.
 */
public class SubmissionLinkDetailsViewHolder {

    @BindView(R.id.submission_linkdetails_icon) ImageView iconView;
    @BindView(R.id.submission_linkdetails_title) TextView titleView;
    @BindView(R.id.submission_linkdetails_subtitle) TextView subtitleView;
    @BindView(R.id.submission_linkdetails_progress) View progressView;

    private ViewGroup itemView;

    public SubmissionLinkDetailsViewHolder(ViewGroup linkedRedditLinkView) {
        this.itemView = linkedRedditLinkView;
        ButterKnife.bind(this, linkedRedditLinkView);
        linkedRedditLinkView.setClipToOutline(true);
    }

    public void setVisible(boolean visible) {
        itemView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Load and show title of user/subreddit/submission.
     */
    public Subscription populate(RedditLink redditLink) {
        Resources resources = titleView.getResources();
        titleView.setMaxLines(1);

        if (redditLink instanceof RedditLink.Subreddit) {
            iconView.setContentDescription(resources.getString(R.string.submission_linkdetails_linked_subreddit));
            iconView.setImageResource(R.drawable.ic_subreddits_24dp);
            titleView.setText(resources.getString(R.string.subreddit_name_r_prefix, ((RedditLink.Subreddit) redditLink).name));
            subtitleView.setText(R.string.submission_linkedredditurl_tap_to_open_subreddit);
            progressView.setVisibility(View.GONE);
            return Subscriptions.unsubscribed();

        } else if (redditLink instanceof RedditLink.User) {
            iconView.setContentDescription(resources.getString(R.string.submission_linkdetails_linked_profile));
            iconView.setImageResource(R.drawable.ic_user_profile_24dp);
            titleView.setText(resources.getString(R.string.user_name_u_prefix, ((RedditLink.User) redditLink).name));
            subtitleView.setText(R.string.submission_linkedredditurl_tap_to_open_profile);
            progressView.setVisibility(View.GONE);
            return Subscriptions.unsubscribed();

        } else if (redditLink instanceof RedditLink.Submission) {
            iconView.setContentDescription(resources.getString(R.string.submission_linkdetails_linked_submission));
            iconView.setImageResource(R.drawable.ic_submission_24dp);
            titleView.setText(Uri.parse(((RedditLink.Submission) redditLink).url).getPath());
            subtitleView.setText(R.string.submission_linkedredditurl_tap_to_open_submission);
            return loadSubmissionData((RedditLink.Submission) redditLink);

        } else {
            throw new UnsupportedOperationException("Unknown reddit link: " + redditLink);
        }
    }

    private Subscription loadSubmissionData(RedditLink.Submission submissionLink) {
        DankSubmissionRequest requestWithoutComments = DankSubmissionRequest.builder(submissionLink.id)
                // We could make commentSort nullable, but the null check is there to detect any accidental absence of sort.
                .commentSort(DankRedditClient.DEFAULT_COMMENT_SORT)
                .commentLimit(0)
                .build();

        return Dank.reddit()
                .withAuth(Dank.reddit().submission(requestWithoutComments))
                .compose(applySchedulers())
                .compose(doOnStartAndFinish(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(submission -> {
                    //noinspection deprecation
                    titleView.setText(Html.fromHtml(submission.getTitle()));
                    titleView.setMaxLines(Integer.MAX_VALUE);

                }, logError("Couldn't load submission details"));
    }

    /**
     * Show information of an external link. Extracts meta-data from the URL to get the favicon and the title.
     */
    public Subscription populate(Link.External externalLink) {
        Resources resources = titleView.getResources();

        iconView.setContentDescription(resources.getString(R.string.submission_linkdetails_linked_url));
        iconView.setImageResource(R.drawable.ic_link_black_24dp);
        titleView.setText(externalLink.url);
        subtitleView.setText(R.string.submission_linkedredditurl_tap_to_open_link);
        progressView.setVisibility(View.VISIBLE);
        return Subscriptions.unsubscribed();
    }

}
