package me.saket.dank.ui.subreddit;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static io.reactivex.schedulers.Schedulers.single;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.transition.TransitionSet;
import android.support.v7.util.DiffUtil;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.zagum.expandicon.ExpandIconView;
import com.jakewharton.rxbinding2.internal.Notification;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.Sorting;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.ReplaySubject;
import me.saket.dank.R;
import me.saket.dank.cache.CachePreFiller;
import me.saket.dank.cache.DatabaseCacheRecyclerJobService;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.ui.subscriptions.SubredditSubscriptionRepository;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.VotingManager;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditLink;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.di.Dank;
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
import me.saket.dank.ui.subreddit.events.SubredditScreenCreateEvent;
import me.saket.dank.ui.subreddit.events.SubredditSubmissionClickEvent;
import me.saket.dank.ui.subreddit.uimodels.SubmissionItemDiffer;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel;
import me.saket.dank.ui.subreddit.uimodels.SubredditUiConstructor;
import me.saket.dank.ui.subscriptions.SubredditPickerSheetView;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.InfiniteScroller;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SubmissionCommentsItemAnimator;
import me.saket.dank.widgets.DankToolbar;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.RxExpandablePage;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

public class SubredditActivity extends DankPullCollapsibleActivity
    implements SubmissionPageLayout.Callbacks, NewSubredditSubscriptionDialog.Callback, InsertGifDialog.OnGifInsertListener
{

  protected static final String KEY_INITIAL_SUBREDDIT_LINK = "initialSubredditLink";
  private static final String KEY_ACTIVE_SUBREDDIT = "activeSubreddit";
  private static final String KEY_IS_SUBREDDIT_PICKER_SHEET_VISIBLE = "isSubredditPickerVisible";
  private static final String KEY_IS_USER_PROFILE_SHEET_VISIBLE = "isUserProfileSheetVisible";
  private static final String KEY_SORTING_AND_TIME_PERIOD = "sortingAndTimePeriod";

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
  @BindView(R.id.subreddit_submission_list) InboxRecyclerView submissionList;
  @BindView(R.id.subreddit_toolbar_expandable_sheet) ToolbarExpandableSheet toolbarSheet;
  @BindView(R.id.subreddit_progress) View fullscreenProgressView;
  @BindView(R.id.subreddit_submission_errorState) ErrorStateView fullscreenErrorStateView;

  // TODO: convert all to lazy injections.
  @Inject SubmissionRepository submissionRepository;
  @Inject ErrorResolver errorResolver;
  @Inject CachePreFiller cachePreFiller;
  @Inject SubredditSubscriptionRepository subscriptionRepository;
  @Inject UserPreferences userPrefs;
  @Inject UserSessionRepository userSessionRepository;
  @Inject SubredditUiConstructor uiConstructor;
  @Inject SubredditSubmissionsAdapter submissionsAdapter;

  @Inject Lazy<UrlRouter> urlRouter;
  @Inject Lazy<UrlParser> urlParser;
  @Inject Lazy<VotingManager> votingManager;
  @Inject Lazy<SubmissionPageAnimationOptimizer> submissionPageAnimationOptimizer;
  @Inject Lazy<SubredditController> subredditController;

  private BehaviorRelay<String> subredditChangesStream = BehaviorRelay.create();
  private BehaviorRelay<SortingAndTimePeriod> sortingChangesStream = BehaviorRelay.create();
  private PublishRelay<Object> forceRefreshSubmissionsRequestStream = PublishRelay.create();
  private BehaviorRelay<Boolean> toolbarRefreshVisibilityStream = BehaviorRelay.createDefault(true);
  private ReplaySubject<UiEvent> uiEvents = ReplaySubject.create();
  private BehaviorRelay<SubredditUserProfileIconType> userProfileIconTypeChanges = BehaviorRelay.create();

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

    setupController();
    setupSubmissionRecyclerView(savedState);
    loadSubmissions();
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

    // Animate changes in sorting button's width on text change.
    LayoutTransition layoutTransition = sortingModeContainer.getLayoutTransition();
    layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

    if (savedState != null && savedState.containsKey(KEY_SORTING_AND_TIME_PERIOD)) {
      //noinspection ConstantConditions
      sortingChangesStream.accept(savedState.getParcelable(KEY_SORTING_AND_TIME_PERIOD));
    } else {
      sortingChangesStream.accept(SortingAndTimePeriod.create(Sorting.HOT));
    }
    sortingChangesStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(sortingAndTimePeriod -> {
          if (sortingAndTimePeriod.sortOrder().requiresTimePeriod()) {
            sortingModeButton.setText(getString(
                R.string.subreddit_sorting_mode_with_time_period,
                getString(sortingAndTimePeriod.getSortingDisplayTextRes()),
                getString(sortingAndTimePeriod.getTimePeriodDisplayTextRes())
            ));
          } else {
            sortingModeButton.setText(getString(R.string.subreddit_sorting_mode, getString(sortingAndTimePeriod.getSortingDisplayTextRes())));
          }
        });

    // Toggle the subscribe button's visibility.
    subredditChangesStream
        .switchMap(subredditName -> subscriptionRepository.isSubscribed(subredditName))
        .compose(applySchedulers())
        .startWith(Boolean.TRUE)
        .onErrorResumeNext(error -> {
          logError("Couldn't get subscribed status for %s", subredditChangesStream.getValue()).accept(error);
          return Observable.just(false);
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe(isSubscribed -> subscribeButton.setVisibility(isSubscribed ? View.GONE : View.VISIBLE));

    userSessionRepository.streamSessions()
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
  public void onDestroy() {
    super.onDestroy();

    if (isFinishing()) {
      // Recycle cached rows in DB.
      DatabaseCacheRecyclerJobService.schedule(this);
    }
  }

  @OnClick(R.id.subreddit_subscribe)
  public void onClickSubscribeToSubreddit() {
    String subredditName = subredditChangesStream.getValue();
    subscribeButton.setVisibility(View.GONE);

    // Intentionally not unsubscribing from this API call on Activity destroy.
    // Treating this as a fire-n-forget call.
    Dank.reddit().findSubreddit(subredditName)
        .flatMapCompletable(subreddit -> subscriptionRepository.subscribe(subreddit))
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

    uiEvents.compose(subredditController.get())
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
            setTitle(getString(R.string.user_name_u_prefix, userSessionRepository.loggedInUserName()));
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
    submissionList.setLayoutManager(submissionList.createLayoutManager());
    submissionList.setItemAnimator(new SubmissionCommentsItemAnimator(0)
        .withInterpolator(Animations.INTERPOLATOR)
        .withRemoveDuration(250)
        .withAddDuration(250));
    submissionList.setExpandablePage(submissionPage, toolbarContainer);
    submissionList.addOnItemTouchListener(new RecyclerSwipeListener(submissionList));

    // Note to self: if adding support for preserving data across orientation changes
    // is being considered, make sure to also preserve scroll position.
    submissionList.setAdapter(submissionsAdapter);

    // Row clicks.
    submissionsAdapter.submissionClicks()
        .withLatestFrom(subredditChangesStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubredditSubmissionClickEvent clickEvent = pair.first();
          String currentSubredditName = pair.second();
          Submission submission = clickEvent.submission();

          DankSubmissionRequest submissionRequest = DankSubmissionRequest.builder(submission.getId())
              .commentSort(submission.getSuggestedSort() != null ? submission.getSuggestedSort() : DankRedditClient.DEFAULT_COMMENT_SORT)
              .build();

          long delay = submissionPageAnimationOptimizer.get().shouldDelayLoad(submission)
              ? submissionPage.getAnimationDurationMillis()
              : 0;

          Single.timer(delay, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
              .takeUntil(lifecycle().onDestroy().ignoreElements())
              .subscribe(o -> {
                submissionPage.populateUi(Optional.of(submission), submissionRequest, Optional.of(currentSubredditName));
                submissionPageAnimationOptimizer.get().trackSubmissionOpened(submission);
              });

          submissionPage.post(() ->
              Observable.timer(100 + delay / 2, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                  .takeUntil(lifecycle().onDestroy())
                  .subscribe(o -> submissionList.expandItem(submissionList.indexOfChild(clickEvent.itemView()), clickEvent.itemId()))
          );
        });

    // Thumbnail clicks.
    submissionsAdapter.thumbnailClicks()
        .doOnNext(event -> {
          if (event.submission().isSelfPost()) {
            throw new AssertionError("Shouldn't happen");
          }
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> {
          Submission submission = event.submission();
          Link contentLink = urlParser.get().parse(submission.getUrl(), submission);

          switch (contentLink.type()) {
            case SINGLE_IMAGE:
            case SINGLE_GIF:
            case SINGLE_VIDEO:
            case MEDIA_ALBUM:
              urlRouter.get()
                  .forLink(((MediaLink) contentLink))
                  .withRedditSuppliedImages(submission.getThumbnails())
                  .open(this);
              break;

            case REDDIT_PAGE:
              switch (((RedditLink) contentLink).redditLinkType()) {
                case COMMENT:
                case SUBMISSION:
                case SUBREDDIT:
                  urlRouter.get()
                      .forLink(contentLink)
                      .expandFrom(new Point(0, event.itemView().getBottom()))
                      .open(this);
                  break;

                case USER:
                  throw new AssertionError("Did not expect Reddit to create a thumbnail for user links");

                default:
                  throw new AssertionError();
              }
              break;

            case EXTERNAL:
              urlRouter.get()
                  .forLink(contentLink)
                  .expandFrom(new Point(0, event.itemView().getBottom()))
                  .open(this);
              break;

            default:
              throw new AssertionError();
          }
        });

    // Option swipe gestures.
    submissionsAdapter.optionSwipeActions()
        .withLatestFrom(subredditChangesStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionOptionSwipeEvent swipeEvent = pair.first();
          Point showLocation = new Point(0, swipeEvent.itemView().getTop() + Views.statusBarHeight(getResources()));

          // Align with submission body.
          int padding = getResources().getDimensionPixelSize(R.dimen.subreddit_submission_start_padding);
          showLocation.offset(padding, padding);

          String subredditName = pair.second(); // This will be different from submission.getSubredditName() in case of frontpage, etc.
          boolean showVisitSubredditOption = !subredditName.equals(swipeEvent.submission().getSubredditName());

          SubmissionOptionsPopup optionsMenu = SubmissionOptionsPopup.builder(this, swipeEvent.submission())
              .showVisitSubreddit(showVisitSubredditOption)
              .build();
          optionsMenu.showAtLocation(swipeEvent.itemView(), Gravity.NO_GRAVITY, showLocation);
        });

    // Vote swipe gestures.
    Observable<ContributionVoteSwipeEvent> sharedVoteSwipeActions = submissionsAdapter.voteSwipeActions().share();

    sharedVoteSwipeActions
        .filter(voteEvent -> !voteEvent.contribution().isArchived())
        .flatMapCompletable(voteEvent -> votingManager.get()
            .voteWithAutoRetry(voteEvent.contribution(), voteEvent.newVoteDirection())
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
      submissionList.handleOnRestoreInstanceState(savedState);
    }
  }

  private void loadSubmissions() {
    Observable<CachedSubmissionFolder> submissionFolderStream = Observable.combineLatest(
        subredditChangesStream,
        sortingChangesStream,
        CachedSubmissionFolder::create
    );

    Relay<SubmissionPaginationResult> paginationResults = BehaviorRelay.createDefault(SubmissionPaginationResult.idle());
    Relay<Optional<List<Submission>>> cachedSubmissionStream = BehaviorRelay.createDefault(Optional.empty());

    // Pagination.
    submissionFolderStream
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .switchMap(folder -> InfiniteScroller.streamPagingRequests(submissionList)
            .mergeWith(submissionsAdapter.paginationFailureRetryClicks())
            .mergeWith(fullscreenErrorStateView.retryClicks())
            .observeOn(io())
            .flatMap(o -> submissionRepository.loadAndSaveMoreSubmissions(folder))
        )
        .subscribe(paginationResults);

    // Folder change.
    submissionFolderStream
        .compose(RxUtils.replayLastItemWhen(forceRefreshSubmissionsRequestStream))
        .switchMap(folder -> submissionRepository
            .clearCachedSubmissionLists(folder.subredditName())
            .andThen(submissionRepository.loadAndSaveMoreSubmissions(folder).doOnNext(paginationResults).ignoreElements())
            .subscribeOn(io())
            .observeOn(mainThread())
            .andThen(submissionRepository.submissions(folder))
            .map(Optional::of)
            .startWith(Optional.empty())
        )
        .takeUntil(lifecycle().onDestroy())
        .subscribe(cachedSubmissionStream);

    Observable<SubredditScreenUiModel> sharedUiModels = uiConstructor
        .stream(this, cachedSubmissionStream.observeOn(io()), paginationResults.observeOn(io()))
        .subscribeOn(io())
        .share();

    Observable<Pair<List<SubredditScreenUiModel.SubmissionRowUiModel>, DiffUtil.DiffResult>> adapterUpdates = sharedUiModels
        .map(SubredditScreenUiModel::rowUiModels)
        .observeOn(io())
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(SubmissionItemDiffer::create))
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
    int displayWidth = getResources().getDisplayMetrics().widthPixels;
    int submissionAlbumLinkThumbnailWidth = SubmissionCommentsHeader.getWidthForAlbumContentLinkThumbnail(this);
    submissionFolderStream
        .observeOn(single())
        .switchMap(folder -> subscriptionRepository.isSubscribed(folder.subredditName()))
        .filter(isSubscribed -> isSubscribed)
        .switchMap(o -> cachedSubmissionStream
            .filter(Optional::isPresent)
            .map(Optional::get)
            .switchMap(cachedSubmissions -> cachePreFiller
                .preFillInParallelThreads(cachedSubmissions, displayWidth, submissionAlbumLinkThumbnailWidth)
                .doOnError(error -> {
                  ResolvedError resolvedError = errorResolver.resolve(error);
                  resolvedError.ifUnknown(() -> Timber.e(error, "Unknown error while pre-filling cache."));
                })
                .onErrorComplete()
                .toObservable()))
        .takeUntil(lifecycle().onDestroy())
        .subscribe();
  }

  @Override
  public SubmissionPageAnimationOptimizer submissionPageAnimationOptimizer() {
    return submissionPageAnimationOptimizer.get();
  }

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
        if (userSessionRepository.isUserLoggedIn()) {
          showUserProfileSheet();
        } else {
          startActivity(LoginActivity.intent(this));
        }
        return true;

      case R.id.action_preferences:
        startActivity(UserPreferencesActivity.intent(this));
        //ComposeReplyActivity.start(this, ComposeStartOptions.builder()
        //    .secondPartyName("Test")
        //    .parentContribution(ContributionFullNameWrapper.create("Poop"))
        //    .build()
        //);
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
      public void onSubredditsChanged() {
        // Refresh the submissions if the frontpage was active.
        if (subscriptionRepository.isFrontpage(subredditChangesStream.getValue())) {
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

// ======== USER PROFILE ======== //

  void setToolbarUserProfileIcon(SubredditUserProfileIconType iconType) {
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
      submissionList.collapse();
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
