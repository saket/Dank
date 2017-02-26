package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.dean.jraw.paginators.SubredditPaginator;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;
import timber.log.Timber;

public class SubredditActivity extends DankActivity implements SubmissionFragment.Callbacks {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
    @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
    @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
    @BindView(R.id.subreddit_progress) ProgressBar progressBar;
    private SubmissionFragment submissionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        findAndSetupToolbar(false);
        ButterKnife.bind(this);

        // Add top-margin to make room for the status bar.
        ButterKnife.findById(this, Window.ID_ANDROID_CONTENT).setOnApplyWindowInsetsListener((v, insets) -> {
            Views.setMarginTop(toolbarContainer, insets.getSystemWindowInsetTop());
            Views.setMarginTop(submissionList, insets.getSystemWindowInsetTop());
            return insets;
        });

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        setTitle(getString(R.string.app_name).toLowerCase(Locale.ENGLISH));

        // Setup submission list.
        submissionList.setLayoutManager(submissionList.createLayoutManager());
        submissionList.setItemAnimator(new DefaultItemAnimator());
        submissionList.setExpandablePage(submissionPage, toolbarContainer);

        SubRedditSubmissionsAdapter submissionsAdapter = new SubRedditSubmissionsAdapter();
        submissionList.setAdapter(submissionsAdapter);

        submissionsAdapter.setOnItemClickListener((submission, submissionItemView, submissionId) -> {
            submissionFragment.populateUi(submission);
            submissionPage.post(() -> {
                // Posing the expand() call to the page's message queue seems to result in a smoother
                // expand animation. I assume this is because the expand() call only executes once the
                // page is done updating its views for this new submission. This might be a placebo too
                // and without enough testing I cannot be sure about this.
                submissionList.expandItem(submissionList.indexOfChild(submissionItemView), submissionId);
            });
        });

        // Setup submission fragment.
        submissionFragment = (SubmissionFragment) getSupportFragmentManager().findFragmentById(submissionPage.getId());
        if (submissionFragment == null) {
            submissionFragment = SubmissionFragment.create();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(submissionPage.getId(), submissionFragment)
                .commit();

        // Get frontpage submissions.
        SubredditPaginator frontPagePaginator = Dank.reddit().frontPagePaginator();
        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(frontPagePaginator.next()))
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .compose(RxUtils.applySchedulers())
                .doOnTerminate(() -> progressBar.setVisibility(View.GONE))
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        toolbarSheet.hideOnOutsideTouch(submissionList);
        toolbarSheet.setStateChangeListener(isVisible -> {
            Fragment pickerFragment = getSupportFragmentManager().findFragmentByTag(SubredditPickerFragment.TAG);
            Timber.i("isVisible: %s", isVisible);

            if (isVisible) {
                if (pickerFragment == null) {
                    pickerFragment = SubredditPickerFragment.create();
                }
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(toolbarSheet.getId(), pickerFragment, SubredditPickerFragment.TAG)
                        .commitNow();
                toolbarSheet.show();

            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .remove(pickerFragment)
                        .commitNow();
                toolbarSheet.hide();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        submissionList.handleOnSaveInstance(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        submissionList.handleOnRestoreInstanceState(savedInstanceState);
    }

    @OnClick(R.id.subreddit_toolbar_title)
    void onClickSubredditPicker() {
        toolbarSheet.toggleVisibility();
    }

    @Override
    public void setTitle(CharSequence title) {
        toolbarTitleView.setText(title);
    }

// ======== NAVIGATION ======== //

    @Override
    public void onSubmissionToolbarUpClick() {
        submissionList.collapse();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_subreddit, menu);

        // Click listeners for a menu item with an action view must be set manually.
        MenuItem changeOrderMenuItem = menu.findItem(R.id.action_login);
        Button button = (Button) ((ViewGroup) changeOrderMenuItem.getActionView()).getChildAt(0);
        button.setText(Dank.reddit().isUserLoggedIn() ? R.string.logout : R.string.login);
        button.setOnClickListener(v -> {
            if (Dank.reddit().isUserLoggedIn()) {
                Dank.reddit().logout();
                button.setText(R.string.login);

            } else {
                LoginActivity.start(this);
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (submissionPage.isExpanded()) {
            boolean backPressHandled = submissionFragment.handleBackPress();
            if (!backPressHandled) {
                submissionList.collapse();
            }
            return;
        }

        super.onBackPressed();
    }

}
