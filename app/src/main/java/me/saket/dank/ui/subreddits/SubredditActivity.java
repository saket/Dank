package me.saket.dank.ui.subreddits;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.di.Dank.subscriptions;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.zagum.expandicon.ExpandIconView;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.Sorting;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.CachedSubmissionFolder;
import me.saket.dank.ui.submission.SortingAndTimePeriod;
import me.saket.dank.ui.submission.SubmissionFragment;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.widgets.DankToolbar;
import me.saket.dank.widgets.EmptyStateView;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

public class SubredditActivity extends DankPullCollapsibleActivity implements SubmissionFragment.Callbacks, NewSubredditSubscriptionDialog.Callback {

  public static final int REQUEST_CODE_LOGIN = 100;
  protected static final String KEY_INITIAL_SUBREDDIT_LINK = "initialSubredditLink";
  private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";
  private static final String KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE = "isSubredditPickerVisible";
  private static final String KEY_IS_USER_PROFILE_SHEET_VISIBLE = "isUserProfileSheetVisible";
  private static final String KEY_SORTING_AND_TIME_PERIOD = "sortingAndTimePeriod";

  @BindView(R.id.subreddit_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.toolbar) DankToolbar toolbar;
  @BindView(R.id.subreddit_toolbar_close) View toolbarCloseButton;
  @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
  @BindView(R.id.subreddit_toolbar_title_arrow) ExpandIconView toolbarTitleArrowView;
  @BindView(R.id.subreddit_toolbar_title_container) ViewGroup toolbarTitleContainer;
  @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
  @BindView(R.id.subreddit_sorting_mode_container) ViewGroup sortingModeContainer;
  @BindView(R.id.subreddit_sorting_mode) Button sortingModeButton;
  @BindView(R.id.subreddit_subscribe) Button subscribeButton;
  @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
  @BindView(R.id.subreddit_submission_page) ExpandablePageLayout submissionPage;
  @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
  @BindView(R.id.subreddit_progress) View fullscreenProgressView;
  @BindView(R.id.subreddit_submission_emptyState) EmptyStateView emptyStateView;
  @BindView(R.id.subreddit_submission_errorState) ErrorStateView firstLoadErrorStateView;

  @Inject SubmissionRepository submissionRepository;

  private SubmissionFragment submissionFragment;
  private InfiniteScrollRecyclerAdapter<Submission, ?> submissionAdapterWithProgress;
  private BehaviorRelay<String> subredditChangesStream = BehaviorRelay.create();
  private BehaviorRelay<SortingAndTimePeriod> sortingChangesStream = BehaviorRelay.create();
  private Relay<Object> forceRefreshRequestStream = PublishRelay.create();

  protected static void addStartExtrasToIntent(RedditSubredditLink subredditLink, @Nullable Rect expandFromShape, Intent intent) {
    intent.putExtra(KEY_INITIAL_SUBREDDIT_LINK, subredditLink);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    boolean isPullCollapsible = !isTaskRoot();
    setPullToCollapseEnabled(isPullCollapsible);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_subreddit);
    ButterKnife.bind(this);
    setupContentExpandablePage(contentPage);

    // Add top-margin to make room for the status bar.
    executeOnMeasure(toolbar, () -> setMarginTop(sortingModeContainer, toolbar.getHeight()));
    executeOnMeasure(sortingModeContainer, () -> setPaddingTop(submissionList, sortingModeContainer.getHeight() + toolbar.getHeight()));

    findAndSetupToolbar();
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (isPullCollapsible) {
      expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
      toolbarCloseButton.setVisibility(View.VISIBLE);
      toolbarCloseButton.setOnClickListener(o -> finish());
    }
    contentPage.setNestedExpandablePage(submissionPage);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedState) {
    super.onPostCreate(savedState);

    setupSubmissionList(savedState);
    setupSubmissionFragment();
    setupToolbarSheet();

    // Schedule subreddit syncs + do an immediate sync.
    // TODO: Also subscribe to on-logged-in-event and run these jobs:
    if (isTaskRoot() && Dank.userSession().isUserLoggedIn()) {
      SubredditSubscriptionsSyncJob.syncImmediately(this);
      SubredditSubscriptionsSyncJob.schedule(this);
      CheckUnreadMessagesJobService.syncImmediately(this);
      CheckUnreadMessagesJobService.schedule(this);
    }

    // Restore state of subreddit picker sheet / user profile sheet.
    if (savedState != null) {
      if (savedState.getBoolean(KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE)) {
        showSubredditPickerSheet();
      } else if (savedState.getBoolean(KEY_IS_USER_PROFILE_SHEET_VISIBLE)) {
        showUserProfileSheet();
      }
    }

    // Setup sorting button.
    LayoutTransition layoutTransition = sortingModeContainer.getLayoutTransition();
    layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

    if (savedState != null && savedState.containsKey(KEY_SORTING_AND_TIME_PERIOD)) {
      //noinspection ConstantConditions
      sortingChangesStream.accept(savedState.getParcelable(KEY_SORTING_AND_TIME_PERIOD));
    } else {
      sortingChangesStream.accept(SortingAndTimePeriod.create(Sorting.HOT));
    }
    unsubscribeOnDestroy(
        sortingChangesStream.subscribe(sortingAndTimePeriod -> {
          if (sortingAndTimePeriod.sortOrder().requiresTimePeriod()) {
            sortingModeButton.setText(getString(
                R.string.subreddit_sorting_mode_with_time_period,
                getString(sortingAndTimePeriod.getSortingDisplayTextRes()),
                getString(sortingAndTimePeriod.getTimePeriodDisplayTextRes())
            ));
          } else {
            sortingModeButton.setText(getString(R.string.subreddit_sorting_mode, getString(sortingAndTimePeriod.getSortingDisplayTextRes())));
          }
        })
    );

    emptyStateView.setEmoji(R.string.subreddit_empty_state_title);
    emptyStateView.setMessage(R.string.subreddit_empty_state_message);

    // Toggle the subscribe button's visibility.
    unsubscribeOnDestroy(
        subredditChangesStream
            .switchMap(subredditName -> Dank.subscriptions().isSubscribed(subredditName))
            .compose(applySchedulers())
            .startWith(Boolean.FALSE)
            .onErrorResumeNext(error -> {
              logError("Couldn't get subscribed status for %s", subredditChangesStream.getValue()).accept(error);
              return Observable.just(false);
            })
            .subscribe(isSubscribed -> subscribeButton.setVisibility(isSubscribed ? View.GONE : View.VISIBLE))
    );
  }

  @OnClick(R.id.subreddit_subscribe)
  public void onClickSubscribeToSubreddit() {
    String subredditName = subredditChangesStream.getValue();
    subscribeButton.setVisibility(View.GONE);

    // Intentionally not unsubscribing from this API call on Activity destroy.
    // We'll treat it as a fire-n-forget call and let them run even when this Activity exits.
    Dank.reddit().findSubreddit(subredditName)
        .flatMapCompletable(subreddit -> Dank.subscriptions().subscribe(subreddit))
        .subscribeOn(io())
        .subscribe(doNothingCompletable(), logError("Couldn't subscribe to %s", subredditName));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    submissionList.handleOnSaveInstance(outState);
    if (subredditChangesStream.hasValue()) {
      outState.putString(KEY_ACTIVE_SUBREDDIT, subredditChangesStream.getValue());
    }
    if (sortingChangesStream.hasValue()) {
      outState.putParcelable(KEY_SORTING_AND_TIME_PERIOD, sortingChangesStream.getValue());
    }
    outState.putBoolean(KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE, isSubredditPickerVisible());
    outState.putBoolean(KEY_IS_USER_PROFILE_SHEET_VISIBLE, isUserProfileSheetVisible());
    super.onSaveInstanceState(outState);
  }

  @Override
  public void setTitle(CharSequence subredditName) {
    boolean isFrontpage = subscriptions().isFrontpage(subredditName.toString());
    toolbarTitleView.setText(isFrontpage ? getString(R.string.app_name) : subredditName);
  }

  private void setupSubmissionFragment() {
    submissionFragment = (SubmissionFragment) getSupportFragmentManager().findFragmentById(submissionPage.getId());
    if (submissionFragment == null) {
      submissionFragment = SubmissionFragment.create();
    }
    getSupportFragmentManager()
        .beginTransaction()
        .replace(submissionPage.getId(), submissionFragment)
        .commit();
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
            setTitle(getString(R.string.user_name_u_prefix, Dank.userSession().loggedInUserName()));
          }

          toolbarTitleArrowView.setState(ExpandIconView.LESS, true);
          break;

        case EXPANDED:
          break;

        case COLLAPSING:
          if (isSubredditPickerVisible()) {
            Keyboards.hide(this, toolbarSheet);

          } else if (isUserProfileSheetVisible()) {
            // This will update the title.
            setTitle(subredditChangesStream.getValue());
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

// ======== SUBMISSION LIST ======== //

  private void setupSubmissionList(Bundle savedState) {
    submissionList.setLayoutManager(submissionList.createLayoutManager());
    submissionList.setItemAnimator(new DefaultItemAnimator());
    submissionList.setExpandablePage(submissionPage, toolbarContainer);

    // Swipe gestures.
    OnLoginRequireListener onLoginRequireListener = () -> LoginActivity.startForResult(this, REQUEST_CODE_LOGIN);
    SubmissionSwipeActionsProvider swipeActionsProvider = new SubmissionSwipeActionsProvider(
        Dank.submissions(),
        Dank.voting(),
        Dank.userSession(),
        onLoginRequireListener
    );
    submissionList.addOnItemTouchListener(new RecyclerSwipeListener(submissionList));

    SubmissionsAdapter submissionsAdapter = new SubmissionsAdapter(Dank.voting(), Dank.userPrefs(), swipeActionsProvider);
    submissionsAdapter.setOnItemClickListener((submission, submissionItemView, submissionId) -> {
      DankSubmissionRequest submissionRequest = DankSubmissionRequest.builder(submission.getId())
          .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : DankRedditClient.DEFAULT_COMMENT_SORT)
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

    // Wrapper adapter for infinite scroll progress and errors.
    submissionAdapterWithProgress = InfiniteScrollRecyclerAdapter.wrap(submissionsAdapter);
    submissionList.setAdapter(submissionAdapterWithProgress);

    subredditChangesStream
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(subreddit -> setTitle(subreddit));

    loadSubmissions();

    // Get frontpage (or retained subreddit's) submissions.
    if (savedState != null && savedState.containsKey(KEY_ACTIVE_SUBREDDIT)) {
      String retainedSub = savedState.getString(KEY_ACTIVE_SUBREDDIT);
      //noinspection ConstantConditions
      subredditChangesStream.accept(retainedSub);
    } else if (getIntent().hasExtra(KEY_INITIAL_SUBREDDIT_LINK)) {
      String requestedSub = ((RedditSubredditLink) getIntent().getParcelableExtra(KEY_INITIAL_SUBREDDIT_LINK)).name();
      subredditChangesStream.accept(requestedSub);
    } else {
      subredditChangesStream.accept(Dank.subscriptions().defaultSubreddit());
    }

    if (savedState != null) {
      submissionList.handleOnRestoreInstanceState(savedState);
    }
  }

  private void loadSubmissions() {
    // TODO: Show first load progress
    // TODO: Show load more progress
    // TODO: Handle errors.
    // TODO: Refresh submission on start.
    // TODO: Remove SubmissionManager and CachedSubmissionDeprecated.

    Observable<CachedSubmissionFolder> submissionFolderStream = Observable.combineLatest(
        subredditChangesStream,
        sortingChangesStream,
        CachedSubmissionFolder::create
    );
    Relay<NetworkCallState> loadFromRemoteProgressStream = PublishRelay.create();
    Observable<List<Submission>> databaseStream = submissionFolderStream
        .switchMap(folder -> submissionRepository.submissions(folder).subscribeOn(io()))
        .share();

    // Infinite scroll.
    submissionFolderStream
        .observeOn(mainThread())
        .doOnNext(folder -> Timber.i("-------------------------------"))
        .doOnNext(folder -> Timber.i("%s", folder))
        .switchMap(folder -> InfiniteScroller.streamPagingRequests(submissionList)
            .mergeWith(forceRefreshRequestStream)
            .observeOn(Schedulers.io())
            .doOnNext(o -> Timber.i("load request"))
            .flatMapSingle(o -> submissionRepository.loadMoreSubmissions(folder)
                .retry(3)
                .doOnSubscribe(d -> loadFromRemoteProgressStream.accept(NetworkCallState.IN_FLIGHT))
                .doOnSuccess(s -> loadFromRemoteProgressStream.accept(NetworkCallState.IDLE))
                .doOnError(e -> loadFromRemoteProgressStream.accept(NetworkCallState.FAILED))
                .onErrorResumeNext(Single.never()))
            .doOnError(error -> Timber.e(error, "Load more fail"))
        )
        .subscribe();

    // DB subscription.
    databaseStream
        .observeOn(mainThread())
        .doOnNext(s -> Timber.i("Found %s subms", s.size()))
        .takeUntil(lifecycle().onDestroy())
        .doAfterNext(submissions -> {
          if (submissions.isEmpty()) {
            // I initially tried making the infinite scroll Rx chain check DB items and force-reload itself,
            // but that's resulting in ThreadInterruptedExceptions.
            forceRefreshRequestStream.accept(Notification.INSTANCE);
          }
        })
        .subscribe(submissionAdapterWithProgress);

    // Thoughts: Combining the DB and infinite-scroll streams does not seem possible.

    Observable.combineLatest(
        databaseStream.map(submissions -> !submissions.isEmpty()),
        loadFromRemoteProgressStream.startWith(NetworkCallState.IDLE),
        SubmissionsDatabaseAndNetworkState::create
    )
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(state -> {
          Predicate<SubmissionsDatabaseAndNetworkState> fullscreenProgressVisibility = o -> {
            //noinspection CodeBlock2Expr
            if (state.hasItemsInDatabase())
              return false;
            else {
              if (state.loadFromRemoteState() == NetworkCallState.IN_FLIGHT) {
                return true;
              }
              return false;
            }
          };
          fullscreenProgressView.setVisibility(fullscreenProgressVisibility.test(state) ? View.VISIBLE : View.GONE);

          if (state.hasItemsInDatabase()) {
            switch (state.loadFromRemoteState()) {
              case IN_FLIGHT:
                Timber.i("TODO: show load-more progress");
                break;

              case IDLE:
                Timber.i("TODO: hide load-more progress");
                break;

              case FAILED:
                Timber.i("TODO: load-more error");
                break;
            }
          } else {
            switch (state.loadFromRemoteState()) {
              case IN_FLIGHT:
                break;

              case IDLE:
                Timber.i("TODO: show empty state");
                break;

              case FAILED:
                Toast.makeText(this, "Load failed", Toast.LENGTH_SHORT).show();
                Timber.i("TODO: initial load-more error");
                break;
            }
          }
        });
  }

//  private void startInfiniteScroll(boolean isRetrying) {
//    InfiniteScrollListener scrollListener = InfiniteScrollListener.create(submissionList, InfiniteScrollListener.DEFAULT_LOAD_THRESHOLD);
//    scrollListener.setEmitInitialEvent(isRetrying);
//
//    CachedSubmissionFolder folder = CachedSubmissionFolder.create(subredditChangesStream.getValue(), sortingChangesStream.getValue());
//    unsubscribeOnDestroy(scrollListener.emitWhenLoadNeeded()
//        .doOnNext(o -> scrollListener.setLoadOngoing(true))
//        .flatMapSingle(o -> Dank.submissions().fetchAndSaveMoreSubmissions(folder)
//            .compose(RxUtils.applySchedulersSingle())
//            .compose(handleProgressAndErrorForLoadMore())
//        )
//        .doOnNext(o -> scrollListener.setLoadOngoing(false))
//        .takeUntil(fetchedMessages -> (boolean) fetchedMessages.isEmpty())
//        .subscribe(RxUtils.doNothing(), RxUtils.doNothing()));
//  }

//  private <T> SingleTransformer<T, T> handleProgressAndErrorForLoadMore() {
//    return upstream -> upstream
//        .doOnSubscribe(o -> submissionAdapterWithProgress.setFooter(HeaderFooterInfo.createFooterProgress()))
//        .doOnSuccess(o -> submissionAdapterWithProgress.setFooter(HeaderFooterInfo.createHidden()))
//        .doOnError(error -> submissionAdapterWithProgress.setFooter(HeaderFooterInfo.createError(
//            R.string.subreddit_error_failed_to_load_more_submissions,
//            o -> startInfiniteScroll(true)
//        )));
//  }

// ======== SORTING MODE ======== //

  @OnClick(R.id.subreddit_sorting_mode)
  public void onClickSortingMode(Button sortingModeButton) {
    SubmissionsSortingModePopupMenu sortingPopupMenu = new SubmissionsSortingModePopupMenu(this, sortingModeButton);
    sortingPopupMenu.inflate(R.menu.menu_submission_sorting_mode);
    sortingPopupMenu.highlightActiveSortingAndTImePeriod(sortingChangesStream.getValue());
    sortingPopupMenu.setOnSortingModeSelectListener(sortingAndTimePeriod -> sortingChangesStream.accept(sortingAndTimePeriod));
    sortingPopupMenu.show();
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
      case R.id.action_refresh_submissions:
        subredditChangesStream
            .take(1)
            .observeOn(io())
            .flatMapCompletable(subreddit -> submissionRepository.clearCachedSubmissionLists(subreddit))
            .observeOn(mainThread())
            .subscribe(() -> forceRefreshRequestStream.accept(Notification.INSTANCE));
        return true;

      case R.id.action_user_profile:
        if (Dank.userSession().isUserLoggedIn()) {
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

  @OnClick(R.id.subreddit_toolbar_title)
  void onClickToolbarTitle() {
    if (isUserProfileSheetVisible() || isSubredditPickerVisible()) {
      toolbarSheet.collapse();
    } else {
      showSubredditPickerSheet();
    }
  }

// ======== SUBREDDIT PICKER SHEET ======== //

  void showSubredditPickerSheet() {
    SubredditPickerSheetView pickerSheet = SubredditPickerSheetView.showIn(toolbarSheet, contentPage);
    pickerSheet.post(() -> toolbarSheet.expand());

    pickerSheet.setCallbacks(new SubredditPickerSheetView.Callbacks() {
      @Override
      public void onSelectSubreddit(String subredditName) {
        toolbarSheet.collapse();
        if (!subredditName.equalsIgnoreCase(subredditChangesStream.getValue())) {
          subredditChangesStream.accept(subredditName);
        }
      }

      @Override
      public void onClickAddNewSubreddit() {
        NewSubredditSubscriptionDialog.show(getSupportFragmentManager());
      }

      @Override
      public void onSubredditsChanged() {
        // Refresh the submissions if the frontpage was active.
        if (Dank.subscriptions().isFrontpage(subredditChangesStream.getValue())) {
          subredditChangesStream.accept(subredditChangesStream.getValue());
        }
      }
    });
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
      if (!submissionPage.isExpanded()) {
        showUserProfileSheet();
      }

      // Reload subreddit subscriptions. Not implementing onError() is intentional.
      // This code is not supposed to fail :/
      Dank.subscriptions().removeAll()
          .andThen(submissionRepository.clearCachedSubmissionLists())
          .andThen(submissionRepository.clearCachedSubmissions())
          .andThen(Dank.subscriptions().getAllIncludingHidden().ignoreElements())
          .subscribeOn(io())
          .subscribe();

      // Reload submissions if we're on the frontpage because the frontpage
      // submissions will change if the subscriptions change.
      if (Dank.subscriptions().isFrontpage(subredditChangesStream.getValue())) {
        forceRefreshRequestStream.accept(Notification.INSTANCE);
      }

      // TODO: Expose a callback when the user logs in. Get subreddits, messages and profile.

    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}
