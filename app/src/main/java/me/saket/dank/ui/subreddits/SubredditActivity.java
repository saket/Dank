package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.fromCallable;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import net.dean.jraw.paginators.SubredditPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.data.DankSubreddit;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;

public class SubredditActivity extends DankActivity implements SubmissionFragment.Callbacks {

    private static final int REQUEST_CODE_LOGIN = 100;
    private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
    @BindView(R.id.subreddit_toolbar_title_container) ViewGroup toolbarTitleContainer;
    @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
    @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
    @BindView(R.id.subreddit_progress) View progressView;

    DankSubreddit activeSubreddit;

    private SubmissionFragment submissionFragment;
    private SubRedditSubmissionsAdapter submissionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        findAndSetupToolbar(false);
        ButterKnife.bind(this);

        // Add top-margin to make room for the status bar.
        Views.getStatusBarHeight(this, statusBarHeight -> {
            Views.setMarginTop(toolbarContainer, statusBarHeight);
            Views.setMarginTop(submissionList, statusBarHeight);
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
        activeSubreddit = savedInstanceState != null
                ? savedInstanceState.getParcelable(KEY_ACTIVE_SUBREDDIT)
                : DankSubreddit.createFrontpage(getString(R.string.frontpage_subreddit_name));
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
        outState.putParcelable(KEY_ACTIVE_SUBREDDIT, activeSubreddit);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        submissionList.handleOnRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void setTitle(CharSequence title) {
        toolbarTitleView.setText(title);
    }

    public void setTitle(DankSubreddit subreddit) {
        setTitle(subreddit.isFrontpage()
                ? getString(R.string.app_name)
                : getString(R.string.subreddit_name_r_prefix, subreddit.displayName()));
    }

    private void loadSubmissions(DankSubreddit subreddit) {
        activeSubreddit = subreddit;
        submissionsAdapter.updateData(null);

        setTitle(subreddit);

        SubredditPaginator subredditPaginator = Dank.reddit().subredditPaginator(subreddit.name());
        Subscription subscription = Dank.reddit()
                .withAuth(fromCallable(() -> subredditPaginator.next()))
                .compose(applySchedulers())
                .compose(doOnStartAndFinish(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    private void setupToolbarSheet() {
        toolbarSheet.hideOnOutsideTouch(submissionList);
        toolbarSheet.setStateChangeListener(state -> {
            switch (state) {
                case EXPANDING:
                    if (isSubredditPickerVisible()) {
                        // When subreddit picker is showing, we'll show a "configure subreddits" button in the toolbar.
                        invalidateOptionsMenu();

                    } else if (isUserProfileSheetVisible()) {
                        setTitle(getString(R.string.user_name_u_prefix, Dank.reddit().loggedInUserName()));
                    }
                    break;

                case EXPANDED:
                    break;

                case COLLAPSING:
                    if (isSubredditPickerVisible()) {
                        Keyboards.hide(this, toolbarSheet);
                        invalidateOptionsMenu();

                    } else if (isUserProfileSheetVisible()) {
                        setTitle(activeSubreddit);
                    }
                    break;

                case COLLAPSED:
                    toolbarSheet.removeAllViews();
                    toolbarSheet.collapse();
                    break;
            }
        });
    }

// ======== NAVIGATION ======== //

    @Override
    public void onClickSubmissionToolbarUp() {
        submissionList.collapse();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isSubredditPickerExpanded()) {
            getMenuInflater().inflate(R.menu.menu_subreddit_picker, menu);

            // Click listeners for a menu item with an action view must be set manually.
            MenuItem changeOrderMenuItem = menu.findItem(R.id.action_manage_subreddits);
            Button button = (Button) ((ViewGroup) changeOrderMenuItem.getActionView()).getChildAt(0);
            button.setText(R.string.manage);

        } else {
            getMenuInflater().inflate(R.menu.menu_subreddit, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_user_profile:
                if (Dank.reddit().isUserLoggedIn()) {
                    onClickUserProfileMenu();
                } else {
                    LoginActivity.startForResult(this, REQUEST_CODE_LOGIN);
                }
                return true;

            case R.id.action_preferences:
                UserPreferencesActivity.start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.subreddit_toolbar_title)
    void onClickSubredditPicker() {
        if (toolbarSheet.isExpandedOrExpanding()) {
            toolbarSheet.collapse();

        } else {
            SubredditPickerSheetView pickerSheet = SubredditPickerSheetView.showIn(toolbarSheet);
            pickerSheet.post(() -> toolbarSheet.expand());
            pickerSheet.setOnSubredditClickListener(subreddit -> {
                toolbarSheet.collapse();
                if (!subreddit.equals(activeSubreddit)) {
                    loadSubmissions(subreddit);
                }
            });
        }
    }

    void onClickUserProfileMenu() {
        if (toolbarSheet.isExpandedOrExpanding()) {
            toolbarSheet.collapse();

        } else {
            UserProfileSheetView pickerSheet = UserProfileSheetView.showIn(toolbarSheet);
            pickerSheet.post(() -> toolbarSheet.expand());
        }
    }

    /**
     * Whether the subreddit picker is visible, albeit partially.
     */
    private boolean isSubredditPickerVisible() {
        return !toolbarSheet.isCollapsed() && toolbarSheet.getChildAt(0) instanceof SubredditPickerSheetView;
    }

    /**
     * Whether the subreddit picker is "fully" visible.
     */
    private boolean isSubredditPickerExpanded() {
        return toolbarSheet.isExpandedOrExpanding() && toolbarSheet.getChildAt(0) instanceof SubredditPickerSheetView;
    }

    private boolean isUserProfileSheetVisible() {
        return !toolbarSheet.isCollapsed() && toolbarSheet.getChildAt(0) instanceof UserProfileSheetView;
    }

    @Override
    public void onBackPressed() {
        if (submissionPage.isExpanded()) {
            boolean backPressHandled = submissionFragment.handleBackPress();
            if (!backPressHandled) {
                submissionList.collapse();
            }

        } else if (!toolbarSheet.isCollapsed()) {
            toolbarSheet.collapse();

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            // Show user's profile.
            onClickUserProfileMenu();

            // Reload submissions if we're on the frontpage.
            if (activeSubreddit.isFrontpage()) {
                loadSubmissions(activeSubreddit);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
