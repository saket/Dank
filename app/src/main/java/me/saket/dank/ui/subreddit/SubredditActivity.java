package me.saket.dank.ui.subreddit;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.f2prateek.rx.preferences2.Preference;
import com.github.zagum.expandicon.ExpandIconView;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Submission;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.ReplaySubject;
import me.saket.dank.R;
import me.saket.dank.cache.CachePreFiller;
import me.saket.dank.cache.DatabaseCacheRecyclerJobService;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.compose.InsertGifDialog;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.ArchivedSubmissionDialogActivity;
import me.saket.dank.ui.submission.CachedSubmissionFolder;
import me.saket.dank.ui.submission.SortingAndTimePeriod;
import me.saket.dank.ui.submission.SubmissionPageLayout;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsHeader;
import me.saket.dank.ui.submission.events.ContributionVoteSwipeEvent;
import me.saket.dank.ui.subreddit.events.SubmissionOpenInNewTabSwipeEvent;
import me.saket.dank.ui.subreddit.events.SubmissionOptionSwipeEvent;
import me.saket.dank.ui.subreddit.events.SubredditScreenCreateEvent;
import me.saket.dank.ui.subreddit.uimodels.SubmissionItemDiffer;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel;
import me.saket.dank.ui.subreddit.uimodels.SubredditUiConstructor;
import me.saket.dank.ui.subscriptions.SubredditPickerSheetView;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.InfiniteScroller;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.itemanimators.SubmissionCommentsItemAnimator;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.widgets.DankToolbar;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.RxExpandablePage;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static io.reactivex.schedulers.Schedulers.single;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.touchLiesOn;

public class SubredditActivity extends DankPullCollapsibleActivity
    implements SubredditUi, SubmissionPageLayout.Callbacks, NewSubredditSubscriptionDialog.Callback, InsertGifDialog.OnGifInsertListener
{

  protected static final String KEY_INITIAL_SUBREDDIT_LINK = "initialSubredditLink";
  private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";
  private static final String KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE = "isSubredditPickerVisible";
  private static final String KEY_IS_USER_PROFILE_SHEET_VISIBLE = "isUserProfileSheetVisible";
  private static final String KEY_SORTING_AND_TIME_PERIOD = "sortingAndTimePeriod";
  private static final String KEY_SUBREDDIT_LINK = "subredditLink";

  @BindView(R.id.subreddit_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.subreddit_submission_page) SubmissionPageLayout submissionPage;
  @BindView(R.id.toolbar) DankToolbar toolbar;
  @BindView(R.id.subreddit_toolbar_close) View toolbarCloseButton;
  @BindView(R.id.subreddit_toolbar_title) TextView toolbarTitleView;
  @BindView(R.id.subreddit_toolbar_title_arrow) ExpandIconView toolbarTitleArrowView;
  @BindView(R.id.subreddit_toolbar_title_container) ViewGroup toolbarTitleContainer;
  @BindView(R.id.subreddit_toolbar_container) ViewGroup toolbarContainer;
  @BindView(R.id.subreddit_sorting_mode_container) ViewGroup sortingModeContainer;
  @BindView(R.id.subreddit_sorting_mode) Button sortingModeButton;
  @BindView(R.id.subreddit_subscribe) Button subscribeButton;
  @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionRecyclerView;
  @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
  @BindView(R.id.subreddit_progress) View fullscreenProgressView;
  @BindView(R.id.subreddit_submission_errorState) ErrorStateView fullscreenErrorStateView;

  // TODO: convert all to lazy injections.
  @Inject SubmissionRepository submissionRepository;
  @Inject ErrorResolver errorResolver;
  @Inject CachePreFiller cachePreFiller;
  @Inject SubscriptionRepository subscriptionRepository;
  @Inject UserPreferences userPrefs;
  @Inject SubredditUiConstructor uiConstructor;
  @Inject SubredditSubmissionsAdapter submissionsAdapter;
  @Inject @Named("welcome_text_shown") Preference<Boolean> welcomeTextShownPref;

  @Inject Lazy<Reddit> reddit;
  @Inject Lazy<UrlRouter> urlRouter;
  @Inject Lazy<UrlParser> urlParser;
  @Inject Lazy<VotingManager> votingManager;
  @Inject Lazy<SubredditController> subredditController;
  @Inject Lazy<UserSessionRepository> userSessionRepository;
  @Inject Lazy<OnLoginRequireListener> loginRequireListener;

  private BehaviorRelay<String> subredditChangesStream = BehaviorRelay.create();
  private BehaviorRelay<SortingAndTimePeriod> sortingChangesStream = BehaviorRelay.create();
  private PublishRelay<Object> forceRefreshSubmissionsRequestStream = PublishRelay.create();
  private BehaviorRelay<Boolean> toolbarRefreshVisibilityStream = BehaviorRelay.createDefault(true);
  private ReplaySubject<UiEvent> uiEvents = ReplaySubject.create();
  private BehaviorRelay<SubredditUserProfileIconType> userProfileIconTypeChanges = BehaviorRelay.create();

  public static Intent intent(Context context, RedditSubredditLink subredditLink) {
    return intent(context).putExtra(KEY_SUBREDDIT_LINK, subredditLink);
  }

  public static Intent intent(Context context) {
    return new Intent(context, SubredditActivity.class);
  }

  protected static void addStartExtrasToIntent(RedditSubredditLink subredditLink, @Nullable Rect expandFromShape, Intent intent) {
    intent.putExtra(KEY_INITIAL_SUBREDDIT_LINK, subredditLink);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    boolean isPullCollapsible = !isTaskRoot();
    setPullToCollapseEnabled(isPullCollapsible);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_subreddit);
    ButterKnife.bind(this);
    setupContentExpandablePage(contentPage);

    // Add top-margin to make room for the status bar.
    executeOnMeasure(toolbar, () -> setMarginTop(sortingModeContainer, toolbar.getHeight()));
    executeOnMeasure(sortingModeContainer, () -> setPaddingTop(submissionRecyclerView, sortingModeContainer.getHeight() + toolbar.getHeight()));

    findAndSetupToolbar();
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (isPullCollapsible) {
      expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
      toolbarCloseButton.setVisibility(View.VISIBLE);
      toolbarCloseButton.setOnClickListener(o -> finish());
    }
    contentPage.setNestedExpandablePage(submissionPage);

    // LayoutManager needs to be set before onRestore() of RV gets called to restore scroll position.
    submissionRecyclerView.setLayoutManager(submissionRecyclerView.createLayoutManager());

    if (!welcomeTextShownPref.get()) {
      WelcomeToDankView welcomeTextView = new WelcomeToDankView(this);
      contentPage.addView(welcomeTextView, welcomeTextView.layoutParamsForRelativeLayout());
      welcomeTextView.showAnimation()
          .ambWith(lifecycle().onDestroyCompletable())
          .subscribe(() -> welcomeTextShownPref.set(true));
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedState) {
    super.onPostCreate(savedState);

    setupController();
    setupSubmissionRecyclerView(savedState);
    loadSubmissions(savedState == null);
    setupSubmissionPage();
    setupToolbarSheet();

    // Restore state of subreddit picker sheet / user profile sheet.
    if (savedState != null) {
      if (savedState.getBoolean(KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE)) {
        showSubredditPickerSheet(false);
      } else if (savedState.getBoolean(KEY_IS_USER_PROFILE_SHEET_VISIBLE)) {
        showUserProfileSheet();
      }
    }

    if (getIntent().hasExtra(KEY_SUBREDDIT_LINK)) {
      RedditSubredditLink initialSubreddit = getIntent().getParcelableExtra(KEY_SUBREDDIT_LINK);
      subredditChangesStream.accept(initialSubreddit.name());
    }

    // Animate changes in sorting button's width on text change.
    LayoutTransition layoutTransition = sortingModeContainer.getLayoutTransition();
    layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

    if (savedState != null && savedState.containsKey(KEY_SORTING_AND_TIME_PERIOD)) {
      //noinspection ConstantConditions
      sortingChangesStream.accept(savedState.getParcelable(KEY_SORTING_AND_TIME_PERIOD));
    } else {
      sortingChangesStream.accept(new SortingAndTimePeriod(Reddit.Companion.DEFAULT_SUBREDDIT_SORT()));
    }
    sortingChangesStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(sortingAndTimePeriod -> {
          if (sortingAndTimePeriod.sortOrder().getRequiresTimePeriod()) {
            sortingModeButton.setText(getString(
                R.string.subreddit_sorting_mode_with_time_period,
                getString(sortingAndTimePeriod.sortingDisplayTextRes()),
                getString(sortingAndTimePeriod.timePeriodDisplayTextRes())
            ));
          } else {
            sortingModeButton.setText(getString(R.string.subreddit_sorting_mode, getString(sortingAndTimePeriod.sortingDisplayTextRes())));
          }
        });

    // Subscribe button.
    subredditChangesStream
        .switchMap(subredditName -> subscriptionRepository.isSubscribed(subredditName)
            .subscribeOn(io())
            .observeOn(mainThread())
            .map(isSubscribed -> isSubscribed ? View.GONE : View.VISIBLE))
        .startWith(View.GONE)
        .doOnError(e -> Timber.e(e, "Couldn't get subscribed status for %s", subredditChangesStream.getValue()))
        .onErrorResumeNext(Observable.just(View.GONE))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(subscribeVisibility -> subscribeButton.setVisibility(subscribeVisibility));

    userSessionRepository.get().streamSessions()
        .skip(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(session -> {
          if (session.isPresent()) {
            handleOnUserLogIn();
          } else {
            handleOnUserLogOut();
          }
        });
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);

    if (intent.hasExtra(KEY_SUBREDDIT_LINK)) {
      Single.just(submissionPage.isExpandedOrExpanding())
          .flatMapCompletable(isExpanded -> {
            // The user should see the submission page collapse. Wait till the app is in foreground.
            int appRestoreAnimDuration = isExpanded ? 300 : 0;
            return Completable.timer(appRestoreAnimDuration, TimeUnit.MILLISECONDS, mainThread())
                .andThen(Completable.fromAction(() -> submissionRecyclerView.collapse()))
                .andThen(Completable.timer(isExpanded ? submissionPage.getAnimationDurationMillis() : 0, TimeUnit.MILLISECONDS, mainThread()));
          })
          .ambWith(lifecycle().onDestroyCompletable())
          .subscribe(() -> {
            RedditSubredditLink subredditLink = intent.getParcelableExtra(KEY_SUBREDDIT_LINK);
            subredditChangesStream.accept(subredditLink.name());
          });
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    if (isFinishing()) {
      DatabaseCacheRecyclerJobService.schedule(this);
    }
  }

  @OnClick(R.id.subreddit_subscribe)
  public void onClickSubscribeToSubreddit() {
    if (!userSessionRepository.get().isUserLoggedIn()) {
      loginRequireListener.get().onLoginRequired();
      return;
    }

    String subredditName = subredditChangesStream.getValue();
    subscribeButton.setVisibility(View.GONE);

    // Intentionally not unsubscribing from this API call on Activity destroy.
    // Treating this as a fire-n-forget call.
    reddit.get().subreddits().findOld(subredditName)
        .flatMapCompletable(subreddit -> subscriptionRepository.subscribe(subreddit))
        .subscribeOn(io())
        .subscribe(doNothingCompletable(), logError("Couldn't subscribe to %s", subredditName));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    submissionRecyclerView.saveExpandableState(outState);
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
    boolean isFrontpage = subscriptionRepository.isFrontpage(subredditName.toString());
    toolbarTitleView.setText(isFrontpage ? getString(R.string.app_name) : subredditName);
  }

  @Override
  public void onGifInsert(String title, GiphyGif gif, @Nullable Parcelable payload) {
    assert payload != null;
    submissionPage.onGifInsert(title, gif, payload);
  }

  private void setupController() {
    uiEvents.onNext(SubredditScreenCreateEvent.create());

    subredditChangesStream
        .map(name -> SubredditChangeEvent.create(name))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiEvents);

    uiEvents
        .compose(subredditController.get())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiChange -> uiChange.render(this));
  }

  private void setupSubmissionPage() {
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
      if (touchLiesOn(submissionRecyclerView, downX, downY)) {
        return submissionRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1);
      }
      return false;
    });
  }

  private void setupToolbarSheet() {
    toolbarSheet.hideOnOutsideClick(submissionRecyclerView);
    toolbarSheet.setStateChangeListener(state -> {
      switch (state) {
        case EXPANDING:
          if (isSubredditPickerVisible()) {
            // When subreddit picker is showing, we'll show a "configure subreddits" button in the toolbar.
            invalidateOptionsMenu();

          } else if (isUserProfileSheetVisible()) {
            setTitle(getString(R.string.user_name_u_prefix, userSessionRepository.get().loggedInUserName()));
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

  private void setupSubmissionRecyclerView(@Nullable Bundle savedState) {
    submissionRecyclerView.setItemAnimator(new SubmissionCommentsItemAnimator(0)
        .withInterpolator(Animations.INTERPOLATOR)
        .withRemoveDuration(250)
        .withAddDuration(250));
    submissionRecyclerView.setExpandablePage(submissionPage, toolbarContainer);
    submissionRecyclerView.addOnItemTouchListener(new RecyclerSwipeListener(submissionRecyclerView));

    // RV restores scroll position if the adapter data-set is the same.
    submissionsAdapter.dataChanges()
        .filter(uiModels -> !uiModels.isEmpty())
        .take(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> submissionRecyclerView.setAdapter(submissionsAdapter));

    // Row clicks.
    Observable.merge(submissionsAdapter.submissionClicks(), submissionsAdapter.gestureWalkthroughProceedClicks())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiEvents);

    // Thumbnail clicks.
    submissionsAdapter.thumbnailClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> event.openContent(urlParser.get(), urlRouter.get()));

    // Option swipe gestures.
    submissionsAdapter.swipeEvents()
        .ofType(SubmissionOptionSwipeEvent.class)
        .withLatestFrom(subredditChangesStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionOptionSwipeEvent swipeEvent = pair.first();
          String subredditName = pair.second();
          swipeEvent.showPopupForSubredditScreen(subredditName);
        });

    // Open in new tab gestures.
    submissionsAdapter.swipeEvents()
        .ofType(SubmissionOpenInNewTabSwipeEvent.class)
        .delay(SubmissionOpenInNewTabSwipeEvent.TAB_OPEN_DELAY_MILLIS, TimeUnit.MILLISECONDS)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> event.openInNewTab(urlRouter.get(), urlParser.get()));

    // Vote swipe gestures.
    Observable<ContributionVoteSwipeEvent> sharedVoteSwipeActions = submissionsAdapter.swipeEvents()
        .ofType(ContributionVoteSwipeEvent.class)
        .share();

    sharedVoteSwipeActions
        .filter(voteEvent -> !voteEvent.contribution().isArchived())
        .flatMapCompletable(voteEvent -> voteEvent.toVote()
            .saveAndSend(votingManager.get())
            .subscribeOn(io()))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    sharedVoteSwipeActions
        .filter(voteEvent -> voteEvent.contribution().isArchived())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(voteEvent -> startActivity(ArchivedSubmissionDialogActivity.intent(this)));

    subredditChangesStream
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(subreddit -> setTitle(subreddit));

    // Get frontpage (or retained subreddit's) submissions.
    if (savedState != null && savedState.containsKey(KEY_ACTIVE_SUBREDDIT)) {
      String retainedSub = savedState.getString(KEY_ACTIVE_SUBREDDIT);
      //noinspection ConstantConditions
      subredditChangesStream.accept(retainedSub);
    } else if (getIntent().hasExtra(KEY_INITIAL_SUBREDDIT_LINK)) {
      String requestedSub = ((RedditSubredditLink) getIntent().getParcelableExtra(KEY_INITIAL_SUBREDDIT_LINK)).name();
      subredditChangesStream.accept(requestedSub);
    } else {
      subredditChangesStream.accept(subscriptionRepository.defaultSubreddit());
    }

    if (savedState != null) {
      submissionRecyclerView.restoreExpandableState(savedState);
    }
  }

  private void loadSubmissions(boolean isActivityFirstCreate) {
    Observable<CachedSubmissionFolder> submissionFolderStream = Observable.combineLatest(
        subredditChangesStream,
        sortingChangesStream,
        CachedSubmissionFolder::new
    );

    Relay<SubmissionPaginationResult> paginationResults = BehaviorRelay.createDefault(SubmissionPaginationResult.idle());
    Relay<Optional<List<Submission>>> cachedSubmissionStream = BehaviorRelay.createDefault(Optional.empty());

    // Pagination.
    submissionFolderStream
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .switchMap(folder -> InfiniteScroller.streamPagingRequests(submissionRecyclerView)
            .mergeWith(submissionsAdapter.paginationFailureRetryClicks())
            .mergeWith(fullscreenErrorStateView.retryClicks())
            .observeOn(io())
            .flatMap(o -> submissionRepository.loadAndSaveMoreSubmissions(folder))
        )
        .subscribe(paginationResults);

    // The DB stream and the network stream were previously independent, but were later merged together.
    // This was done because the submissions used to show up for a second before getting cleared off.

    AtomicBoolean shouldRefreshSubmissions = new AtomicBoolean(isActivityFirstCreate);

    // Folder change.
    submissionFolderStream
        .compose(RxUtils.replayLastItemWhen(forceRefreshSubmissionsRequestStream))
        .switchMap(folder -> {
          // The DB stream and the network stream were previously independent, but were later merged together.
          // This was done because the submissions used to show up for a second before getting cleared off.

          boolean shouldRefresh = shouldRefreshSubmissions.getAndSet(true);

          Completable refreshCacheIfNeeded;
          if (shouldRefresh) {
            refreshCacheIfNeeded = submissionRepository.clearCachedSubmissionLists(folder.subredditName())
                .andThen(submissionRepository.loadAndSaveMoreSubmissions(folder)
                    .doOnNext(paginationResults)
                    .ignoreElements());
          } else {
            refreshCacheIfNeeded = submissionRepository.submissions(folder)
                .take(1)
                .flatMapCompletable(submissions -> {
                  if (submissions.isEmpty()) {
                    return submissionRepository.loadAndSaveMoreSubmissions(folder)
                        .doOnNext(paginationResults)
                        .ignoreElements();
                  } else {
                    return Completable.complete();
                  }
                });
          }

          return refreshCacheIfNeeded
              .subscribeOn(io())
              .observeOn(mainThread())
              .andThen(submissionRepository.submissions(folder))
              .map(Optional::of)
              .startWith(Optional.empty());
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe(cachedSubmissionStream);

    Observable<SubredditScreenUiModel> sharedUiModels = uiConstructor
        .stream(this, cachedSubmissionStream.observeOn(io()), paginationResults.observeOn(io()), submissionFolderStream)
        .subscribeOn(io())
        .share();

    Observable<Pair<List<SubredditScreenUiModel.SubmissionRowUiModel>, DiffUtil.DiffResult>> adapterUpdates = sharedUiModels
        .map(SubredditScreenUiModel::rowUiModels)
        .observeOn(io())
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculate(SubmissionItemDiffer.INSTANCE))
        .toObservable()
        .observeOn(mainThread());

    // Suspend updates to the list while any submission is open. We don't want to hide updates.
    Observable.combineLatest(adapterUpdates, RxExpandablePage.stateChanges(submissionPage), Pair::create)
        .filter(pair -> pair.second().isCollapsed())
        .map(pair -> pair.first())
        .distinctUntilChanged((pair1, pair2) -> pair1.first().equals(pair2.first()))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submissionsAdapter);

    // Fullscreen progress.
    sharedUiModels.map(SubredditScreenUiModel::fullscreenProgressVisible)
        .distinctUntilChanged()
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(visible -> fullscreenProgressView.setVisibility(visible ? View.VISIBLE : View.GONE));

    // Fullscreen errors and empty states.
    sharedUiModels
        .distinctUntilChanged((f, s) -> f.fullscreenError().equals(s.fullscreenError()) && f.emptyState().equals(s.emptyState()))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiState -> {
          TransitionSet transitions = Animations.transitions().addTarget(fullscreenErrorStateView);
          TransitionManager.beginDelayedTransition(((ViewGroup) fullscreenErrorStateView.getParent()), transitions);

          if (uiState.fullscreenError().isPresent()) {
            fullscreenErrorStateView.setVisibility(View.VISIBLE);
            fullscreenErrorStateView.applyFrom(uiState.fullscreenError().get());

          } else if (uiState.emptyState().isPresent()) {
            fullscreenErrorStateView.setVisibility(View.VISIBLE);
            fullscreenErrorStateView.applyFrom(uiState.emptyState().get());

          } else {
            fullscreenErrorStateView.setVisibility(View.GONE);
          }
        });

    // Toolbar refresh.
    sharedUiModels.map(SubredditScreenUiModel::toolbarRefreshVisible)
        .distinctUntilChanged()
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(toolbarRefreshVisibilityStream);

    // Cache pre-fill.
    int submissionAlbumLinkThumbnailWidth = SubmissionCommentsHeader.getWidthForAlbumContentLinkThumbnail(this);

    submissionFolderStream
        .switchMap(folder -> subscriptionRepository.isSubscribed(folder.subredditName()).take(1))
        .switchMap(isSubscribed -> {
          if (!isSubscribed) {
            return Observable.empty();
          }

          return cachedSubmissionStream
              .subscribeOn(single())
              .filter(Optional::isPresent)
              .map(Optional::get)
              .distinctUntilChanged((first, second) -> first.size() == second.size())
              .switchMap(cachedSubmissions -> cachePreFiller
                  .preFillInParallelThreads(cachedSubmissions, submissionAlbumLinkThumbnailWidth)
                  .doOnError(error -> {
                    ResolvedError resolvedError = errorResolver.resolve(error);
                    resolvedError.ifUnknown(() -> Timber.e(error, "Unknown error while pre-filling cache."));
                  })
                  .onErrorComplete()
                  .toObservable());
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe();
  }

  @Override
  public void populateSubmission(Submission submission, DankSubmissionRequest submissionRequest, String currentSubredditName) {
    submissionPage.populateUi(Optional.of(submission), submissionRequest, Optional.of(currentSubredditName));
  }

  @Override
  public void expandSubmissionRow(View submissionRowView, long submissionRowId) {
    submissionRecyclerView.expandItem(submissionRecyclerView.indexOfChild(submissionRowView), submissionRowId);
  }

// ======== SORTING MODE ======== //

  @OnClick(R.id.subreddit_sorting_mode)
  public void onClickSortingMode(Button sortingModeButton) {
    SubmissionsSortingModePopupMenu sortingPopupMenu = new SubmissionsSortingModePopupMenu(this, sortingModeButton);
    sortingPopupMenu.inflate(R.menu.menu_submission_sorting_mode);
    sortingPopupMenu.highlightActiveSortingAndTimePeriod(sortingChangesStream.getValue());
    sortingPopupMenu.setOnSortingModeSelectListener(sortingChangesStream::accept);
    sortingPopupMenu.show();
  }

// ======== NAVIGATION ======== //

  @Override
  public void onClickSubmissionToolbarUp() {
    submissionRecyclerView.collapse();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_subreddit, menu);

    MenuItem refreshItem = menu.findItem(R.id.action_refresh_submissions);
    toolbarRefreshVisibilityStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(refreshItem::setVisible);

    MenuItem userProfileItem = menu.findItem(R.id.action_user_profile);
    userProfileIconTypeChanges
        .takeUntil(lifecycle().onDestroy())
        .subscribe(iconType -> {
          userProfileItem.setIcon(iconType.iconRes());
        });

    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_refresh_submissions:
        forceRefreshSubmissionsRequestStream.accept(Notification.INSTANCE);
        return true;

      case R.id.action_user_profile:
        if (userSessionRepository.get().isUserLoggedIn()) {
          showUserProfileSheet();
        } else {
          startActivity(LoginActivity.intent(this));
        }
        return true;

      case R.id.action_preferences:
        startActivity(UserPreferencesActivity.intent(this));
        return true;

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @OnClick(R.id.subreddit_toolbar_title)
  void onToolbarTitleClick() {
    if (isUserProfileSheetVisible() || isSubredditPickerVisible()) {
      toolbarSheet.collapse();
    } else {
      showSubredditPickerSheet(false);
    }
  }

  @OnLongClick(R.id.subreddit_toolbar_title)
  boolean onToolbarTitleLongClick() {
    if (!isSubredditPickerVisible() && !isUserProfileSheetVisible()) {
      showSubredditPickerSheet(true);
    }
    return true;
  }

// ======== SUBREDDIT PICKER SHEET ======== //

  void showSubredditPickerSheet(boolean showKeyboardOnStart) {
    SubredditPickerSheetView pickerSheet = new SubredditPickerSheetView(this);
    pickerSheet.showIn(toolbarSheet, contentPage);
    pickerSheet.post(() -> toolbarSheet.expand());

    if (showKeyboardOnStart) {
      Keyboards.show(pickerSheet.searchView);
    }

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
      public void onSubredditsChange() {
        // Refresh the submissions if the frontpage was active.
        if (subscriptionRepository.isFrontpage(subredditChangesStream.getValue())) {
          subredditChangesStream.accept(subredditChangesStream.getValue());
        }
      }
    });
  }

  @Override
  public void onEnterNewSubredditForSubscription(Subscribeable newSubscribeable) {
    if (isSubredditPickerVisible()) {
      findSubredditPickerSheet().subscribeTo(newSubscribeable);
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

// ======== USER PROFILE ======== //

  @Override
  public void setToolbarUserProfileIcon(SubredditUserProfileIconType iconType) {
    userProfileIconTypeChanges.accept(iconType);
  }

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
      submissionRecyclerView.collapse();
    } else if (!toolbarSheet.isCollapsed()) {
      toolbarSheet.collapse();
    } else {
      super.onBackPressed();
    }
  }

  private void handleOnUserLogIn() {
    // Reload submissions if we're on the frontpage because the frontpage
    // submissions will change if the subscriptions change.
    subredditChangesStream
        .take(1)
        .filter(subscriptionRepository::isFrontpage)
        .subscribe(o -> forceRefreshSubmissionsRequestStream.accept(Notification.INSTANCE));

    if (!submissionPage.isExpanded()) {
      showUserProfileSheet();
    }
  }

  private void handleOnUserLogOut() {
    subredditChangesStream.accept(subscriptionRepository.defaultSubreddit());
  }
}
