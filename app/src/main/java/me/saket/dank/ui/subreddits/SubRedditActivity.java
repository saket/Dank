package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.widget.Toolbar;

import net.dean.jraw.paginators.SubredditPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.submission.SubmissionFragment;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import rx.Subscription;

public class SubRedditActivity extends DankActivity implements SubmissionFragment.Callbacks {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;

    private SubmissionFragment submissionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        findAndSetupToolbar(false);
        ButterKnife.bind(this);

        // Setup submission list.
        SubRedditSubmissionsAdapter submissionsAdapter = new SubRedditSubmissionsAdapter();
        submissionList.setItemAnimator(new DefaultItemAnimator());
        submissionList.setAdapter(submissionsAdapter);
        submissionList.setExpandablePage(submissionPage, toolbar);

        submissionsAdapter.setOnItemClickListener((submission, submissionItemView, submissionId) -> {
            submissionList.expandItem(submissionList.indexOfChild(submissionItemView), submissionId);
            submissionFragment.populate(submission);
        });

        // Setup submission fragment.
        submissionFragment = (SubmissionFragment) getFragmentManager().findFragmentById(submissionPage.getId());
        if (submissionFragment == null) {
            submissionFragment = SubmissionFragment.create();
        }
        getFragmentManager()
                .beginTransaction()
                .replace(submissionPage.getId(), submissionFragment)
                .commit();

        // Get frontpage submissions.
        SubredditPaginator frontPagePaginator = Dank.reddit().frontPagePaginator();
        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(frontPagePaginator.next()))
                .compose(RxUtils.applySchedulers())
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    @Override
    public void onSubmissionToolbarUpClick() {
        submissionList.collapse();
    }

}
