package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.AttributeSet;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.Toast;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.f2prateek.rx.preferences2.Preference;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubmissionPreview;
import net.dean.jraw.tree.RootCommentNode;
import timber.log.Timber;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.R;
import me.saket.dank.data.ActivityResult;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.exceptions.ImgurApiRequestRateLimitReachedException;
import me.saket.dank.di.Dank;
import me.saket.dank.reply.Reply;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.reply.RetryReplyJobService;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.ScreenSavedState;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.InsertGifDialog;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.giphy.GiphyPickerActivity;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.media.MediaLinkWithStartingPosition;
import me.saket.dank.ui.preferences.UserPreferenceGroup;
import me.saket.dank.ui.preferences.UserPreferencesActivity;
import me.saket.dank.ui.submission.adapter.CommentsItemDiffer;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.submission.adapter.SubmissionCommentInlineReply;
import me.saket.dank.ui.submission.adapter.SubmissionCommentRowType;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsAdapter;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsHeader;
import me.saket.dank.ui.submission.adapter.SubmissionRemoteComment;
import me.saket.dank.ui.submission.adapter.SubmissionScreenUiModel;
import me.saket.dank.ui.submission.adapter.SubmissionUiConstructor;
import me.saket.dank.ui.submission.events.CommentClicked;
import me.saket.dank.ui.submission.events.CommentOptionSwipeEvent;
import me.saket.dank.ui.submission.events.ContributionVoteSwipeEvent;
import me.saket.dank.ui.submission.events.InlineReplyRequestEvent;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.ui.submission.events.MarkMessageAsReadRequested;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.ui.submission.events.SubmissionChangeCommentSortClicked;
import me.saket.dank.ui.submission.events.SubmissionChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentSortChanged;
import me.saket.dank.ui.submission.events.SubmissionCommentsLoadFailed;
import me.saket.dank.ui.submission.events.SubmissionContentLinkClickEvent;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingCompleted;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingFailed;
import me.saket.dank.ui.submission.events.SubmissionContentResolvingStarted;
import me.saket.dank.ui.submission.events.SubmissionMediaLoadFailed;
import me.saket.dank.ui.submission.events.SubmissionNsfwContentFiltered;
import me.saket.dank.ui.submission.events.SubmissionRequestChanged;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.ui.subreddit.events.SubmissionOpenInNewTabSwipeEvent;
import me.saket.dank.ui.subreddit.events.SubmissionOptionSwipeEvent;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.ExternalLink;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Function0;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.LinearSmoothScrollerWithVerticalSnapPref;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.Trio;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SubmissionCommentsItemAnimator;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.walkthrough.SyntheticData;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.KeyboardVisibilityDetector.KeyboardVisibilityChangeEvent;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionPageLayout extends ExpandablePageLayout implements ExpandablePageLayout.OnPullToCollapseIntercepter, SubmissionUi {

  private static final String KEY_WAS_PAGE_EXPANDED_OR_EXPANDING = "pageState";
  private static final String KEY_SUBMISSION_DATA_JSON = "submissionDataJson";
  private static final String KEY_SUBMISSION_REQUEST = "submissionRequest";
  private static final String KEY_INLINE_REPLY_ROW_ID = "inlineReplyRowId";
  private static final String KEY_CALLING_SUBREDDIT = "immediateParentSubreddit";
  private static final String KEY_COMMENT_ROW_COUNT = "commentRowCount";
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
  @Inject UrlRouter urlRouter;
  @Inject Moshi moshi;
  @Inject LinkMetadataRepository linkMetadataRepository;
  @Inject ReplyRepository replyRepository;
  @Inject UserPreferences userPreferences;

  @Inject SubmissionUiConstructor submissionUiConstructor;
  @Inject SubmissionCommentsAdapter commentsAdapter;
  @Inject SubmissionCommentTreeUiConstructor commentTreeUiConstructor;
  @Inject SubmissionController controller;

  @Inject @Named("show_nsfw_content") Lazy<Preference<Boolean>> showNsfwContentPreference;
  @Inject @Named("user_learned_submission_gestures") Lazy<Preference<Boolean>> hasUserLearnedGesturesPref;

  @Inject Lazy<OnLoginRequireListener> onLoginRequireListener;
  @Inject Lazy<VotingManager> votingManager;
  @Inject Lazy<UserSessionRepository> userSessionRepository;
  @Inject Lazy<UrlParser> urlParser;
  @Inject Lazy<SubmissionVideoHolder> contentVideoViewHolder;
  @Inject Lazy<SubmissionImageHolder> contentImageViewHolder;
  @Inject Lazy<ErrorResolver> errorResolver;
  @Inject Lazy<MediaHostRepository> mediaHostRepository;

  private BehaviorRelay<DankSubmissionRequest> submissionRequestStream = BehaviorRelay.create();
  private BehaviorRelay<Optional<SubmissionAndComments>> submissionStream = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Link> submissionContentStream = BehaviorRelay.create();
  private BehaviorRelay<KeyboardVisibilityChangeEvent> keyboardVisibilityChangeStream = BehaviorRelay.create();
  private PublishRelay<InlineReplyRequestEvent> inlineReplyRequestStream = PublishRelay.create();
  private PublishRelay<Identifiable> inlineReplyAdditionStream = PublishRelay.create();
  private BehaviorRelay<Optional<Link>> contentLinkStream = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Optional<SubmissionContentLoadError>> mediaContentLoadErrors = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Optional<ResolvedError>> commentsLoadErrors = BehaviorRelay.createDefault(Optional.empty());
  private BehaviorRelay<Optional<String>> callingSubreddits = BehaviorRelay.createDefault(Optional.empty());

  private SubmissionPageLayout submissionPageLayout;
  private int deviceDisplayWidth, deviceDisplayHeight;
  private boolean isCommentSheetBeneathImage;
  private SubmissionPageLifecycleStreams lifecycleStreams;
  private int commentRowCountBeforeActivityDestroy = -1;

  private Relay<UiEvent> uiEvents = PublishRelay.create();

  public interface Callbacks {
    // TODO: remove this now?
//    SubmissionPageAnimationOptimizer submissionPageAnimationOptimizer();

    void onClickSubmissionToolbarUp();
  }

  public SubmissionPageLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    if (isInEditMode()) {
      return;
    }

    Dank.dependencyInjector().inject(this);
    LayoutInflater.from(context).inflate(R.layout.view_submission, this, true);
    ButterKnife.bind(this, this);

    LifecycleOwnerActivity parentLifecycleOwner = (LifecycleOwnerActivity) getContext();
    lifecycleStreams = SubmissionPageLifecycleStreams.create(this, parentLifecycleOwner);

    // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
    deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    deviceDisplayHeight = getResources().getDisplayMetrics().heightPixels;

    lifecycle().viewAttaches()
        .take(1)
        .takeUntil(lifecycle().viewDetaches())
        .subscribe(o -> onViewFirstAttach());

    // LayoutManager needs to be set before onRestore() gets called to retain scroll position.
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

    uiEvents
        .mergeWith(commentsAdapter.uiEvents())
        .observeOn(io())
        .compose(controller)
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiChange -> uiChange.render(this));

    submissionStream
        .map(SubmissionChanged::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(uiEvents);
  }

  @Override
  public void showProgress() {
    contentLoadProgressView.showForReal();
  }

  @Override
  public void hideProgress() {
    contentLoadProgressView.hideForReal();
  }

  @Override
  public void acceptRequest(DankSubmissionRequest lastRequest) {
    submissionRequestStream.accept(lastRequest);
  }

  public void handleMessageToMarkAsRead(Message message) {
    uiEvents.accept(MarkMessageAsReadRequested.create(message));
  }

  @Override
  public void showChangeSortPopup(SubmissionChangeCommentSortClicked event, DankSubmissionRequest activeRequest) {
    CommentSortingModePopupMenu sortingPopupMenu1 = new CommentSortingModePopupMenu(event.buttonView().getContext(), event.buttonView());
    sortingPopupMenu1.highlightActiveSorting(activeRequest.commentSort().mode());
    sortingPopupMenu1.setOnSortingModeSelectListener(selectedSorting -> uiEvents.accept(SubmissionCommentSortChanged.create(selectedSorting)));
    sortingPopupMenu1.show();
  }

  public void onViewFirstAttach() {
    executeOnMeasure(toolbar, () -> setHeight(toolbarBackground, toolbar.getHeight()));
    //noinspection ConstantConditions
    toolbarCloseButton.setOnClickListener(v -> ((Callbacks) getContext()).onClickSubmissionToolbarUp());

    submissionPageLayout = this;
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

      Optional<SubmissionAndComments> optionalSubmission = submissionStream.getValue();
      Optional<RootCommentNode> optionalComments = optionalSubmission.flatMap(SubmissionAndComments::getComments);
      if (optionalSubmission.isPresent() && !optionalComments.isPresent()) {
        // Comments haven't fetched yet == no submission cached in DB. For us to be able to immediately
        // show UI on orientation change, we unfortunately will have to manually retain this submission.
        JsonAdapter<SubmissionAndComments> adapter = moshi.adapter(SubmissionAndComments.class);
        outState.putString(KEY_SUBMISSION_DATA_JSON, adapter.toJson(optionalSubmission.get()));
      }
    }

    Integer uiModelsSize = Optional.ofNullable(commentsAdapter.getData())
        .map(data -> data.size())
        .orElse(0);
    outState.putInt(KEY_COMMENT_ROW_COUNT, uiModelsSize);

    //noinspection CodeBlock2Expr
    callingSubreddits.getValue().ifPresent(subredditName -> {
      outState.putString(KEY_CALLING_SUBREDDIT, subredditName);
    });
    outState.putBoolean(KEY_WAS_PAGE_EXPANDED_OR_EXPANDING, isExpandedOrExpanding());
    return ScreenSavedState.combine(super.onSaveInstanceState(), outState);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    ScreenSavedState savedState = (ScreenSavedState) state;
    super.onRestoreInstanceState(savedState.superSavedState());

    Bundle restoredValues = savedState.values();
    boolean willPageExpandAgain = restoredValues.getBoolean(KEY_WAS_PAGE_EXPANDED_OR_EXPANDING, false);
    Optional<String> callingSubreddit = Optional.ofNullable(restoredValues.getString(KEY_CALLING_SUBREDDIT));

    commentRowCountBeforeActivityDestroy = restoredValues.getInt(KEY_COMMENT_ROW_COUNT);

    if (willPageExpandAgain && restoredValues.containsKey(KEY_SUBMISSION_REQUEST)) {
      DankSubmissionRequest retainedRequest = restoredValues.getParcelable(KEY_SUBMISSION_REQUEST);
      Single
          .fromCallable(() -> {
            boolean manualRestorationNeeded = restoredValues.containsKey(KEY_SUBMISSION_DATA_JSON);
            if (manualRestorationNeeded) {
              JsonAdapter<SubmissionAndComments> adapter = moshi.adapter(SubmissionAndComments.class);
              //noinspection ConstantConditions
              return Optional.of(adapter.fromJson(restoredValues.getString(KEY_SUBMISSION_DATA_JSON)));
            } else {
              return Optional.<SubmissionAndComments>empty();
            }
          })
          .subscribeOn(io())
          .observeOn(mainThread())
          .takeUntil(lifecycle().onDestroyFlowable())
          .subscribe(
              retainedSubmission -> {
                //noinspection ConstantConditions
                populateUi(retainedSubmission.map(SubmissionAndComments::getSubmission), retainedRequest, callingSubreddit);
              }, error -> {
                ResolvedError resolvedError = errorResolver.get().resolve(error);
                resolvedError.ifUnknown(() -> Timber.e(error, "Error while deserializing saved submission"));
              });
    }
  }

  /**
   * Data from persistence flows to this screen in a unidirectional manner. Any modifications are
   * made on {@link SubmissionCommentTreeUiConstructor} or {@link SubmissionUiConstructor} and
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
    // only once its data-set is available.
    commentsAdapter.dataChanges()
        .filter(uiModels -> uiModels.size() >= commentRowCountBeforeActivityDestroy)
        .take(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> commentRecyclerView.setAdapter(commentsAdapter));

    // Load comments when submission changes.
    submissionRequestStream
        .observeOn(mainThread())
        //.doOnNext(o -> commentsLoadProgressVisibleStream.accept(true))
        //.doOnNext(o -> Timber.d("------------------"))
        .doOnNext(request -> uiEvents.accept(SubmissionRequestChanged.create(request)))
        .switchMap(submissionRequest -> submissionRepository.submissionWithComments(submissionRequest)
            //.compose(RxUtils.doOnceOnNext(o -> Timber.d("Submission received")))
            //.doOnNext(o -> Timber.i("%s comments", o.second().getComments().map(node -> node.totalSize())))
            .flatMap(pair -> {
              // It's possible for the remote to suggest a different sort than what was asked by SubredditActivity.
              // In that case, trigger another request with the correct sort. Doing this because we need to store
              // and retain this request in case the Activity gets recreated.
              DankSubmissionRequest updatedRequest = pair.first();
              if (!updatedRequest.equals(submissionRequest)) {
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
            //.doOnNext(o -> commentsLoadProgressVisibleStream.accept(false))
            .onErrorResumeNext(error -> {
              // TODO: Handle 403: Subreddit private.
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't fetch comments"));
              commentsLoadErrors.accept(Optional.of(resolvedError));
              uiEvents.accept(SubmissionCommentsLoadFailed.create());

              if (submissionStream.getValue().flatMap(submission -> submission.getComments()).isPresent()) {
                Toast.makeText(getContext(), resolvedError.errorMessageRes(), Toast.LENGTH_SHORT).show();
              }

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
            commentTreeUiConstructor,
            submissionStream.observeOn(io()),
            submissionRequestStream.observeOn(io()),
            contentLinkStream.observeOn(io()),
            mediaContentLoadErrors.observeOn(io()),
            commentsLoadErrors.observeOn(io())
        )
        .subscribeOn(io())
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(CommentsItemDiffer.Companion::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyFlowable())
        .subscribe(commentsAdapter);

    // Scroll to focused comment on start.
    submissionRequestStream
        .switchMap(request -> {
          if (request.focusCommentId() == null) {
            return Observable.empty();
          } else {
            return commentsAdapter.dataChanges()
                .observeOn(io())
                .flatMap(rows -> {
                  for (int i = 0; i < rows.size(); i++) {
                    SubmissionScreenUiModel row = rows.get(i);
                    if (row instanceof SubmissionRemoteComment.UiModel && ((SubmissionRemoteComment.UiModel) row).isFocused()) {
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
            error -> Timber.e(error, "Couldn't scroll to focused comment")
        );

    lifecycle().onPageCollapse()
        .startWith(LifecycleStreams.NOTHING)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          contentImageView.setVisibility(View.GONE);
          contentVideoViewContainer.setVisibility(View.GONE);
          toolbarBackground.setSyncScrollEnabled(false);
        });

    commentRecyclerView.addOnItemTouchListener(new RecyclerSwipeListener(commentRecyclerView));

    // Option swipe gestures.
    commentsAdapter.swipeEvents()
        .ofType(SubmissionOptionSwipeEvent.class)
        .withLatestFrom(callingSubreddits, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionOptionSwipeEvent swipeEvent = pair.first();
          Optional<String> optionalCallingSubreddit = pair.second();
          swipeEvent.showPopupForSubmissionScreen(optionalCallingSubreddit, commentListParentSheet);
        });
    commentsAdapter.swipeEvents()
        .ofType(CommentOptionSwipeEvent.class)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> event.showPopup(toolbar));

    // Open in new tab gestures.
    commentsAdapter.swipeEvents()
        .ofType(SubmissionOpenInNewTabSwipeEvent.class)
        .delay(SubmissionOpenInNewTabSwipeEvent.TAB_OPEN_DELAY_MILLIS, TimeUnit.MILLISECONDS)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> event.openInNewTab(urlRouter, urlParser.get()));

    // Reply swipe gestures.
    commentsAdapter.swipeEvents()
        .ofType(InlineReplyRequestEvent.class)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(inlineReplyRequestStream);

    Observable<Boolean> isUserASubredditMod = Observable.just(false);  // TODO v2.

    inlineReplyRequestStream
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), Pair::create)
        .withLatestFrom(isUserASubredditMod, Trio::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(trio -> {
          SubmissionAndComments submissionData = trio.second();
          Boolean isUserMod = trio.third();
          Identifiable parentContribution = trio.first().parentContribution();
          boolean isSubmissionReply = parentContribution instanceof Submission;

          if (submissionData.getSubmission().isArchived()) {
            if (isSubmissionReply) {
              Pair<Intent, ActivityOptions> archivedIntent = ArchivedSubmissionDialogActivity.intentWithFabTransform(
                  ((Activity) getContext()),
                  replyFAB,
                  R.color.submission_fab,
                  R.drawable.ic_reply_white_24dp);
              getContext().startActivity(archivedIntent.first(), archivedIntent.second().toBundle());
            } else {
              getContext().startActivity(ArchivedSubmissionDialogActivity.intent(getContext()));
            }

          } else {
            if (submissionData.getSubmission().isLocked() && !isUserMod) {
              if (isSubmissionReply) {
                Pair<Intent, ActivityOptions> archivedIntent = LockedSubmissionDialogActivity.intentWithFabTransform(
                    ((Activity) getContext()),
                    replyFAB,
                    R.color.submission_fab,
                    R.drawable.ic_reply_white_24dp);
                getContext().startActivity(archivedIntent.first(), archivedIntent.second().toBundle());
              } else {
                getContext().startActivity(LockedSubmissionDialogActivity.intent(getContext()));
              }

            } else {
              if (isSubmissionReply) {
                int firstVisiblePosition = ((LinearLayoutManager) commentRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                boolean isSubmissionReplyVisible = firstVisiblePosition <= 1; // 1 == index of reply field.

                if (commentTreeUiConstructor.isReplyActiveFor(submissionData.getSubmission()) && isSubmissionReplyVisible) {
                  // Hide reply only if it's visible. Otherwise the user
                  // won't understand why the reply FAB did not do anything.
                  commentTreeUiConstructor.hideReply(submissionData.getSubmission());
                } else {
                  commentTreeUiConstructor.showReply(submissionData.getSubmission());
                  inlineReplyAdditionStream.accept(submissionData.getSubmission());
                }

              } else {
                if (commentTreeUiConstructor.isCollapsed(parentContribution) || !commentTreeUiConstructor.isReplyActiveFor(parentContribution)) {
                  commentTreeUiConstructor.showReplyAndExpandComments(parentContribution);
                  inlineReplyAdditionStream.accept(parentContribution);
                } else {
                  Keyboards.hide(getContext(), commentRecyclerView);
                  commentTreeUiConstructor.hideReply(parentContribution);
                }
              }
            }
          }
        });

    // Vote swipe gesture.
    Observable<Pair<ContributionVoteSwipeEvent, Submission>> sharedVoteActions = commentsAdapter.swipeEvents()
        .ofType(ContributionVoteSwipeEvent.class)
        .withLatestFrom(
            submissionStream
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SubmissionAndComments::getSubmission),
            Pair::create)
        .share();

    sharedVoteActions
        .filter(pair -> !pair.second().isArchived())
        .map(Pair::first)
        .flatMapCompletable(voteEvent -> voteEvent.toVote().saveAndSend(votingManager.get())
            .subscribeOn(io()))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    sharedVoteActions
        .filter(pair -> pair.second().isArchived())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> getContext().startActivity(ArchivedSubmissionDialogActivity.intent(getContext())));

    // Inline reply additions.
    // Wait till the reply's View is added to the list and show keyboard.
    inlineReplyAdditionStream
        .switchMapSingle(parentContribution -> scrollToNewlyAddedReplyIfHidden(parentContribution).toSingleDefault(parentContribution))
        .switchMap(this::showKeyboardWhenReplyIsVisible)
        .takeUntil(lifecycle().onDestroy())
        .subscribe();

    // Manually dispose reply draft subscribers, because Adapter#onViewHolderRecycled()
    // doesn't get called if the Activity is getting recreated.
    lifecycle().onDestroy()
        .take(1)
        .subscribe(o -> commentsAdapter.forceDisposeDraftSubscribers());

    // Reply discards.
    commentsAdapter.streamReplyDiscardClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(discardEvent -> {
          Keyboards.hide(getContext(), commentRecyclerView);
          commentTreeUiConstructor.hideReply(discardEvent.parent());
        });

    // Reply GIF clicks.
    commentsAdapter.streamReplyGifClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(clickEvent -> {
          Activity activity = (Activity) getContext();
          activity.startActivityForResult(GiphyPickerActivity.intentWithPayload(getContext(), clickEvent), REQUEST_CODE_PICK_GIF);
        });

    // So it appears that SubredditActivity always gets recreated even when GiphyActivity is in foreground.
    // So when the activity result arrives, this Rx chain should be ready and listening.
    lifecycle().onActivityResults()
        .filter(result -> result.requestCode() == REQUEST_CODE_PICK_GIF && result.isResultOk())
        .map(ActivityResult::data)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(resultData -> {
          ReplyInsertGifClickEvent gifInsertClickEvent = GiphyPickerActivity.extractExtraPayload(resultData);
          GiphyGif pickedGiphyGif = GiphyPickerActivity.extractPickedGif(resultData);
          InsertGifDialog.showWithPayload(((DankActivity) getContext()).getSupportFragmentManager(), pickedGiphyGif, gifInsertClickEvent);
        });

    // Reply fullscreen clicks.
    commentsAdapter.streamReplyFullscreenClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(event -> {
          Bundle payload = new Bundle();
          payload.putLong(KEY_INLINE_REPLY_ROW_ID, event.replyRowItemId());
          event.openComposeForResult((Activity) getContext(), payload, REQUEST_CODE_FULLSCREEN_REPLY);
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
      ResolvedError resolvedError = errorResolver.get().resolve(error);
      boolean shouldDelayAutoRetry = !resolvedError.isNetworkError() && !resolvedError.isRedditServerError();
      long initialDelay = shouldDelayAutoRetry ? 5 : 0;

      RetryReplyJobService.scheduleRetry(getContext(), initialDelay, TimeUnit.SECONDS);
      return true;
    };

    // Reply sends.
    commentsAdapter.streamReplySendClicks()
        .mergeWith(fullscreenReplySendStream)
        .filter(sendClickEvent -> !sendClickEvent.replyBody().isEmpty())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(sendClickEvent -> {
          // Posting to RecyclerView's message queue, because onActivityResult() gets called before
          // ComposeReplyActivity is able to exit and so keyboard doesn't get shown otherwise.
          commentRecyclerView.post(() -> Keyboards.hide(getContext(), commentRecyclerView));
          commentTreeUiConstructor.hideReply(sendClickEvent.parentContribution());

          // Message sending is not a part of the chain so that it does not get unsubscribed on destroy.
          // We're also removing the draft before sending it because even if the reply fails, it'll still
          // be present in the DB for the user to retry. Nothing will be lost.
          submissionStream
              .map(Optional::get)
              .take(1)
              .flatMapCompletable(submission -> {
                long createdTimeMillis = System.currentTimeMillis();
                Reply reply = Reply.create(
                    sendClickEvent.parentContribution(),
                    ParentThread.of(submission.getSubmission()),
                    sendClickEvent.replyBody(),
                    createdTimeMillis);

                return replyRepository.removeDraft(sendClickEvent.parentContribution())
                    //.doOnComplete(() -> Timber.i("Sending reply: %s", reply))
                    .andThen(reply.saveAndSend(replyRepository));
              })
              .compose(applySchedulersCompletable())
              .doOnError(e -> {
                ResolvedError resolvedError = errorResolver.get().resolve(e);
                resolvedError.ifUnknown(() -> Timber.e(e, "Reply send error"));
              })
              .onErrorComplete(scheduleAutoRetry)
              .subscribe();
        });

    // Reply retry-sends.
    commentsAdapter.uiEvents()
        .ofType(ReplyRetrySendClickEvent.class)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(retrySendEvent -> {
          // Re-sending is not a part of the chain so that it does not get unsubscribed on destroy.
          replyRepository.reSendReply(retrySendEvent.failedPendingSyncReply())
              .compose(applySchedulersCompletable())
              .onErrorComplete(scheduleAutoRetry)
              .subscribe();
        });

    // Toggle collapse on comment clicks.
    commentsAdapter.uiEvents()
        .ofType(CommentClicked.class)
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
          commentTreeUiConstructor.toggleCollapse(clickEvent.comment());
        });

    // Thread continuations.
    commentsAdapter.streamLoadMoreCommentsClicks()
        .filter(loadMoreClickEvent -> JrawUtils2.isThreadContinuation(loadMoreClickEvent.parentCommentNode()))
        .withLatestFrom(submissionRequestStream, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          LoadMoreCommentsClickEvent loadMoreClickEvent = pair.first();
          DankSubmissionRequest currentSubmissionRequest = pair.second();
          loadMoreClickEvent.openThreadContinuation(currentSubmissionRequest);
        });

    // Load-more-comment clicks.
    commentsAdapter.streamLoadMoreCommentsClicks()
        .filter(loadMoreClickEvent -> !JrawUtils2.isThreadContinuation(loadMoreClickEvent.parentCommentNode()))
        // This filter() is important. Stupid JRAW inserts new comments directly into
        // the comment tree so if multiple API calls are made, it'll insert duplicate
        // items and result in a crash because RecyclerView expects stable IDs.
        .filter(loadMoreClickEvent -> !commentTreeUiConstructor.isMoreCommentsInFlightFor(loadMoreClickEvent.parentCommentNode()))
        .doOnNext(loadMoreClickEvent -> commentTreeUiConstructor.setMoreCommentsLoading(loadMoreClickEvent.parentComment(), true))
        .concatMapEager(loadMoreClickEvent -> submissionRequestStream
            .zipWith(submissionStream.map(Optional::get), Pair::create)
            .take(1)
            .flatMapCompletable(pair -> {
              DankSubmissionRequest submissionRequest = pair.first();
              SubmissionAndComments submissionData = pair.second();
              //noinspection unchecked
              return submissionRepository
                  .loadAndSaveMoreComments(submissionData, submissionRequest, loadMoreClickEvent.parentCommentNode())
                  .subscribeOn(Schedulers.io());
            })
            .doOnError(e -> {
              ResolvedError resolvedError = errorResolver.get().resolve(e);
              resolvedError.ifUnknown(() -> Timber.e(e, "Failed to load more comments"));
              Toast.makeText(getContext(), R.string.submission_error_failed_to_load_more_comments, Toast.LENGTH_SHORT).show();
            })
            .onErrorComplete()
            .doOnTerminate(() -> commentTreeUiConstructor.setMoreCommentsLoading(loadMoreClickEvent.parentComment(), false))
            .toObservable())
        .takeUntil(lifecycle().onDestroy())
        .subscribe();

    // Content link clicks.
    commentsAdapter.streamContentLinkClicks()
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionContentLinkClickEvent event = pair.first();
          SubmissionAndComments submissionData = pair.second();
          event.openContent(submissionData.getSubmission(), urlRouter);
        });

    // Content link long clicks.
    commentsAdapter.streamContentLinkLongClicks()
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionContentLinkClickEvent event = pair.first();
          SubmissionAndComments submissionData = pair.second();
          event.showOptionsPopup(submissionData.getSubmission());
        });
  }

  public void onGifInsert(String title, GiphyGif gif, Parcelable payload) {
    ReplyInsertGifClickEvent gifInsertClickEvent = ((ReplyInsertGifClickEvent) payload);
    RecyclerView.ViewHolder holder = commentRecyclerView.findViewHolderForItemId(gifInsertClickEvent.replyRowItemId());
    if (holder == null) {
      Timber.e(new IllegalStateException("Couldn't find InlineReplyViewHolder after GIPHY activity result"));
      return;
    }
    ((SubmissionCommentInlineReply.ViewHolder) holder).handlePickedGiphyGif(title, gif);
  }

  /**
   * Scroll to <var>parent</var>'s reply if it's not going to
   * be visible because it's located beyond the visible window.
   */
  @CheckResult
  private Completable scrollToNewlyAddedReplyIfHidden(Identifiable parentContribution) {
    return commentsAdapter.dataChanges()
        .map(newItems -> {
          for (int i = 0; i < newItems.size(); i++) {
            // Find the reply item's position.
            SubmissionScreenUiModel commentRow = newItems.get(i);
            //noinspection ConstantConditions Parent contribution's fullname cannot be null. Replies can only be made if they're non-null.
            if (commentRow.type() == SubmissionCommentRowType.INLINE_REPLY
                && ((SubmissionCommentInlineReply.UiModel) commentRow).parent().getFullName().equals(parentContribution.getFullName()))
            {
              return i;
            }
          }
          //throw new AssertionError("Couldn't find inline reply's parent");
          return RecyclerView.NO_POSITION;
        })
        .filter(replyPosition -> replyPosition != RecyclerView.NO_POSITION)
        .take(1)
        .flatMapCompletable(replyPosition -> Completable.fromAction(() -> {
          RecyclerView.ViewHolder parentContributionItemVH = commentRecyclerView.findViewHolderForAdapterPosition(replyPosition - 1);
          Submission activeSubmission = submissionStream.getValue().get().getSubmission();
          if (parentContributionItemVH == null && activeSubmission.getFullName().equals(parentContribution.getFullName())) {
            // Submission reply. The ViewHolder is null because the header isn't visible.
            commentRecyclerView.smoothScrollToPosition(replyPosition);
            return;
          }

          if (parentContributionItemVH == null) {
            throw new AssertionError("Couldn't find reply's parent VH. Submission: " + activeSubmission.getPermalink());
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
   * Wait for <var>parent</var>'s reply View to bind and show keyboard once it's visible.
   */
  @CheckResult
  private Observable<ReplyItemViewBindEvent> showKeyboardWhenReplyIsVisible(Identifiable parentContribution) {
    return commentsAdapter.streamReplyItemViewBinds()
        .filter(replyBindEvent -> {
          // This filter exists so that the keyboard is shown only for the target reply item
          // instead of another reply item that was found while scrolling to the target reply item.
          //noinspection ConstantConditions
          return replyBindEvent.uiModel().parent().getFullName().equals(parentContribution.getFullName());
        })
        .take(1)
        .delay(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION, TimeUnit.MILLISECONDS)
        .observeOn(mainThread())
        .doOnNext(replyBindEvent -> commentRecyclerView.post(() -> Keyboards.show(replyBindEvent.replyField())));
  }

  private void setupContentImageView(View fragmentLayout) {
    Views.setMarginBottom(contentImageView.view(), commentsSheetMinimumVisibleHeight);
    contentImageViewHolder.get().setup(
        uiEvents,
        lifecycle(),
        fragmentLayout,
        submissionPageLayout,
        new Size(deviceDisplayWidth, deviceDisplayHeight)
    );

    // Open media in full-screen on click.
    RxView.clicks(contentImageView.view())
        .withLatestFrom(submissionContentStream, (o, contentLink) -> contentLink)
        .filter(contentLink -> contentLink.isImageOrGif())
        .cast(MediaLink.class)
        .withLatestFrom(submissionStream.filter(Optional::isPresent).map(Optional::get), Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair ->
            urlRouter.forLink(pair.first())
                .withRedditSuppliedImages(pair.second().getSubmission().getPreview())
                .open(getContext())
        );
  }

  private void setupContentVideoView() {
    ExoPlayerManager exoPlayerManager = ExoPlayerManager.newInstance(contentVideoView);
    exoPlayerManager.manageLifecycle(lifecycle())
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    contentVideoViewHolder.get().setup(
        uiEvents,
        exoPlayerManager,
        contentVideoView,
        commentListParentSheet,
        submissionPageLayout,
        lifecycle(),
        new Size(deviceDisplayWidth, deviceDisplayHeight),
        Views.statusBarHeight(getResources()),
        commentsSheetMinimumVisibleHeight
    );

    // Open media in full-screen on click.
    contentVideoViewHolder.get().streamVideoClicks()
        .withLatestFrom(submissionContentStream.ofType(MediaLink.class), Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubmissionVideoClickEvent clickEvent = pair.first();
          MediaLinkWithStartingPosition videoLink = MediaLinkWithStartingPosition.create(pair.second(), clickEvent.seekPosition());
          urlRouter
              .forLink(videoLink)
              .open(getContext());
        });
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

    submissionContentStream
        .ofType(MediaLink.class)
        .filter(o -> commentListParentSheet.isAtMaxScrollY())
        .switchMap(o -> commentsAdapter.streamHeaderClicks())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          if (submissionContentStream.getValue() instanceof MediaLink) {
            commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.calculate());
          } else {
            // FIXME: Investigate this.
            Timber.e("Received a header click for a non-media link.");
          }
        });

    // Calculates if the top of the comment sheet is directly below the image.
    Function0<Boolean> isCommentSheetBeneathImageFunc = () -> {
      //noinspection CodeBlock2Expr
      return (int) commentListParentSheet.getY() == (int) contentImageView.getVisibleZoomedImageHeight();
    };

    contentImageView.addOnImageZoomChangeListener(new ZoomableImageView.OnZoomChangeListener() {
      float lastZoom = contentImageView.getZoom();

      @Override
      public void onZoomChange(float zoom) {
        if (!contentImageView.hasImage()) {
          // Image isn't present yet. Ignore.
          return;
        }
        boolean isZoomingOut = lastZoom > zoom;
        lastZoom = zoom;

        // Scroll the comment sheet along with the image if it's zoomed in. This ensures that the sheet always sticks to the bottom of the image.
        int minimumGapWithBottom = 0;
        int contentHeightWithoutKeyboard = deviceDisplayHeight - minimumGapWithBottom - Views.statusBarHeight(getResources());

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
    });

    commentListParentSheet.addOnSheetScrollChangeListener(newScrollY ->
        isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate()
    );
  }

  private void setupReplyFAB() {
    Observable<Boolean> fabSpaceAvailabilityChanges = commentsAdapter.streamHeaderBinds()
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
        .takeUntil(lifecycle().onDestroy())
        .withLatestFrom(
            submissionStream
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SubmissionAndComments::getSubmission),
            (o, submission) -> submission)
        .map(InlineReplyRequestEvent::create)
        .subscribe(inlineReplyRequestStream);
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
          public void accept(KeyboardVisibilityChangeEvent changeEvent) {
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
    Observable<Optional<Bitmap>> contentBitmapStream = Observable.merge(
        contentImageViewHolder.get().streamImageBitmaps(),
        contentVideoViewHolder.get().streamVideoFirstFrameBitmaps().map(Optional::of)
    );

    SubmissionStatusBarTintProvider statusBarTintProvider = new SubmissionStatusBarTintProvider(
        defaultStatusBarColor,
        Views.statusBarHeight(getResources()),
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
          public void accept(StatusBarTint statusBarTint) {
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
    Observable<Object> sharedRetryClicks = commentsAdapter.streamCommentsLoadRetryClicks().share();

    submissionRequestStream
        .cast(Object.class)
        .mergeWith(lifecycle().onPageCollapse())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> commentsLoadErrors.accept(Optional.empty()));

    sharedRetryClicks
        .withLatestFrom(submissionRequestStream, (o, req) -> req)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submissionRequestStream);
  }

  private void setupSubmissionContentLoadErrors() {
    lifecycle().onPageCollapse()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> mediaContentLoadErrors.accept(Optional.empty()));

    commentsAdapter.streamMediaContentLoadRetryClicks()
        .ofType(SubmissionContentLoadError.LoadFailure.class)
        .withLatestFrom(
            submissionStream
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SubmissionAndComments::getSubmission),
            (o, sub) -> sub)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(submission -> {
          mediaContentLoadErrors.accept(Optional.empty());
          loadSubmissionContent(submission);
        });

    commentsAdapter.streamMediaContentLoadRetryClicks()
        .ofType(SubmissionContentLoadError.NsfwContentDisabled.class)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          getContext().startActivity(UserPreferencesActivity.intent(getContext(), UserPreferenceGroup.FILTERS));

          lifecycle().onResume()
              .map(oo -> showNsfwContentPreference.get().get())
              .filter(nsfwEnabled -> nsfwEnabled)
              .flatMap(oo -> submissionStream.map(Optional::get))
              .firstOrError()
              .takeUntil(lifecycle().onPageCollapseOrDestroyCompletable())
              .subscribe(
                  submissionData -> {
                    mediaContentLoadErrors.accept(Optional.empty());
                    loadSubmissionContent(submissionData.getSubmission());
                  }, error -> {
                    ResolvedError resolvedError = errorResolver.get().resolve(error);
                    resolvedError.ifUnknown(() -> Timber.e(error, "Error while waiting for user to return from preferences"));
                  });
        });
  }

  /**
   * Update the submission to be shown. Since this page is retained by {@link SubredditActivity},
   * we only update the UI every time a new submission is to be shown.
   * <p>
   * populateUi() shouldn't be called internally because it'll reload the content every time.
   *
   * @param optionalSubmission When empty, the UI gets populated when comments are loaded along with the submission details.
   * @param submissionRequest  Used for loading the comments of this submission.
   * @param callingSubreddit   Subreddit name from where this submission is being open. Empty when being opened from elsewhere.
   */
  public void populateUi(Optional<Submission> optionalSubmission, DankSubmissionRequest submissionRequest, Optional<String> callingSubreddit) {
    // This will load comments and then again update the title, byline and content.
    submissionRequestStream.accept(submissionRequest);

    callingSubreddits.accept(callingSubreddit);

    // Wait till the submission is fetched before loading content.
    submissionStream
        .skip(1)  // Current value.
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(SubmissionAndComments::getSubmission)
        .filter(fetchedSubmission -> fetchedSubmission.getId().equals(submissionRequest.id()))
        .take(1)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(this::loadSubmissionContent);

    if (optionalSubmission.isPresent()) {
      // This will setup the title, byline and content immediately.
      Timber.d("Populating submission");
      submissionStream.accept(optionalSubmission.map(submission -> new SubmissionAndComments(submission, Optional.empty())));
    }
  }

  private void loadSubmissionContent(Submission submission) {
    Single.fromCallable(() -> urlParser.get().parse(submission.getUrl(), submission))
        .subscribeOn(io())
        .observeOn(mainThread())
        .flatMapObservable(parsedLink -> {
          if (!(parsedLink instanceof MediaLink)) {
            return Observable.just(parsedLink);
          }

          return mediaHostRepository.get().resolveActualLinkIfNeeded(((MediaLink) parsedLink))
              .subscribeOn(io())
              .doOnSubscribe(o -> {
                // Progress bar is later hidden when the link is resolved (in subscribe()).
                uiEvents.accept(SubmissionContentResolvingStarted.create());
              })
              .map((Link resolvedLink) -> {
                // Replace Imgur's cover image URL with reddit supplied URL, which will already be cached by Glide.
                if (resolvedLink instanceof ImgurAlbumLink) {
                  ImageWithMultipleVariants redditSuppliedImages = ImageWithMultipleVariants.Companion.of(submission.getPreview());
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
                  return Observable.just(ExternalLink.create(parsedLink.unparsedUrl()));
                } else {
                  uiEvents.accept(SubmissionContentResolvingFailed.create());
                  showMediaLoadError(error);
                  return Observable.never();
                }
              });
        })
        .flatMapSingle(link -> {
          if (!showNsfwContentPreference.get().get() && link instanceof MediaLink && submission.isNsfw()) {
            mediaContentLoadErrors.accept(Optional.of(SubmissionContentLoadError.NsfwContentDisabled.create()));
            uiEvents.accept(SubmissionNsfwContentFiltered.create());
            return Single.never();

          } else {
            return Single.just(link);
          }
        })
        .observeOn(mainThread())
        .doOnNext(resolvedLink -> {
          contentImageView.setVisibility(resolvedLink.isImageOrGif() ? View.VISIBLE : View.GONE);
          contentVideoViewContainer.setVisibility(resolvedLink.isVideo() ? View.VISIBLE : View.GONE);

          // Show a shadow behind the toolbar because image/video submissions have a transparent toolbar.
          boolean transparentToolbar = resolvedLink.isImageOrGif() || resolvedLink.isVideo();
          toolbarBackground.setSyncScrollEnabled(transparentToolbar);
        })
        .takeUntil(lifecycle().onPageCollapseOrDestroy())
        .subscribe(
            resolvedLink -> {
              submissionContentStream.accept(resolvedLink);
              uiEvents.accept(SubmissionContentResolvingCompleted.create(resolvedLink));

              switch (resolvedLink.type()) {
                case SINGLE_IMAGE:
                case SINGLE_GIF:
                  SubmissionPreview redditSuppliedImages = submission.getPreview();

                  // Threading is handled internally by SubmissionImageHolder#load().
                  contentImageViewHolder.get().load((MediaLink) resolvedLink, redditSuppliedImages)
                      .ambWith(lifecycle().onPageCollapseOrDestroyCompletable())
                      .subscribe(doNothingCompletable(), error -> tryRecoveringOrShowMediaLoadError(error, resolvedLink));

                  contentImageView.view().setContentDescription(getResources().getString(
                      R.string.cd_submission_image,
                      submission.getTitle()
                  ));
                  break;

                case REDDIT_PAGE:
                  if (!submission.isSelfPost()) {
                    contentLinkStream.accept(Optional.of(resolvedLink));
                  }
                  break;

                case MEDIA_ALBUM:
                case EXTERNAL:
                  contentLinkStream.accept(Optional.of(resolvedLink));
                  break;

                case SINGLE_VIDEO:
                  contentVideoViewHolder.get().load((MediaLink) resolvedLink)
                      .toObservable()
                      .takeUntil(lifecycle().onPageCollapseOrDestroy())
                      .subscribe(doNothing(), error -> tryRecoveringOrShowMediaLoadError(error, resolvedLink));
                  break;

                default:
                  throw new UnsupportedOperationException("Unknown content: " + resolvedLink);
              }

            }, error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Error while loading content"));
              // TODO: Rename SubmissionMediaContentLoadError to SubmissionContentLoadError.
            }
        );
  }

  private void tryRecoveringOrShowMediaLoadError(Throwable error, Link resolvedContentLink) {
    if (error instanceof ExoPlaybackException && error.getCause() instanceof HttpDataSource.HttpDataSourceException) {
      // Flagging this link will trigger another emission from media repository, so no need to do anything else.
      mediaHostRepository.get().flagLocalUrlParsingAsIncorrect(resolvedContentLink);

    } else {
      uiEvents.accept(SubmissionMediaLoadFailed.create());
      showMediaLoadError(error);
    }
  }

  private void showMediaLoadError(Throwable error) {
    ResolvedError resolvedError = errorResolver.get().resolve(error);
    if (resolvedError.isUnknown()) {
      Timber.e(error, "Media content load error: %s", submissionStream.getValue().get().getSubmission().getPermalink());
    }

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
    }

    //noinspection SimplifiableIfStatement
    if (commentListParentSheet.hasSheetReachedTheTop() && touchLiesOn(toolbar, downX, downY)) {
      return false;
    }

    return touchLiesOn(contentImageView.view(), downX, downY) && contentImageView.canPanFurtherVertically(upwardPagePull);
  }

  protected boolean shouldExpandMediaSmoothly() {
    //Timber.i("--------------------");
    //Timber.i("Page: %s", submissionPageLayout.getCurrentState());
    switch (submissionPageLayout.getCurrentState()) {
      case COLLAPSED:
        return false;

      case EXPANDING:
        //Timber.i("Page ty: %s", submissionPageLayout.getTranslationY());
        //Timber.i("Page h: %s", submissionPageLayout.getHeight());
        // It's better if translation-Y is closer to 0, because that's the distance left for the page to fully expand.
        return submissionPageLayout.getTranslationY() <= submissionPageLayout.getHeight() * 0.2f;

      case COLLAPSING:
      case EXPANDED:
        return true;

      default:
        throw new AssertionError("Unknown page state: " + submissionPageLayout.getCurrentState());
    }
  }

  @Override
  protected void onPageExpanded() {
    lifecycle().onPageCollapse()
        .take(1)
        .withLatestFrom(submissionStream, (o, submission) -> submission)
        .filter(optionalSubmission -> optionalSubmission
            .map(SubmissionAndComments::getSubmission)
            .map(submission -> SyntheticData.Companion.isSynthetic(submission))
            .orElse(false))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> hasUserLearnedGesturesPref.get().set(true));
  }

  @Override
  public void onPageAboutToCollapse(long collapseAnimDuration) {
    Keyboards.hide(getContext(), commentRecyclerView);
  }

  @Override
  public void onPageCollapsed() {
    contentVideoViewHolder.get().resetPlayback();

    commentListParentSheet.scrollTo(0);
    commentListParentSheet.setScrollingEnabled(false);
    commentListParentSheet.setMaxScrollY(0);

    submissionStream.accept(Optional.empty());
    contentLinkStream.accept(Optional.empty());

    // Bug workaround: Setting of adapter fails occasionally because the number
    // of rows do not match before and after state restoration, resulting in
    // submissions never loading anymore. I'm yet to understand why, but this
    // will ensure that this only happens until a submission is collapsed.
    commentRowCountBeforeActivityDestroy = -1;
  }

  public SubmissionPageLifecycleStreams lifecycle() {
    return lifecycleStreams;
  }
}
