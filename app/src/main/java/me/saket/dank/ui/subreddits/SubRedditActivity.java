package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import net.dean.jraw.paginators.SubredditPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;

public class SubredditActivity extends DankActivity implements SubmissionFragment.Callbacks {

    private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
    @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
    @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
    @BindView(R.id.subreddit_progress) View progressView;

    private SubmissionFragment submissionFragment;
    private SubRedditSubmissionsAdapter submissionsAdapter;
    private String activeSubreddit;

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

        // Setup submission list.
        submissionList.setLayoutManager(submissionList.createLayoutManager());
        submissionList.setItemAnimator(new DefaultItemAnimator());
        submissionList.setExpandablePage(submissionPage, toolbarContainer);

        submissionsAdapter = new SubRedditSubmissionsAdapter();
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
        submissionList.setAdapter(submissionsAdapter);

        // Setup submission page.
        submissionFragment = (SubmissionFragment) getFragmentManager().findFragmentById(submissionPage.getId());
        if (submissionFragment == null) {
            submissionFragment = SubmissionFragment.create();
        }
        getFragmentManager()
                .beginTransaction()
                .replace(submissionPage.getId(), submissionFragment)
                .commit();

        // Get frontpage (or retained subreddit's) submissions.
        if (savedInstanceState != null) {
            activeSubreddit = savedInstanceState.getString(KEY_ACTIVE_SUBREDDIT);
        }
        loadSubmissions(activeSubreddit);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupToolbarSheet();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        submissionList.handleOnSaveInstance(outState);
        outState.putString(KEY_ACTIVE_SUBREDDIT, activeSubreddit);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        submissionList.handleOnRestoreInstanceState(savedInstanceState);
    }

    private void loadSubmissions(String subreddit) {
        activeSubreddit = subreddit;
        submissionsAdapter.updateData(null);

        toolbarTitleView.setText(TextUtils.isEmpty(subreddit) || subreddit.equals(getString(R.string.frontpage_subreddit_name))
                ? getString(R.string.app_name)
                : getString(R.string.subreddit_name_r_prefix, subreddit));

        SubredditPaginator subredditPaginator = Dank.reddit().subredditPaginator(subreddit);
        Subscription subscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(subredditPaginator.next()))
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .compose(RxUtils.applySchedulers())
                .compose(doOnStartAndFinish(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    private void setupToolbarSheet() {
        toolbarSheet.hideOnOutsideTouch(submissionList);
        toolbarSheet.setStateChangeListener(state -> {
            switch (state) {
                case EXPANDING:
                    // When sub-reddits are showing, we'll show a "configure subreddits" button in the toolbar.
                    invalidateOptionsMenu();
                    break;

                case COLLAPSING:
                    Keyboards.hide(this, toolbarSheet);
                    invalidateOptionsMenu();
                    break;

                case COLLAPSED:
                    toolbarSheet.removeAllViews();
                    toolbarSheet.collapse();
                    break;
            }
        });
    }

    @OnClick(R.id.subreddit_toolbar_title)
    void onClickSubredditPicker() {
        if (toolbarSheet.isExpandedOrExpanding()) {
            toolbarSheet.collapse();

        } else {
            SubredditPickerView pickerView = SubredditPickerView.showIn(toolbarSheet);
            pickerView.post(() -> toolbarSheet.expand());
            pickerView.setOnSubredditClickListener(subreddit -> {
                toolbarSheet.collapse();
                if (!subreddit.equals(activeSubreddit)) {
                    loadSubmissions(subreddit);
                }
            });
        }
    }

// ======== NAVIGATION ======== //

    @Override
    public void onSubmissionToolbarUpClick() {
        submissionList.collapse();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (toolbarSheet.isExpandedOrExpanding()) {
            getMenuInflater().inflate(R.menu.menu_subreddit_picker, menu);

            // Click listeners for a menu item with an action view must be set manually.
            MenuItem changeOrderMenuItem = menu.findItem(R.id.action_manage_subreddits);
            Button button = (Button) ((ViewGroup) changeOrderMenuItem.getActionView()).getChildAt(0);
            button.setText(R.string.manage);

        } else {
            getMenuInflater().inflate(R.menu.menu_subreddit, menu);

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
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (submissionPage.isExpanded()) {
            boolean backPressHandled = submissionFragment.handleBackPress();
            if (!backPressHandled) {
                submissionList.collapse();
            }

        } else if (toolbarSheet.isExpandedOrExpanding()) {
            toolbarSheet.collapse();

        } else {
            super.onBackPressed();
        }
    }

}
