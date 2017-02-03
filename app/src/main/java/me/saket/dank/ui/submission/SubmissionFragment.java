package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.app.Fragment;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import com.google.common.collect.FluentIterable;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import rx.Subscription;

public class SubmissionFragment extends Fragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_comments_list) RecyclerView commentsList;
    @BindView(R.id.submission_comments_progress) ProgressBar loadProgressBar;

    private CommentsAdapter commentsAdapter;
    private Subscription commentsSubscription;
    private Submission lastSubmission;

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

        toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onSubmissionToolbarUpClick());

        commentsList.setLayoutManager(new LinearLayoutManager(getActivity()));
        commentsList.setItemAnimator(new DefaultItemAnimator());
        commentsAdapter = new CommentsAdapter();
        commentsList.setAdapter(commentsAdapter);

        // TODO: 01/02/17 Preload Views for adapter rows.

        return fragmentLayout;
    }

    public void populate(Submission submission) {
        if (submission.equals(lastSubmission)) {
            return;
        }
        lastSubmission = submission;

        titleView.setText(submission.getTitle());
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));

        commentsAdapter.updateData(null);
        loadProgressBar.setVisibility(View.VISIBLE);

        // Load comments.
        commentsSubscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(Dank.reddit().fullSubmissionData(submission.getId())))
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .map(submissionData -> {
                    // Flatten the comments tree into one list so that it can be displayed in the RecyclerView.
                    CommentNode commentNode = submissionData.getComments();
                    List<CommentNode> flattenedComments = new ArrayList<>(commentNode.getTotalSize());
                    FluentIterable<CommentNode> commentNodes = commentNode.walkTree();
                    for (CommentNode nestedNode : commentNodes) {
                        flattenedComments.add(nestedNode);
                    }
                    return flattenedComments;
                })
                .compose(RxUtils.applySchedulers())
                .doOnTerminate(() -> loadProgressBar.setVisibility(View.GONE))
                .subscribe(commentsAdapter, logError("Couldn't get comments"));
    }

    /**
     * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward swipe == downward scroll and vice versa.
     * @return True to consume this touch event. False otherwise.
     */
    @Override
    public boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull) {
        Rect commentListBounds = new Rect();
        commentsList.getGlobalVisibleRect(commentListBounds);
        return commentListBounds.contains((int) downX, (int) downY) && commentsList.canScrollVertically(upwardPagePull ? 1 : -1);
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
