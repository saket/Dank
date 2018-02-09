package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.danikula.videocache.HttpProxyCacheServer;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.VotingManager;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.data.links.ExternalLink;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.data.links.UnresolvedMediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.giphy.GiphyPickerActivity;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.preferences.UserPreferenceGroup;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.adapter.CommentsItemDiffer;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.submission.adapter.SubmissionComment;
import me.saket.dank.ui.submission.adapter.SubmissionCommentInlineReply;
import me.saket.dank.ui.submission.adapter.SubmissionCommentRowType;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsAdapter;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsHeader;
import me.saket.dank.ui.submission.adapter.SubmissionScreenUiModel;
import me.saket.dank.ui.submission.adapter.SubmissionUiConstructor;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.ui.subreddit.SubmissionOptionSwipeEvent;
import me.saket.dank.ui.subreddit.SubmissionOptionsPopupMenu;
import me.saket.dank.ui.subreddit.SubmissionPageAnimationOptimizer;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Function0;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.LinearSmoothScrollerWithVerticalSnapPref;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtils;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SubmissionCommentsItemAnimator;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews.Streams;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.KeyboardVisibilityDetector.KeyboardVisibilityChangeEvent;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionPageLayout extends ExpandablePageLayout
    implements ExpandablePageLayout.StateChangeCallbacks, ExpandablePageLayout.OnPullToCollapseIntercepter
{

  private static final String KEY_SUPER_CLASS_STATE = "superClassState";
  private static final String KEY_WAS_PAGE_EXPANDED_OR_EXPANDING = "pageState";
  private static final String KEY_SUBMISSION_JSON = "submissionJson";
  private static final String KEY_SUBMISSION_REQUEST = "submissionRequest";
  private static final String KEY_INLINE_REPLY_ROW_ID = "inlineReplyRowId";
  private static final String KEY_CALLING_SUBREDDIT = "immediateParentSubreddit";
  private static final long COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION = ExpandablePageLayout.DEFAULT_ANIM_DURATION;
  private static final long ACTIVITY_CONTENT_RESIZE_ANIM_DURATION = 300;
  private static final int REQUEST_CODE_PICK_GIF = 98;
  private static final int REQUEST_CODE_FULLSCREEN_REPLY = 99;

  @BindView(R.id.submission_toolbar) View toolbar;
  @BindView(R.id.submission_toolbar_close) ImageButton toolbarCloseButton;
  @BindView(R.id.submission_toolbar_background) AnimatedToolbarBackground toolbarBackground;
  @BindView(R.id.submission_content_progress_bar) SubmissionAnimatedProgressBar contentLoadProgressView;
  @BindView(R.id.submission_image) ZoomableImageView contentImageView;
  @BindView(R.id.submission_video_container) ViewGroup contentVideoViewContainer;
  @BindView(R.id.submission_video) VideoView contentVideoView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
  @BindView(R.id.submission_comment_list) RecyclerView commentRecyclerView;
  @BindView(R.id.submission_reply) FloatingActionButton replyFAB;

  @BindDrawable(R.drawable.ic_toolbar_close_24dp) Drawable closeIconDrawable;
  @BindDimen(R.dimen.submission_commentssheet_minimum_visible_height) int commentsSheetMinimumVisibleHeight;

  // TODO: Convert all to lazy injections.
  @Inject SubmissionRepository submissionRepository;
  @Inject MediaHostRepository mediaHostRepository;
  @Inject UrlRouter urlRouter;
  @Inject Moshi moshi;
  @Inject LinkMetadataRepository linkMetadataRepository;
  @Inject ReplyRepository replyRepository;
  @Inject VotingManager votingManager;
  @Inject UserSessionRepository userSessionRepository;
  @Inject ErrorResolver errorResolver;
  @Inject UserPreferences userPreferences;
  @Inject NetworkStateListener networkStateListener;
  @Inject HttpProxyCacheServer httpProxyCacheServer;
  @Inject SubmissionUiConstructor submissionUiConstructor;
  @Inject SubmissionCommentsAdapter submissionCommentsAdapter;
  @Inject CommentTreeConstructor commentTreeConstructor;

  @Inject @Named("show_nsfw_content") Lazy<Preference<Boolean>> showNsfwContentPreference;

  private BehaviorRelay<DankSubmissionRequest> submissionRequestStream = BehaviorRelay.create();
  private BehaviorRelay<Optional<Submission>> submissionStream = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Link> submissionContentStream = BehaviorRelay.create();
  private BehaviorRelay<KeyboardVisibilityChangeEvent> keyboardVisibilityChangeStream = BehaviorRelay.create();
  private PublishRelay<PostedOrInFlightContribution> inlineReplyStream = PublishRelay.create();
  private BehaviorRelay<Optional<Link>> contentLinkStream = BehaviorRelay.createDefault(Optional.empty());
  private PublishRelay<Boolean> commentsLoadProgressVisibleStream = PublishRelay.create();
  private BehaviorRelay<Optional<SubmissionContentLoadError>> mediaContentLoadErrors = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Optional<ResolvedError>> commentsLoadErrors = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Optional<String>> callingSubreddits = BehaviorRelay.createDefault(Optional.empty());

  private ExpandablePageLayout submissionPageLayout;
  private SubmissionVideoHolder contentVideoViewHolder;
  private SubmissionImageHolder contentImageViewHolder;
  private int deviceDisplayWidth, deviceDisplayHeight;
  private boolean isCommentSheetBeneathImage;
  private SubmissionPageLifecycleStreams lifecycleStreams;

  public interface Callbacks {
    SubmissionPageAnimationOptimizer submissionPageAnimationOptimizer();

    void onClickSubmissionToolbarUp();
  }

  public SubmissionPageLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    Dank.dependencyInjector().inject(this);
    LayoutInflater.from(context).inflate(R.layout.fragment_submission, this, true);
    ButterKnife.bind(this, this);

    LifecycleOwnerActivity parentLifecycleOwner = (LifecycleOwnerActivity) getContext();
    Streams viewLifecycleStreams = LifecycleOwnerViews.create(this, parentLifecycleOwner);
    lifecycleStreams = SubmissionPageLifecycleStreams.wrap(this, viewLifecycleStreams);

    // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
    deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    deviceDisplayHeight = getResources().getDisplayMetrics().heightPixels;

    lifecycle().viewAttaches()
        .take(1)
        .takeUntil(lifecycle().viewDetaches())
        .subscribe(o -> onViewFirstAttach());
  }

  public void onViewFirstAttach() {
    executeOnMeasure(toolbar, () -> setHeight(toolbarBackground, toolbar.getHeight()));
    //noinspection ConstantConditions
    toolbarCloseButton.setOnClickListener(v -> ((Callbacks) getContext()).onClickSubmissionToolbarUp());

    submissionPageLayout = this;
    submissionPageLayout.addStateChangeCallbacks(this);
    submissionPageLayout.setPullToCollapseIntercepter(this);

    Keyboards.streamKeyboardVisibilityChanges(((Activity) getContext()), Views.statusBarHeight(getResources()))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(keyboardVisibilityChangeStream);

    setupCommentRecyclerView();
    setupContentImageView(this);
    setupContentVideoView();
    setupCommentsSheet();
    setupStatusBarTint();
    setupReplyFAB();
    setupSoftInputModeChangesAnimation();
    setupSubmissionLoadErrors();
    setupSubmissionContentLoadErrors();

    // Extra bottom padding in list to make space for FAB.
    Views.executeOnMeasure(replyFAB, () -> {
      int spaceForFab = replyFAB.getHeight() + ((ViewGroup.MarginLayoutParams) replyFAB.getLayoutParams()).bottomMargin * 2;
      Views.setPaddingBottom(commentRecyclerView, spaceForFab);
    });
  }

  @Nullable
  @Override
  protected Parcelable onSaveInstanceState() {
    Bundle outState = new Bundle();
    if (submissionRequestStream.getValue() != null) {
      outState.putParcelable(KEY_SUBMISSION_REQUEST, submissionRequestStream.getValue());

      Optional<Submission> optionalSubmission = submissionStream.getValue();
      Optional<CommentNode> optionalComments = optionalSubmission.map(Submission::getComments);
      if (optionalSubmission.isPresent() && !optionalComments.isPresent()) {
        // Comments haven't fetched yet == no submission cached in DB. For us to be able to immediately
        // show UI on orientation change, we unfortunately will have to manually retain this submission.
        outState.putString(KEY_SUBMISSION_JSON, moshi.adapter(Submission.class).toJson(optionalSubmission.get()));
      }
    }

    callingSubreddits.getValue().ifPresent(subredditName -> {
      outState.putString(KEY_CALLING_SUBREDDIT, subredditName);
    });
    outState.putBoolean(KEY_WAS_PAGE_EXPANDED_OR_EXPANDING, isExpandedOrExpanding());
    outState.putParcelable(KEY_SUPER_CLASS_STATE, super.onSaveInstanceState());
    return outState;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    Bundle savedState = (Bundle) state;
    Parcelable superState = savedState.getParcelable(KEY_SUPER_CLASS_STATE);
    super.onRestoreInstanceState(superState);

    boolean willPageExpandAgain = savedState.getBoolean(KEY_WAS_PAGE_EXPANDED_OR_EXPANDING, false);
    Optional<String> callingSubreddit = Optional.ofNullable(savedState.getString(KEY_CALLING_SUBREDDIT));

    if (willPageExpandAgain && savedState.containsKey(KEY_SUBMISSION_REQUEST)) {
      DankSubmissionRequest retainedRequest = savedState.getParcelable(KEY_SUBMISSION_REQUEST);
      Single
          .fromCallable(() -> {
            boolean manualRestorationNeeded = savedState.containsKey(KEY_SUBMISSION_JSON);
            //noinspection ConstantConditions
            return manualRestorationNeeded
                ? Optional.of(moshi.adapter(Submission.class).fromJson(savedState.getString(KEY_SUBMISSION_JSON)))
                : Optional.<Submission>empty();
          })
          .subscribeOn(io())
          .observeOn(mainThread())
          .takeUntil(lifecycle().onDestroyFlowable())
          .subscribe(retainedSubmission -> {
            //noinspection ConstantConditions
            populateUi(retainedSubmission, retainedRequest, callingSubreddit);
          });
    }
  }

  /**
   * Data from persistence flows to this screen in a unidirectional manner. Any modifications are
   * made on {@link CommentTreeConstructor} or {@link SubmissionUiConstructor} and
   * {@link SubmissionCommentsAdapter} subscribes to its updates.
   */
  private void setupCommentRecyclerView() {
    int itemElevation = getResources().getDimensionPixelSize(R.dimen.submission_comment_elevation);
    SimpleItemAnimator itemAnimator = new SubmissionCommentsItemAnimator(itemElevation)
        .withInterpolator(Animations.INTERPOLATOR)
        .withRemoveDuration(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION)
        .withAddDuration(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION);
    commentRecyclerView.setItemAnimator(itemAnimator);

    // RecyclerView automatically handles saving and restoring scroll position if the
    // adapter contents are the same once the adapter is set. So we set the adapter
    // only once its dataset is available.
    submissionCommentsAdapter.dataChanges()
        .take(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> commentRecyclerView.setAdapter(submissionCommentsAdapter));

    // Load comments when submission changes.
    submissionRequestStream
        .observeOn(mainThread())
        .doOnNext(o -> commentsLoadProgressVisibleStream.accept(true))
        //.doOnNext(o -> Timber.d("------------------"))
        .switchMap(submissionRequest -> submissionRepository.submissionWithComments(submissionRequest)
            //.compose(RxUtils.doOnceOnNext(o -> Timber.i("Submission received")))
            .flatMap(pair -> {
              // It's possible for the remote to suggest a different sort than what was asked by SubredditActivity.
              // In that case, trigger another request with the correct sort. Doing this because we need to store
              // and retain this request in case the Activity gets recreated.
              DankSubmissionRequest updatedRequest = pair.first();
              if (updatedRequest != submissionRequest) {
                //Timber.i("Triggering another request");
                //noinspection ConstantConditions
                submissionRequestStream.accept(updatedRequest);
                return Observable.never();
              } else {
                //noinspection ConstantConditions
                return Observable.just(pair.second());
              }
            })
            .subscribeOn(io())
            .observeOn(mainThread())
            .takeUntil(lifecycle().onPageAboutToCollapse())
            .doOnNext(o -> commentsLoadProgressVisibleStream.accept(false))
            .onErrorResumeNext(error -> {
              ResolvedError resolvedError = errorResolver.resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't fetch comments"));
              commentsLoadErrors.accept(Optional.of(resolvedError));
              return Observable.never();
            })
        )
        .as(Optional.of())
        //.compose(RxUtils.doOnceOnNext(o -> Timber.i("Submission passed to stream")))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submissionStream);

    // Adapter data-set.
    submissionUiConstructor
        .stream(
            getContext(),
            commentTreeConstructor,
            submissionStream.observeOn(io()),
            submissionRequestStream.observeOn(io()),
            contentLinkStream.observeOn(io()),
            mediaContentLoadErrors.observeOn(io()),
            commentsLoadErrors.observeOn(io())
        )
        .subscribeOn(io())
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtils.calculateDiff(CommentsItemDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyFlowable())
        .subscribe(submissionCommentsAdapter);

    // Scroll to focused comment on start.
    submissionRequestStream
        .switchMap(request -> {
          if (request.focusCommentId() == null) {
            return Observable.empty();
          } else {
            return submissionCommentsAdapter.dataChanges()
                .observeOn(io())
                .flatMap(rows -> {
                  for (int i = 0; i < rows.size(); i++) {
                    SubmissionScreenUiModel row = rows.get(i);
                    if (row instanceof SubmissionComment.UiModel && ((SubmissionComment.UiModel) row).isFocused()) {
                      return Observable.just(i);
                    }
                  }
                  return Observable.never();
                })
                .take(1)
                .takeUntil(lifecycle().onPageCollapse());
          }
        })
        .delay(500, TimeUnit.MILLISECONDS, mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(
            focusedCommentPosition -> commentRecyclerView.post(() -> commentRecyclerView.smoothScrollToPosition(focusedCommentPosition)),
            error -> Timber.w(error.getMessage())
        );

    // When request changes, wait until adapter's data-set gets updated AND with comment rows.
    lifecycle().onPageCollapse()
        .startWith(LifecycleStreams.NOTHING)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          contentImageView.setVisibility(View.GONE);
          contentVideoViewContainer.setVisibility(View.GONE);
          toolbarBackground.setSyncScrollEnabled(false);
        });

    // Swipe gestures.
    submissionCommentsAdapter.streamCommentReplySwipeActions()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(parentCommentInfo -> {
          if (commentTreeConstructor.isCollapsed(parentCommentInfo) || !commentTreeConstructor.isReplyActiveFor(parentCommentInfo)) {
            commentTreeConstructor.showReplyAndExpandComments(parentCommentInfo);
            inlineReplyStream.accept(parentCommentInfo);
          } else {
            Keyboards.hide(getContext(), commentRecyclerView);
            commentTreeConstructor.hideReply(parentCommentInfo);
          }
        });
    submissionCommentsAdapter.streamSubmissionOptionSwipeActions()
        .withLatestFrom(callingSubreddits, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          Point sheetLocation = Views.locationOnScreen(commentListParentSheet);
          Point menuLocation = new Point(0, sheetLocation.y);

          // Align the menu with submission title.
          int headerPadding = getResources().getDimensionPixelSize(R.dimen.subreddit_submission_start_padding);
          menuLocation.offset(headerPadding, headerPadding);

          SubmissionOptionSwipeEvent swipeEvent = pair.first();
          Optional<String> optionalCallingSubreddit = pair.second();

          boolean showVisitSubredditOption = optionalCallingSubreddit
              .map(name -> !swipeEvent.submission().getSubredditName().equals(name))
              .orElse(true);

          SubmissionOptionsPopupMenu optionsMenu = new SubmissionOptionsPopupMenu(getContext(), swipeEvent.submission(), showVisitSubredditOption);
          optionsMenu.showAtLocation(swipeEvent.itemView(), Gravity.NO_GRAVITY, menuLocation);
        });

    commentRecyclerView.addOnItemTouchListener(new RecyclerSwipeListener(commentRecyclerView));
    commentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()) {
      @Override
      public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        // Bug workaround: when smooth-scrolling to a position, if the target View is already visible,
        // RecyclerView ends up snapping to the bottom of the child. This is not what is needed in any
        // case so I'm defaulting to SNAP_TO_START.
        LinearSmoothScroller linearSmoothScroller = new LinearSmoothScrollerWithVerticalSnapPref(getContext(), LinearSmoothScroller.SNAP_TO_START);
        linearSmoothScroller.setTargetPosition(position);
        startSmoothScroll(linearSmoothScroller);
      }
    });

    // Inline reply additions.
    // Wait till the reply's View is added to the list and show keyboard.
    inlineReplyStream
        .switchMapSingle(parentContribution -> scrollToNewlyAddedReplyIfHidden(parentContribution).toSingleDefault(parentContribution))
        .switchMap(parentContribution -> showKeyboardWhenReplyIsVisible(parentContribution))
        .takeUntil(lifecycle().onDestroy())
        .subscribe();

    // Manually dispose reply draft subscribers, because Adapter#onViewHolderRecycled()
    // doesn't get called if the Activity is getting recreated.
    lifecycle().onDestroy()
        .take(1)
        .subscribe(o -> submissionCommentsAdapter.forceDisposeDraftSubscribers());

    // Reply discards.
    submissionCommentsAdapter.streamReplyDiscardClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(discardEvent -> {
          Keyboards.hide(getContext(), commentRecyclerView);
          commentTreeConstructor.hideReply(discardEvent.parentContribution());
        });

    // Reply GIF clicks.
    submissionCommentsAdapter.streamReplyGifClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(clickEvent -> {
          Activity activity = (Activity) getContext();
          activity.startActivityForResult(GiphyPickerActivity.intentWithPayload(getContext(), clickEvent), REQUEST_CODE_PICK_GIF);
        });

    // So it appears that SubredditActivity always gets recreated even when GiphyActivity is in foreground.
    // So when the activity result arrives, this Rx chain should be ready and listening.
    lifecycle().onActivityResults()
        .filter(result -> result.requestCode() == REQUEST_CODE_PICK_GIF && result.isResultOk())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(result -> {
          //noinspection ConstantConditions
          ReplyInsertGifClickEvent gifInsertClickEvent = GiphyPickerActivity.extractExtraPayload(result.data());
          RecyclerView.ViewHolder holder = commentRecyclerView.findViewHolderForItemId(gifInsertClickEvent.replyRowItemId());
          if (holder == null) {
            Timber.e(new IllegalStateException("Couldn't find InlineReplyViewHolder after GIPHY activity result"));
            return;
          }

          //noinspection ConstantConditions
          GiphyGif pickedGiphyGif = GiphyPickerActivity.extractPickedGif(result.data());
          ((SubmissionCommentInlineReply.ViewHolder) holder).handlePickedGiphyGif(pickedGiphyGif);
        });

    // Reply fullscreen clicks.
    submissionCommentsAdapter.streamReplyFullscreenClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(fullscreenClickEvent -> {
          Bundle extraPayload = new Bundle();
          extraPayload.putLong(KEY_INLINE_REPLY_ROW_ID, fullscreenClickEvent.replyRowItemId());

          ComposeStartOptions startOptions = ComposeStartOptions.builder()
              .secondPartyName(fullscreenClickEvent.authorNameIfComment())
              .parentContribution(fullscreenClickEvent.parentContribution())
              .draftKey(fullscreenClickEvent.parentContribution())
              .preFilledText(fullscreenClickEvent.replyMessage())
              .extras(extraPayload)
              .build();
          Activity activity = (Activity) getContext();
          activity.startActivityForResult(ComposeReplyActivity.intent(getContext(), startOptions), REQUEST_CODE_FULLSCREEN_REPLY);
        });

    // Fullscreen reply results.
    Relay<ReplySendClickEvent> fullscreenReplySendStream = BehaviorRelay.create();
    //noinspection ConstantConditions
    lifecycle().onActivityResults()
        .filter(activityResult -> activityResult.requestCode() == REQUEST_CODE_FULLSCREEN_REPLY && activityResult.isResultOk())
        .map(activityResult -> ComposeReplyActivity.extractActivityResult(activityResult.data()))
        .doOnNext(composeResult -> {
          //noinspection ConstantConditions
          long inlineReplyRowId = composeResult.extras().getLong(KEY_INLINE_REPLY_ROW_ID);
          RecyclerView.ViewHolder holder = commentRecyclerView.findViewHolderForItemId(inlineReplyRowId);
          if (holder != null) {
            // Inline replies try saving message body to drafts when they're getting dismissed,
            // and because the dismissal happens asynchronously by RecyclerView, it often happens
            // after the draft has already been removed and sent, resulting in the message getting
            // re-saved as a draft. We'll manually disable saving of drafts here to solve that.
            //Timber.i("Disabling draft saving");
            ((SubmissionCommentInlineReply.ViewHolder) holder).setSavingDraftsAllowed(false);

          } else {
            Timber.e(new IllegalStateException("Couldn't find InlineReplyViewHolder after fullscreen reply result"));
          }
        })
        .map(composeResult -> ReplySendClickEvent.create(composeResult.optionalParentContribution().get(), composeResult.reply().toString()))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(fullscreenReplySendStream);

    Predicate<Throwable> scheduleAutoRetry = error -> {
      // For non-network errors, it's important to have an initial delay because the only criteria
      // for auto-retry is to have network. For non-network errors, the user will see an immediate
      // retry which will probably also fail.
      ResolvedError resolvedError = errorResolver.resolve(error);
      boolean shouldDelayAutoRetry = !resolvedError.isNetworkError() && !resolvedError.isRedditServerError();
      long initialDelay = shouldDelayAutoRetry ? 5 : 0;

      RetryReplyJobService.scheduleRetry(getContext(), initialDelay, TimeUnit.SECONDS);
      return true;
    };

    // Reply sends.
    submissionCommentsAdapter.streamReplySendClicks()
        .mergeWith(fullscreenReplySendStream)
        .filter(sendClickEvent -> !sendClickEvent.replyMessage().isEmpty())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(sendClickEvent -> {
          // Posting to RecyclerView's message queue, because onActivityResult() gets called before
          // ComposeReplyActivity is able to exit and so keyboard doesn't get shown otherwise.
          commentRecyclerView.post(() -> Keyboards.hide(getContext(), commentRecyclerView));
          commentTreeConstructor.hideReply(sendClickEvent.parentContribution());

          // Message sending is not a part of the chain so that it does not get unsubscribed on destroy.
          // We're also removing the draft before sending it because even if the reply fails, it'll still
          // be present in the DB for the user to retry. Nothing will be lost.
          submissionStream
              .map(Optional::get)
              .take(1)
              .flatMapCompletable(submission -> replyRepository.removeDraft(sendClickEvent.parentContribution())
                  //.doOnComplete(() -> Timber.i("Sending reply: %s", sendClickEvent.replyMessage()))
                  .andThen(replyRepository.sendReply(sendClickEvent.parentContribution(), ParentThread.of(submission), sendClickEvent.replyMessage()))
              )
              .compose(applySchedulersCompletable())
              .doOnError(e -> Timber.e(e, "Reply send error"))
              .onErrorComplete(scheduleAutoRetry)
              .subscribe();
        });

    // Reply retry-sends.
    submissionCommentsAdapter.streamReplyRetrySendClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(retrySendEvent -> {
          // Re-sending is not a part of the chain so that it does not get unsubscribed on destroy.
          replyRepository.reSendReply(retrySendEvent.failedPendingSyncReply())
              .compose(applySchedulersCompletable())
              .onErrorComplete(scheduleAutoRetry)
              .subscribe();
        });

    // Toggle collapse on comment clicks.
    submissionCommentsAdapter.streamCommentCollapseExpandEvents()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(clickEvent -> {
          if (clickEvent.willCollapseOnClick()) {
            int firstCompletelyVisiblePos = ((LinearLayoutManager) commentRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            boolean commentExtendsBeyondWindowTopEdge = firstCompletelyVisiblePos == -1 || clickEvent.commentRowPosition() < firstCompletelyVisiblePos;
            if (commentExtendsBeyondWindowTopEdge) {
              float viewTop = clickEvent.commentItemView().getY();
              commentRecyclerView.smoothScrollBy(0, (int) viewTop);
            }
          }
          commentTreeConstructor.toggleCollapse(clickEvent.commentInfo());
        });

    // Thread continuations.
    submissionCommentsAdapter.streamLoadMoreCommentsClicks()
        .filter(loadMoreClickEvent -> loadMoreClickEvent.parentCommentNode().isThreadContinuation())
        .withLatestFrom(submissionRequestStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          LoadMoreCommentsClickEvent loadMoreClickEvent = pair.first();
          DankSubmissionRequest continueThreadRequest = pair.second().toBuilder()
              .focusCommentId(loadMoreClickEvent.parentCommentNode().getComment().getId())
              .build();
          Rect expandFromShape = Views.globalVisibleRect(loadMoreClickEvent.loadMoreItemView());
          expandFromShape.top = expandFromShape.bottom;   // Because only expanding from a line is supported so far.
          SubmissionPageLayoutActivity.start(getContext(), continueThreadRequest, expandFromShape);
        });

    // Load-more-comment clicks.
    submissionCommentsAdapter.streamLoadMoreCommentsClicks()
        .filter(loadMoreClickEvent -> !loadMoreClickEvent.parentCommentNode().isThreadContinuation())
        // This filter() is important. Stupid JRAW inserts new comments directly into
        // the comment tree so if multiple API calls are made, it'll insert duplicate
        // items and result in a crash because RecyclerView expects stable IDs.
        .filter(loadMoreClickEvent -> !commentTreeConstructor.isMoreCommentsInFlightFor(loadMoreClickEvent.parentCommentNode()))
        .doOnNext(loadMoreClickEvent -> commentTreeConstructor.setMoreCommentsLoading(loadMoreClickEvent.parentContribution(), true))
        .concatMapEager(loadMoreClickEvent -> submissionRequestStream
            .zipWith(submissionStream.map(Optional::get), Pair::create)
            .take(1)
            .flatMapCompletable(pair -> {
              DankSubmissionRequest submissionRequest = pair.first();
              Submission submission = pair.second();
              return submissionRepository
                  .loadMoreComments(submission, submissionRequest, loadMoreClickEvent.parentCommentNode())
                  .subscribeOn(Schedulers.io());
            })
            .doOnError(e -> {
              ResolvedError resolvedError = errorResolver.resolve(e);
              resolvedError.ifUnknown(() -> Timber.e(e, "Failed to load more comments"));
              Toast.makeText(getContext(), R.string.submission_error_failed_to_load_more_comments, Toast.LENGTH_SHORT).show();
            })
            .onErrorComplete()
            .doOnTerminate(() -> commentTreeConstructor.setMoreCommentsLoading(loadMoreClickEvent.parentContribution(), false))
            .toObservable())
        .takeUntil(lifecycle().onDestroy())
        .subscribe();

    // Content link clicks.
    submissionCommentsAdapter.streamContentLinkClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> {
          if (event.link() instanceof RedditUserLink) {
            // TODO: Open fullscreen user profile.
            Point expandFromPoint = new Point(event.contentLinkView().getLeft(), event.contentLinkView().getBottom());
            urlRouter.forLink(((RedditUserLink) event.link()))
                .expandFrom(expandFromPoint)
                .open(event.contentLinkView());

          } else {
            urlRouter.forLink(event.link())
                .expandFromBelowToolbar()
                .open(getContext());
          }
        });

    // View full thread.
    submissionCommentsAdapter.streamViewAllCommentsClicks()
        .takeUntil(lifecycle().onDestroy())
        .map(request -> request.toBuilder()
            .focusCommentId(null)
            .contextCount(null)
            .build())
        .withLatestFrom(submissionStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          DankSubmissionRequest submissionRequest = pair.first();
          Optional<Submission> submissionWithoutComments = pair.second().map(sub -> new Submission(sub.getDataNode()));
          populateUi(submissionWithoutComments, submissionRequest, callingSubreddits.getValue());
        });
  }

  /**
   * Scroll to <var>parentContribution</var>'s reply if it's not going to be visible because it's located beyond the visible window.
   */
  @CheckResult
  private Completable scrollToNewlyAddedReplyIfHidden(PostedOrInFlightContribution parentContribution) {
    return submissionCommentsAdapter.dataChanges()
        .take(1)
        .map(newItems -> {
          for (int i = 0; i < newItems.size(); i++) {
            // Find the reply item's position.
            SubmissionScreenUiModel commentRow = newItems.get(i);
            //noinspection ConstantConditions Parent contribution's fullname cannot be null. Replies can only be made if they're non-null.
            if (commentRow.type() == SubmissionCommentRowType.INLINE_REPLY
                && ((SubmissionCommentInlineReply.UiModel) commentRow).parentContribution().fullName().equals(parentContribution.fullName()))
            {
              return i;
            }
          }
          throw new AssertionError("Couldn't find inline reply's parent");
        })
        .flatMapCompletable(replyPosition -> Completable.fromAction(() -> {
          RecyclerView.ViewHolder parentContributionItemVH = commentRecyclerView.findViewHolderForAdapterPosition(replyPosition - 1);
          if (parentContributionItemVH == null && submissionStream.getValue().get().getFullName().equals(parentContribution.fullName())) {
            // Submission reply. The ViewHolder is null because the header isn't visible.
            commentRecyclerView.smoothScrollToPosition(replyPosition);
            return;
          }

          if (parentContributionItemVH == null) {
            throw new AssertionError("Couldn't find reply's parent VH. Submission: " + submissionStream.getValue().get().getPermalink());
          }

          int parentContributionBottom = parentContributionItemVH.itemView.getBottom() + commentListParentSheet.getTop();
          boolean isReplyHidden = parentContributionBottom >= submissionPageLayout.getBottom();

          // So this will not make the reply fully visible, but it'll be enough for the reply field
          // to receive focus which will in turn trigger the keyboard and push the field above in
          // the visible window.
          if (isReplyHidden) {
            int dy = parentContributionItemVH.itemView.getHeight();
            commentRecyclerView.smoothScrollBy(0, dy);
          }
        }));
  }

  /**
   * Wait for <var>parentContribution</var>'s reply View to bind and show keyboard once it's visible.
   */
  @CheckResult
  private Observable<ReplyItemViewBindEvent> showKeyboardWhenReplyIsVisible(PostedOrInFlightContribution parentContribution) {
    return submissionCommentsAdapter.streamReplyItemViewBinds()
        .filter(replyBindEvent -> {
          // This filter exists so that the keyboard is shown only for the target reply item
          // instead of another reply item that was found while scrolling to the target reply item.
          //noinspection ConstantConditions
          return replyBindEvent.uiModel().parentContribution().fullName().equals(parentContribution.fullName());
        })
        .take(1)
        .delay(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION, TimeUnit.MILLISECONDS)
        .observeOn(mainThread())
        .doOnNext(replyBindEvent -> commentRecyclerView.post(() -> Keyboards.show(replyBindEvent.replyField())));
  }

  private void setupContentImageView(View fragmentLayout) {
    Views.setMarginBottom(contentImageView, commentsSheetMinimumVisibleHeight);
    contentImageViewHolder = new SubmissionImageHolder(
        lifecycle(),
        fragmentLayout,
        contentLoadProgressView,
        submissionPageLayout,
        deviceDisplayWidth
    );
  }

  private void setupContentVideoView() {
    contentVideoViewHolder = new SubmissionVideoHolder(
        contentVideoView,
        commentListParentSheet,
        contentLoadProgressView,
        submissionPageLayout,
        ExoPlayerManager.newInstance(lifecycle(), contentVideoView),
        mediaHostRepository,
        httpProxyCacheServer,
        deviceDisplayHeight,
        commentsSheetMinimumVisibleHeight
    );
  }

  private void setupCommentsSheet() {
    toolbarBackground.syncPositionWithSheet(commentListParentSheet);
    commentListParentSheet.setScrollingEnabled(false);
    contentLoadProgressView.syncPositionWithSheet(commentListParentSheet);
    contentLoadProgressView.setSyncScrollEnabled(true);

    Function0<Integer> mediaRevealDistanceFunc = () -> {
      // If the sheet cannot scroll up because the top-margin > sheet's peek distance, scroll it to 60%
      // of its height so that the user doesn't get confused upon not seeing the sheet scroll up.
      float visibleMediaHeight = submissionContentStream.getValue().isImageOrGif()
          ? contentImageView.getVisibleZoomedImageHeight()
          : contentVideoViewContainer.getHeight();

      return (int) Math.min(
          commentListParentSheet.getHeight() * 6 / 10,
          visibleMediaHeight - commentListParentSheet.getTop()
      );
    };

    // and on submission title click.
    submissionContentStream
        .ofType(MediaLink.class)
        .filter(o -> commentListParentSheet.isAtMaxScrollY())
        .switchMap(o -> submissionCommentsAdapter.streamHeaderClicks())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(v -> commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.calculate()));

    // Calculates if the top of the comment sheet is directly below the image.
    Function0<Boolean> isCommentSheetBeneathImageFunc = () -> {
      //noinspection CodeBlock2Expr
      return (int) commentListParentSheet.getY() == (int) contentImageView.getVisibleZoomedImageHeight();
    };

    contentImageView.getController().addOnStateChangeListener(new GestureController.OnStateChangeListener() {
      float lastZoom = contentImageView.getZoom();

      @Override
      public void onStateChanged(State state) {
        if (contentImageView.getDrawable() == null) {
          // Image isn't present yet. Ignore.
          return;
        }
        boolean isZoomingOut = lastZoom > state.getZoom();
        lastZoom = state.getZoom();

        // Scroll the comment sheet along with the image if it's zoomed in. This ensures that the sheet always sticks to the bottom of the image.
        int minimumGapWithBottom = 0;
        int contentHeightWithoutKeyboard = deviceDisplayHeight - minimumGapWithBottom - Views.statusBarHeight(contentVideoView.getResources());

        int boundedVisibleImageHeight = (int) Math.min(contentHeightWithoutKeyboard, contentImageView.getVisibleZoomedImageHeight());
        int boundedVisibleImageHeightMinusToolbar = boundedVisibleImageHeight - commentListParentSheet.getTop();
        commentListParentSheet.setMaxScrollY(boundedVisibleImageHeightMinusToolbar);

        if (isCommentSheetBeneathImage
            // This is a hacky workaround: when zooming out, the received callbacks are very discrete and
            // it becomes difficult to lock the comments sheet beneath the image.
            || (isZoomingOut && contentImageView.getVisibleZoomedImageHeight() <= commentListParentSheet.getY()))
        {
          commentListParentSheet.scrollTo(boundedVisibleImageHeightMinusToolbar);
        }
        isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate();
      }

      @Override
      public void onStateReset(State oldState, State newState) {
      }
    });
    commentListParentSheet.addOnSheetScrollChangeListener(newScrollY ->
        isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate()
    );
  }

  private void setupReplyFAB() {
    Observable<Boolean> fabSpaceAvailabilityChanges = submissionCommentsAdapter.streamHeaderBinds()
        .switchMap(optionalHeaderVH -> {
          if (optionalHeaderVH.isEmpty()) {
            return Observable.never();

          } else {
            SubmissionCommentsHeader.ViewHolder headerVH = optionalHeaderVH.get();
            return commentListParentSheet.streamSheetScrollChanges()
                .map(sheetScrollY -> headerVH.bylineView.getBottom() + sheetScrollY + commentListParentSheet.getTop())
                .map(bylineBottom -> bylineBottom < replyFAB.getTop());
          }
        });

    // Show the FAB only while the keyboard is hidden and
    // submission title + byline are positioned above it.
    Observable
        .combineLatest(
            keyboardVisibilityChangeStream.map(event -> event.visible()),
            fabSpaceAvailabilityChanges,
            (keyboardVisible, spaceAvailable) -> !keyboardVisible && spaceAvailable)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(canShowReplyFAB -> {
          if (canShowReplyFAB) {
            replyFAB.show();
          } else {
            replyFAB.hide();
          }
        });

    RxView.clicks(replyFAB)
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), (o, submission) -> submission)
        .map(submission -> PostedOrInFlightContribution.from(submission))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submissionInfo -> {
          if (!userSessionRepository.isUserLoggedIn()) {
            getContext().startActivity(LoginActivity.intent(getContext()));
            return;
          }

          int firstVisiblePosition = ((LinearLayoutManager) commentRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
          boolean isSubmissionReplyVisible = firstVisiblePosition <= 1; // 1 == index of reply field.

          if (commentTreeConstructor.isReplyActiveFor(submissionInfo) && isSubmissionReplyVisible) {
            // Hide reply only if it's visible. Otherwise the user
            // won't understand why the reply FAB did not do anything.
            commentTreeConstructor.hideReply(submissionInfo);
          } else {
            commentTreeConstructor.showReply(submissionInfo);
            inlineReplyStream.accept(submissionInfo);
          }
        });
  }

  /**
   * Smoothly resize content when keyboard is shown or dismissed.
   */
  private void setupSoftInputModeChangesAnimation() {
    keyboardVisibilityChangeStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(new Consumer<KeyboardVisibilityChangeEvent>() {
          private ValueAnimator heightAnimator;
          private ViewGroup contentViewGroup;

          @Override
          public void accept(KeyboardVisibilityChangeEvent changeEvent) throws Exception {
            if (contentViewGroup == null) {
              //noinspection ConstantConditions
              contentViewGroup = ((Activity) getContext()).findViewById(Window.ID_ANDROID_CONTENT);
            }

            if (heightAnimator != null) {
              heightAnimator.cancel();
            }

            if (!commentListParentSheet.hasSheetReachedTheTop()) {
              // Bug workaround: when the sheet is not at the top, avoid smoothly animating the size change.
              // The sheet gets anyway pushed by the keyboard smoothly. Otherwise, only the list was getting
              // scrolled by the keyboard and the sheet wasn't receiving any nested scroll callbacks.
              Views.setHeight(contentViewGroup, changeEvent.contentHeightCurrent());
              return;
            }

            heightAnimator = ObjectAnimator.ofInt(changeEvent.contentHeightPrevious(), changeEvent.contentHeightCurrent());
            heightAnimator.addUpdateListener(animation -> Views.setHeight(contentViewGroup, (int) animation.getAnimatedValue()));
            heightAnimator.setInterpolator(Animations.INTERPOLATOR);
            heightAnimator.setDuration(ACTIVITY_CONTENT_RESIZE_ANIM_DURATION);
            heightAnimator.start();
          }
        });
  }

  private void setupStatusBarTint() {
    //noinspection ConstantConditions
    int defaultStatusBarColor = ContextCompat.getColor(getContext(), R.color.color_primary_dark);
    int statusBarHeight = Views.statusBarHeight(getResources());
    Observable<Optional<Bitmap>> contentBitmapStream = Observable.merge(
        contentImageViewHolder.streamImageBitmaps(),
        contentVideoViewHolder.streamVideoFirstFrameBitmaps(statusBarHeight).map(Optional::of)
    );

    SubmissionStatusBarTintProvider statusBarTintProvider = new SubmissionStatusBarTintProvider(
        defaultStatusBarColor,
        statusBarHeight,
        deviceDisplayWidth
    );

    // Reset the toolbar icons' tint until the content is loaded.
    submissionContentStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> toolbarCloseButton.setColorFilter(Color.WHITE));

    // For images and videos.
    statusBarTintProvider.streamStatusBarTintColor(contentBitmapStream, submissionPageLayout, commentListParentSheet)
        // Using switchMap() instead of delay() here so that any pending delay gets canceled in case a new tint is received.
        .switchMap(statusBarTint -> Observable.just(statusBarTint).delay(statusBarTint.delayedTransition() ? 100 : 0, TimeUnit.MILLISECONDS))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(new Consumer<StatusBarTint>() {
          public ValueAnimator tintChangeAnimator;

          @Override
          public void accept(StatusBarTint statusBarTint) throws Exception {
            if (tintChangeAnimator != null) {
              tintChangeAnimator.cancel();
            }

            Window window = ((Activity) getContext()).getWindow();
            tintChangeAnimator = ValueAnimator.ofArgb(window.getStatusBarColor(), statusBarTint.color());
            tintChangeAnimator.addUpdateListener(animation -> window.setStatusBarColor((int) animation.getAnimatedValue()));
            tintChangeAnimator.setDuration(150L);
            tintChangeAnimator.setInterpolator(Animations.INTERPOLATOR);
            tintChangeAnimator.start();

            // Set a light status bar on M+.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              int flags = submissionPageLayout.getSystemUiVisibility();
              if (!statusBarTint.isDarkColor()) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
              } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
              }
              submissionPageLayout.setSystemUiVisibility(flags);
            }

            // Use darker colors on light images.
            if (submissionPageLayout.getTranslationY() == 0f) {
              toolbarCloseButton.setColorFilter(statusBarTint.isDarkColor() ? Color.WHITE : Color.DKGRAY);
            }
          }
        }, logError("Failed to generate tint"));
  }

  /** Manage showing of error when loading the entire submission fails. */
  private void setupSubmissionLoadErrors() {
    Observable<Object> sharedRetryClicks = submissionCommentsAdapter.streamCommentsLoadRetryClicks().share();

    sharedRetryClicks.mergeWith(lifecycle().onPageCollapse())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> commentsLoadErrors.accept(Optional.empty()));

    sharedRetryClicks
        .withLatestFrom(submissionRequestStream, (o, req) -> req)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submissionRequestStream);

    sharedRetryClicks
        .withLatestFrom(submissionStream, (o, sub) -> sub)
        .filter(Optional::isEmpty)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> contentLoadProgressView.show());

    commentsLoadErrors
        .filter(Optional::isPresent)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> contentLoadProgressView.hide());
  }

  private void setupSubmissionContentLoadErrors() {
    lifecycle().onPageCollapse()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> mediaContentLoadErrors.accept(Optional.empty()));

    submissionCommentsAdapter.streamMediaContentLoadRetryClicks()
        .ofType(SubmissionContentLoadError.LoadFailure.class)
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), (o, sub) -> sub)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submission -> {
          mediaContentLoadErrors.accept(Optional.empty());
          loadSubmissionContent(submission);
        });

    submissionCommentsAdapter.streamMediaContentLoadRetryClicks()
        .ofType(SubmissionContentLoadError.NsfwContentDisabled.class)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          getContext().startActivity(UserPreferencesActivity.intent(getContext(), UserPreferenceGroup.FILTERS));

          lifecycle().onResume()
              .map(oo -> showNsfwContentPreference.get().get())
              .filter(nsfwEnabled -> nsfwEnabled)
              .flatMap(oo -> submissionStream.map(Optional::get))
              .firstOrError()
              .takeUntil(lifecycle().onPageCollapseOrDestroy().ignoreElements())
              .subscribe(submission -> {
                mediaContentLoadErrors.accept(Optional.empty());
                loadSubmissionContent(submission);
              });
        });
  }

  /**
   * Update the submission to be shown. Since this page is retained by {@link SubredditActivity},
   * we only update the UI everytime a new submission is to be shown.
   *
   * @param submission        When empty, the UI gets populated when comments are loaded along with the submission details.
   * @param submissionRequest Used for loading the comments of this submission.
   * @param callingSubreddit  Subreddit name from where this submission is being open. Empty when being opened from elsewhere.
   */
  public void populateUi(Optional<Submission> submission, DankSubmissionRequest submissionRequest, Optional<String> callingSubreddit) {
    // This will load comments and then again update the title, byline and content.
    submissionRequestStream.accept(submissionRequest);

    callingSubreddits.accept(callingSubreddit);

    // Wait till the submission is fetched before loading content.
    submissionStream
        .skip(1)  // Current value.
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(fetchedSubmission -> fetchedSubmission.getId().equals(submissionRequest.id()))
        .take(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(this::loadSubmissionContent);

    if (submission.isPresent()) {
      // This will setup the title, byline and content immediately.
      submissionStream.accept(submission);
    } else {
      contentLoadProgressView.show();
    }
  }

  private void loadSubmissionContent(Submission submission) {
    Single.fromCallable(() -> UrlParser.parse(submission.getUrl()))
        .subscribeOn(io())
        .observeOn(mainThread())
        .flatMap(parsedLink -> {
          if (!(parsedLink instanceof UnresolvedMediaLink)) {
            return Single.just(parsedLink);
          }

          return mediaHostRepository.resolveActualLinkIfNeeded(((MediaLink) parsedLink))
              .subscribeOn(io())
              .doOnSubscribe(o -> {
                // Progress bar is later hidden in the subscribe() block.
                contentLoadProgressView.show();
              })
              .map((Link resolvedLink) -> {
                // Replace Imgur's cover image URL with reddit supplied URL, which will already be cached by Glide.
                if (resolvedLink instanceof ImgurAlbumLink) {
                  ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.of(submission.getThumbnails());
                  int albumContentLinkThumbnailWidth = SubmissionCommentsHeader.getWidthForAlbumContentLinkThumbnail(getContext());
                  String albumCoverImageUrl = redditSuppliedImages.findNearestFor(
                      albumContentLinkThumbnailWidth,
                      ((ImgurAlbumLink) resolvedLink).coverImageUrl()
                  );
                  return ((ImgurAlbumLink) resolvedLink).withCoverImageUrl(albumCoverImageUrl);
                }
                return resolvedLink;
              })
              .observeOn(mainThread())
              .onErrorResumeNext(error -> {
                // Open this album in browser if Imgur rate limits have reached.
                if (error instanceof ImgurApiRequestRateLimitReachedException) {
                  return Single.just(ExternalLink.create(parsedLink.unparsedUrl()));
                } else {
                  handleMediaLoadError(error);
                  return Single.never();
                }
              });
        })
        .flatMap(link -> {
          if (!showNsfwContentPreference.get().get() && link instanceof MediaLink && submission.isNsfw()) {
            contentLoadProgressView.hide();
            mediaContentLoadErrors.accept(Optional.of(SubmissionContentLoadError.NsfwContentDisabled.create()));
            return Single.never();

          } else {
            return Single.just(link);
          }
        })
        .observeOn(mainThread())
        .doOnSuccess(resolvedLink -> {
          contentImageView.setVisibility(resolvedLink.isImageOrGif() ? View.VISIBLE : View.GONE);
          contentVideoViewContainer.setVisibility(resolvedLink.isVideo() ? View.VISIBLE : View.GONE);

          // Show a shadow behind the toolbar because image/video submissions have a transparent toolbar.
          boolean transparentToolbar = resolvedLink.isImageOrGif() || resolvedLink.isVideo();
          toolbarBackground.setSyncScrollEnabled(transparentToolbar);
        })
        .takeUntil(lifecycle().onPageCollapseOrDestroy().ignoreElements())
        .subscribe(
            resolvedLink -> {
              submissionContentStream.accept(resolvedLink);

              switch (resolvedLink.type()) {
                case SINGLE_IMAGE:
                case SINGLE_GIF:
                  Thumbnails redditSuppliedImages = submission.getThumbnails();

                  // Threading is handled internally by SubmissionImageHolder#load().
                  contentImageViewHolder.load((MediaLink) resolvedLink, redditSuppliedImages)
                      .ambWith(lifecycle().onPageCollapseOrDestroy().ignoreElements())
                      .subscribe(doNothingCompletable(), error -> handleMediaLoadError(error));

                  // Open media in full-screen on click.
                  contentImageView.setOnClickListener(o -> urlRouter.forLink(((MediaLink) resolvedLink))
                      .withRedditSuppliedImages(submission.getThumbnails())
                      .open(getContext())
                  );
                  contentImageView.setContentDescription(getResources().getString(
                      R.string.cd_submission_image,
                      submission.getTitle()
                  ));
                  break;

                case REDDIT_PAGE:
                  contentLoadProgressView.hide();
                  if (!submission.isSelfPost()) {
                    contentLinkStream.accept(Optional.of(resolvedLink));
                  }
                  break;

                case MEDIA_ALBUM:
                case EXTERNAL:
                  contentLoadProgressView.hide();
                  contentLinkStream.accept(Optional.of(resolvedLink));
                  break;

                case SINGLE_VIDEO:
                  userPreferences.streamHighResolutionMediaNetworkStrategy()
                      .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy))
                      .firstOrError()
                      .flatMapCompletable(canLoadHighQualityVideo -> contentVideoViewHolder.load((MediaLink) resolvedLink, canLoadHighQualityVideo))
                      .ambWith(lifecycle().onPageCollapseOrDestroy().ignoreElements())
                      .subscribe(doNothingCompletable(), error -> handleMediaLoadError(error));
                  break;

                default:
                  throw new UnsupportedOperationException("Unknown content: " + resolvedLink);
              }

            }, error -> {
              ResolvedError resolvedError = errorResolver.resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Error while loading content"));
            }
        );
  }

  private void handleMediaLoadError(Throwable error) {
    contentLoadProgressView.hide();

    ResolvedError resolvedError = errorResolver.resolve(error);
    resolvedError.ifUnknown(() -> Timber.e(error, "Media content load error"));
    mediaContentLoadErrors.accept(Optional.of(SubmissionContentLoadError.LoadFailure.create(resolvedError)));
  }

// ======== EXPANDABLE PAGE CALLBACKS ======== //

  /**
   * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward pull == downward scroll and vice versa.
   * @return True to consume this touch event. False otherwise.
   */
  @Override
  public boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull) {
    if (touchLiesOn(commentListParentSheet, downX, downY)) {
      return upwardPagePull
          ? commentListParentSheet.canScrollUpwardsAnyFurther()
          : commentListParentSheet.canScrollDownwardsAnyFurther();
    } else {
      return touchLiesOn(contentImageView, downX, downY) && contentImageView.canPanFurtherVertically(upwardPagePull);
    }
  }

  @Override
  public void onPageAboutToExpand(long expandAnimDuration) {
  }

  @Override
  public void onPageExpanded() {
  }

  @Override
  public void onPageAboutToCollapse(long collapseAnimDuration) {
    Keyboards.hide(getContext(), commentRecyclerView);
  }

  @Override
  public void onPageCollapsed() {
    contentVideoViewHolder.pausePlayback();

    commentListParentSheet.scrollTo(0);
    commentListParentSheet.setScrollingEnabled(false);

    submissionStream.accept(Optional.empty());
    contentLinkStream.accept(Optional.empty());
  }

  public SubmissionPageLifecycleStreams lifecycle() {
    return lifecycleStreams;
  }
}
