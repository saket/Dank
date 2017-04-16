package me.saket.dank.ui.subreddits;

import static me.saket.dank.di.Dank.subscriptionManager;
import static me.saket.dank.utils.CommonUtils.defaultIfNull;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndEnd;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.statusBarHeight;
import static me.saket.dank.utils.Views.touchLiesOn;
import static rx.Observable.fromCallable;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.zagum.expandicon.ExpandIconView;

import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.SubredditPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.RedditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.widgets.DankToolbar;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Subscription;

public class SubredditActivity extends DankPullCollapsibleActivity implements SubmissionFragment.Callbacks, SubscribeToNewSubredditDialog.Callback {

    private static final int REQUEST_CODE_LOGIN = 100;
    protected static final String KEY_INITIAL_SUBREDDIT_LINK = "initialSubredditLink";
    private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";

    @BindView(R.id.subreddit_root) IndependentExpandablePageLayout contentPage;
    @BindView(R.id.toolbar) DankToolbar toolbar;
    @BindView(R.id.subreddit_toolbar_close) View toolbarCloseButton;
    @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
    @BindView(R.id.subreddit_toolbar_title_arrow) ExpandIconView toolbarTitleArrowView;
    @BindView(R.id.subreddit_toolbar_title_container) ViewGroup toolbarTitleContainer;
    @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
    @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
    @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
    @BindView(R.id.subreddit_progress) View progressView;

    private String activeSubredditName;
    private SubmissionFragment submissionFragment;
    private SubRedditSubmissionsAdapter submissionsAdapter;

    protected static void addStartExtrasToIntent(RedditLink.Subreddit subredditLink, @Nullable Rect expandFromShape, Intent intent) {
        intent.putExtra(KEY_INITIAL_SUBREDDIT_LINK, subredditLink);
        intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean isPullCollapsible = !isTaskRoot();
        setPullToCollapseEnabled(isPullCollapsible);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit);
        ButterKnife.bind(this);
        setupContentExpandablePage(contentPage);

        // Add top-margin to make room for the status bar.
        int statusBarHeight = statusBarHeight(getResources());
        setPaddingTop(toolbar, statusBarHeight);
        setMarginTop(toolbarTitleContainer, statusBarHeight);
        setMarginTop(submissionList, statusBarHeight);
        setPaddingTop(toolbarSheet, statusBarHeight);

        findAndSetupToolbar();
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (isPullCollapsible) {
            contentPage.setNestedExpandablePage(submissionPage);
            expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));

            toolbarCloseButton.setVisibility(View.VISIBLE);
            toolbarCloseButton.setOnClickListener(__ -> NavUtils.navigateUpFromSameTask(this));
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Setup submission list.
        submissionList.setLayoutManager(submissionList.createLayoutManager());
        submissionList.setItemAnimator(new DefaultItemAnimator());
        submissionList.setExpandablePage(submissionPage, toolbarContainer);

        contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
            if (touchLiesOn(toolbarContainer, downX, downY)) {
                if (touchLiesOn(toolbarSheet, downX, downY) && isSubredditPickerVisible()) {
                    boolean intercepted = findSubredditPickerSheet().shouldInterceptPullToCollapse(downX, downY);
                    if (intercepted) {
                        return true;
                    }
                }
                return false;
            }

            //noinspection SimplifiableIfStatement
            if (touchLiesOn(submissionList, downX, downY)) {
                return submissionList.canScrollVertically(upwardPagePull ? 1 : -1);
            }

            return false;
        });

        if (savedInstanceState != null) {
            submissionList.handleOnRestoreInstanceState(savedInstanceState);
        }

        submissionsAdapter = new SubRedditSubmissionsAdapter();
        submissionsAdapter.setOnItemClickListener((submission, submissionItemView, submissionId) -> {
            DankSubmissionRequest submissionRequest = DankSubmissionRequest.builder(submission.getId())
                    .commentSort(defaultIfNull(submission.getSuggestedSort(), DankRedditClient.DEFAULT_COMMENT_SORT))
                    .build();
            submissionFragment.populateUi(submission, submissionRequest);

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
        submissionFragment = (SubmissionFragment) getSupportFragmentManager().findFragmentById(submissionPage.getId());
        if (submissionFragment == null) {
            submissionFragment = SubmissionFragment.create();
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(submissionPage.getId(), submissionFragment)
                .commit();

        // Get frontpage (or retained subreddit's) submissions.
        if (savedInstanceState != null) {
            loadSubmissions(savedInstanceState.getString(KEY_ACTIVE_SUBREDDIT));

        } else if (getIntent().hasExtra(KEY_INITIAL_SUBREDDIT_LINK)) {
            loadSubmissions(((RedditLink.Subreddit) getIntent().getSerializableExtra(KEY_INITIAL_SUBREDDIT_LINK)).name);

        } else {
            loadSubmissions(Dank.subscriptionManager().defaultSubreddit());
        }

        setupToolbarSheet();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        submissionList.handleOnSaveInstance(outState);
        outState.putString(KEY_ACTIVE_SUBREDDIT, activeSubredditName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void setTitle(CharSequence subredditName) {
        boolean isFrontpage = subscriptionManager().isFrontpage(subredditName.toString());
        toolbarTitleView.setText(isFrontpage ? getString(R.string.app_name) : subredditName);
    }

    // TODO: 15/04/17 Cancel existing loads before loading a new one.
    private void loadSubmissions(String subredditName) {
        activeSubredditName = subredditName;
        submissionsAdapter.updateData(null);

        setTitle(subredditName);

        SubredditPaginator subredditPaginator = Dank.reddit().subredditPaginator(subredditName);
        Subscription subscription = Dank.reddit()
                .withAuth(fromCallable(() -> subredditPaginator.next()))
                .compose(applySchedulers())
                .compose(doOnStartAndEnd(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(submissionsAdapter, logError("Couldn't get front-page"));
        unsubscribeOnDestroy(subscription);
    }

    private void setupToolbarSheet() {
        toolbarSheet.hideOnOutsideClick(submissionList);
        toolbarSheet.setStateChangeListener(state -> {
            switch (state) {
                case EXPANDING:
                    if (isSubredditPickerVisible()) {
                        // When subreddit picker is showing, we'll show a "configure subreddits" button in the toolbar.
                        invalidateOptionsMenu();

                    } else if (isUserProfileSheetVisible()) {
                        setTitle(getString(R.string.user_name_u_prefix, Dank.reddit().loggedInUserName()));
                    }

                    toolbarTitleArrowView.setState(ExpandIconView.LESS, true);
                    break;

                case EXPANDED:
                    break;

                case COLLAPSING:
                    if (isSubredditPickerVisible()) {
                        Keyboards.hide(this, toolbarSheet);

                    } else if (isUserProfileSheetVisible()) {
                        setTitle(activeSubredditName);
                    }
                    toolbarTitleArrowView.setState(ExpandIconView.MORE, true);
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
        getMenuInflater().inflate(R.menu.menu_subreddit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_user_profile:
                if (Dank.reddit().isUserLoggedIn()) {
                    showUserProfileSheet();
                } else {
                    LoginActivity.startForResult(this, REQUEST_CODE_LOGIN);
                }
                return true;

            case R.id.action_preferences:
                UserPreferencesActivity.start(this);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

// ======== SUBREDDIT PICKER SHEET ======== //

    @OnClick(R.id.subreddit_toolbar_title)
    void onClickToolbarTitle() {
        if (toolbarSheet.isExpandedOrExpanding() || isUserProfileSheetVisible()) {
            toolbarSheet.collapse();

        } else {
            SubredditPickerSheetView pickerSheet = SubredditPickerSheetView.showIn(toolbarSheet, contentPage);
            pickerSheet.post(() -> toolbarSheet.expand());
            pickerSheet.setCallbacks(new SubredditPickerSheetView.Callbacks() {
                @Override
                public void onSelectSubreddit(String subredditName) {
                    toolbarSheet.collapse();
                    if (!subredditName.equalsIgnoreCase(activeSubredditName)) {
                        loadSubmissions(subredditName);
                    }
                }

                @Override
                public void onClickAddNewSubreddit() {
                    SubscribeToNewSubredditDialog.show(getSupportFragmentManager());
                }
            });
        }
    }

    @Override
    public void onEnterNewSubredditForSubscription(Subreddit newSubreddit) {
        if (isSubredditPickerVisible()) {
            findSubredditPickerSheet().subscribeTo(newSubreddit);
        }
    }

    /**
     * Whether the subreddit picker is visible, albeit partially.
     */
    private boolean isSubredditPickerVisible() {
        return !toolbarSheet.isCollapsed() && toolbarSheet.getChildAt(0) instanceof SubredditPickerSheetView;
    }

    private SubredditPickerSheetView findSubredditPickerSheet() {
        return ((SubredditPickerSheetView) toolbarSheet.getChildAt(0));
    }

// ======== USER PROFILE SHEET ======== //

    void showUserProfileSheet() {
        UserProfileSheetView pickerSheet = UserProfileSheetView.showIn(toolbarSheet);
        pickerSheet.post(() -> toolbarSheet.expand());
    }

    private boolean isUserProfileSheetVisible() {
        return !toolbarSheet.isCollapsed() && toolbarSheet.getChildAt(0) instanceof UserProfileSheetView;
    }

    @Override
    public void onBackPressed() {
        if (submissionPage.isExpandedOrExpanding()) {
            submissionList.collapse();

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
            showUserProfileSheet();

            // Reload submissions if we're on the frontpage because the frontpage
            // submissions will change if the subscriptions change.
            if (subscriptionManager().isFrontpage(activeSubredditName)) {
                loadSubmissions(activeSubredditName);
            }

            // Reload subreddit subscriptions. Not implementing onError() is intentional.
            // This code is not supposed to fail :/
            subscriptionManager().removeAll().subscribe();

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}
