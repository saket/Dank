package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.os.Bundle;
import android.os.Looper;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toolbar;

import net.dean.jraw.http.NetworkException;
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
import rx.Observable;
import rx.Subscription;
import timber.log.Timber;

public class SubRedditActivity extends DankActivity implements SubmissionFragment.Callbacks {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
    @BindView(R.id.subreddit_progress) ProgressBar progressBar;

    private SubmissionFragment submissionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        findAndSetupToolbar(false);
        ButterKnife.bind(this);

        // Setup submission list.
        submissionList.setLayoutManager(submissionList.createLayoutManager());
        submissionList.setItemAnimator(new DefaultItemAnimator());
        submissionList.setExpandablePage(submissionPage, toolbar);

        SubRedditSubmissionsAdapter submissionsAdapter = new SubRedditSubmissionsAdapter();
        submissionList.setAdapter(submissionsAdapter);

        submissionsAdapter.setOnItemClickListener((submission, submissionItemView, submissionId) -> {
            submissionList.expandItem(submissionList.indexOfChild(submissionItemView), submissionId);
            submissionFragment.populate(submission);
        });

        // Setup submission fragment.
        submissionFragment = (SubmissionFragment) getFragmentManager().findFragmentById(submissionPage.getId());
        if (submissionFragment == null) {
            submissionFragment = SubmissionFragment.create();
        }
        submissionPage.addCallbacks(submissionFragment);
        submissionPage.setPullToCollapseIntercepter(submissionFragment);

        getFragmentManager()
                .beginTransaction()
                .replace(submissionPage.getId(), submissionFragment)
                .commit();

        // Get frontpage submissions.
        SubredditPaginator frontPagePaginator = Dank.reddit().frontPagePaginator();
        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(frontPagePaginator.next()))
                .retryWhen(errors -> errors.flatMap(error -> {
                    if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 401) {
                        // Re-try authenticating.
                        Timber.w("Attempting to refresh token");
                        return Observable
                                .just(Dank.reddit().refreshApiToken())
                                .doOnNext(booleanObservable -> {
                                    boolean isMainThread = Looper.getMainLooper() == Looper.myLooper();
                                    Timber.i("isMainThread: %s", isMainThread);
                                })
                                .map(__ -> null);

                    } else {
                        return Observable.error(error);
                    }
                }))
                .compose(RxUtils.applySchedulers())
                .doOnTerminate(() -> progressBar.setVisibility(View.GONE))
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO: 01/02/17 Retain expandable page's state.
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSubmissionToolbarUpClick() {
        submissionList.collapse();
    }

}
