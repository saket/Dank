package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.app.Fragment;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.io.IOException;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddits.SubRedditActivity;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import rx.Subscription;
import timber.log.Timber;

public class SubmissionFragment extends Fragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    private static final String KEY_SUBMISSION_JSON = "submissionJson";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_linked_image) ImageView submissionImageView;
    @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
    @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
    @BindView(R.id.submission_comment_list) RecyclerView commentList;
    @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;

    @BindDrawable(R.drawable.ic_close_black_24dp) Drawable closeIconDrawable;

    private CommentsAdapter commentsAdapter;
    private Subscription commentsSubscription;
    private CommentsCollapseHelper commentsCollapseHelper;
    private Submission activeSubmission;

    public interface Callbacks {
        void onSubmissionToolbarUpClick();
    }

    public static SubmissionFragment create() {
        return new SubmissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
        ButterKnife.bind(this, fragmentLayout);

        // Add a close icon to the toolbar.
        closeIconDrawable = closeIconDrawable.mutate();
        closeIconDrawable.setTint(ContextCompat.getColor(getActivity(), R.color.gray_500));
        toolbar.setNavigationIcon(closeIconDrawable);
        toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onSubmissionToolbarUpClick());

        // Setup comment list and its adapter.
        commentsAdapter = new CommentsAdapter();
        commentsAdapter.setOnCommentClickListener(comment -> {
            // Collapse/expand on tap.
            commentsAdapter.updateData(commentsCollapseHelper.toggleCollapseAndGet(comment));
        });
        commentList.setAdapter(RecyclerAdapterWithHeader.wrap(commentsAdapter, commentsHeaderView));
        commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
        commentList.setItemAnimator(new DefaultItemAnimator());

        commentsCollapseHelper = new CommentsCollapseHelper();

        // TODO: 17/02/17 Add ScrollingRecyclerViewSheet.setCollapsible().
        // TODO: 01/02/17 Should we preload Views for adapter rows?

        // Restore submission if the Activity was recreated.
        if (savedInstanceState != null) {
            onRestoreSavedInstanceState(savedInstanceState);
        }
        return fragmentLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (activeSubmission != null) {
            JsonNode dataNode = activeSubmission.getDataNode();
            outState.putString(KEY_SUBMISSION_JSON, dataNode.toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void onRestoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_SUBMISSION_JSON)) {
            try {
                String submissionJson = savedInstanceState.getString(KEY_SUBMISSION_JSON);
                JsonNode jsonNode = new ObjectMapper().readTree(submissionJson);
                populateUi(new Submission(jsonNode));

            } catch (IOException e) {
                Timber.e(e, "Couldn't deserialize Submission for state restoration");
            }
        }
    }

    /**
     * Update the submission to be shown. Since this Fragment is retained by {@link SubRedditActivity},
     * we only update the UI everytime a new submission is to be shown.
     */
    public void populateUi(Submission submission) {
        activeSubmission = submission;

        // Reset everything.
        commentListParentSheet.scrollTo(0, 0);
        commentListParentSheet.setScrollingEnabled(false);
        commentsCollapseHelper.reset();
        commentsAdapter.updateData(null);

        // Update submission information.
        titleView.setText(submission.getTitle());
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));

        // Load self-text/media/webpage.
        loadSubmissionContent(submission);

        // Load new comments.
        commentsLoadProgressView.setVisibility(View.VISIBLE);
        commentsSubscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(Dank.reddit().fullSubmissionData(submission.getId())))
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .map(submissionData -> {
                    CommentNode commentNode = submissionData.getComments();
                    commentsCollapseHelper.setupWith(commentNode);
                    return commentsCollapseHelper.flattenExpandedComments();
                })
                .compose(applySchedulers())
                .doOnTerminate(() -> commentsLoadProgressView.setVisibility(View.GONE))
                .subscribe(commentsAdapter, logError("Couldn't get comments"));
    }

    private void loadSubmissionContent(Submission submission) {
//        Timber.d("-------------------------------------------");
//        Timber.i("%s", submission.getTitle());
//        Timber.i("Post hint: %s", submission.getPostHint());
//        Timber.i("%s", submission.getDataNode().toString());

        switch (submission.getPostHint()) {
            case IMAGE:
                Glide.with(this)
                        .load(submission.getUrl())
                        .priority(Priority.IMMEDIATE)
                        .into(new GlideDrawableImageViewTarget(submissionImageView) {
                            @Override
                            protected void setResource(GlideDrawable resource) {
                                super.setResource(resource);
                                submissionImageView.post(() -> {
                                    // Scroll the comments sheet to reveal the image.
                                    int distanceToReveal = Math.min(submissionImageView.getHeight(), commentListParentSheet.getHeight() / 2);
                                    commentListParentSheet.smoothScrollTo(distanceToReveal);
                                    commentListParentSheet.setScrollingEnabled(true);

                                    // TransitionDrawable (used by Glide for fading-in) messes up the scaleType. Set it everytime.
                                    submissionImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                });
                            }
                        });
                break;

            case SELF:
                selfPostTextView.setText(submission.getSelftext());
                break;

            case VIDEO:
            case UNKNOWN:
            case LINK:
                // TODO: 12/02/17.
                submissionImageView.setVisibility(View.GONE);
                break;

            default:
                throw new UnsupportedOperationException("Unknown post hint: " + submission.getPostHint());
        }

        selfPostTextView.setVisibility(submission.getPostHint() == Submission.PostHint.SELF ? View.VISIBLE : View.GONE);
        submissionImageView.setVisibility(submission.getPostHint() == Submission.PostHint.IMAGE ? View.VISIBLE : View.GONE);
    }

// ======== EXPANDABLE PAGE CALLBACKS ======== //

    /**
     * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward pull == downward scroll and vice versa.
     * @return True to consume this touch event. False otherwise.
     */
    @Override
    public boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull) {
        Rect commentSheetBounds = new Rect();
        commentListParentSheet.getGlobalVisibleRect(commentSheetBounds);
        boolean touchInsideCommentsSheet = commentSheetBounds.contains((int) downX, (int) downY);

        //noinspection SimplifiableIfStatement
        if (touchInsideCommentsSheet) {
            return upwardPagePull
                    ? commentListParentSheet.canScrollUpwardsAnyFurther()
                    : commentListParentSheet.canScrollDownwardsAnyFurther();
        } else {
            return false;
        }
    }

    @Override
    public void onPageAboutToExpand(long expandAnimDuration) {

    }

    @Override
    public void onPageExpanded() {

    }

    @Override
    public void onPageAboutToCollapse(long collapseAnimDuration) {

    }

    @Override
    public void onPageCollapsed() {
        if (commentsSubscription != null) {
            commentsSubscription.unsubscribe();
        }
    }

}
