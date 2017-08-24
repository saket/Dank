package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Commons.findOptimizedImage;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.doOnSingleStartAndTerminate;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.RedditLink;
import me.saket.dank.data.StatusBarTint;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.authentication.LoginActivity;
import me.saket.dank.ui.subreddits.SubmissionSwipeActionsProvider;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Function0;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.MediaHostRepository;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SlideDownAlphaAnimator;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.KeyboardVisibilityDetector.KeyboardVisibilityChangeEvent;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends DankFragment implements ExpandablePageLayout.StateChangeCallbacks,
    ExpandablePageLayout.OnPullToCollapseIntercepter
{

  private static final String KEY_SUBMISSION_JSON = "submissionJson";
  private static final String KEY_SUBMISSION_REQUEST = "submissionRequest";
  private static final long COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION = 250;
  private static final long ACTIVITY_CONTENT_RESIZE_ANIM_DURATION = 300;

  @BindView(R.id.submission_toolbar) View toolbar;
  @BindView(R.id.submission_toolbar_close) ImageButton toolbarCloseButton;
  @BindView(R.id.submission_toolbar_background) AnimatedToolbarBackground toolbarBackground;
  @BindView(R.id.submission_content_progress_bar) SubmissionAnimatedProgressBar contentLoadProgressView;
  @BindView(R.id.submission_image) ZoomableImageView contentImageView;
  @BindView(R.id.submission_video_container) ViewGroup contentVideoViewContainer;
  @BindView(R.id.submission_video) VideoView contentVideoView;
  @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
  @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
  @BindView(R.id.submission_byline) TextView submissionBylineView;
  @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
  @BindView(R.id.submission_link_container) ViewGroup linkDetailsView;
  @BindView(R.id.submission_comment_list) RecyclerView commentList;
  @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;
  @BindView(R.id.submission_reply) FloatingActionButton replyFAB;

  @BindDrawable(R.drawable.ic_toolbar_close_24dp) Drawable closeIconDrawable;
  @BindDimen(R.dimen.submission_commentssheet_minimum_visible_height) int commentsSheetMinimumVisibleHeight;

  @Inject MediaHostRepository mediaHostRepository;

  private ExpandablePageLayout submissionPageLayout;
  private CommentsAdapter commentsAdapter;
  private SubmissionAdapterWithHeader adapterWithSubmissionHeader;
  private CompositeDisposable onCollapseSubscriptions = new CompositeDisposable();
  private CommentTreeConstructor commentTreeConstructor;
  private DankSubmissionRequest activeSubmissionRequest;
  private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();
  private Link activeSubmissionContentLink;
  private BehaviorRelay<Submission> submissionChangeStream = BehaviorRelay.create();
  private Relay<Link> submissionContentStream = PublishRelay.create();
  private BehaviorRelay<KeyboardVisibilityChangeEvent> keyboardVisibilityChangeStream = BehaviorRelay.create();
  private Relay<PublicContribution> inlineReplyStream = PublishRelay.create();
  private SubmissionVideoHolder contentVideoViewHolder;
  private SubmissionImageHolder contentImageViewHolder;
  private SubmissionLinkHolder linkDetailsViewHolder;
  private int deviceDisplayWidth, deviceDisplayHeight;
  private boolean isCommentSheetBeneathImage;
  private Relay<List<SubmissionCommentRow>> commentsAdapterDatasetUpdatesStream = PublishRelay.create();

  public interface Callbacks {

    void onClickSubmissionToolbarUp();
  }

  public static SubmissionFragment create() {
    return new SubmissionFragment();
  }

  @Override
  public void onAttach(Context context) {
    Dank.dependencyInjector().inject(this);
    super.onAttach(context);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
    ButterKnife.bind(this, fragmentLayout);

    // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
    deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
    deviceDisplayHeight = getResources().getDisplayMetrics().heightPixels;

    return fragmentLayout;
  }

  @Override
  public void onViewCreated(View fragmentLayout, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(fragmentLayout, savedInstanceState);

    executeOnMeasure(toolbar, () -> setHeight(toolbarBackground, toolbar.getHeight()));
    toolbarCloseButton.setOnClickListener(v -> ((Callbacks) getActivity()).onClickSubmissionToolbarUp());

    DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
    linkMovementMethod.setOnLinkClickListener((textView, url) -> {
      Link parsedLink = UrlParser.parse(url);
      Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();

      if (parsedLink instanceof RedditLink.User) {
        UrlRouter.openUserProfilePopup(((RedditLink.User) parsedLink), textView, clickedUrlCoordinates);
      } else {
        UrlRouter.resolveAndOpen(parsedLink, getActivity(), clickedUrlCoordinates);
      }
      return true;
    });
    selfPostTextView.setMovementMethod(linkMovementMethod);

    submissionPageLayout = ((ExpandablePageLayout) fragmentLayout.getParent());
    submissionPageLayout.addStateChangeCallbacks(this);
    submissionPageLayout.setPullToCollapseIntercepter(this);

    unsubscribeOnDestroy(
        Keyboards
            .streamKeyboardVisibilityChanges(getActivity(), Views.statusBarHeight(getResources()))
            .subscribe(keyboardVisibilityChangeStream)
    );

    setupCommentList(linkMovementMethod);
    setupCommentTreeConstructor();
    setupContentImageView(fragmentLayout);
    setupContentVideoView();
    setupCommentsSheet();
    setupStatusBarTint();
    setupReplyFAB();
    setupSoftInputModeChangesAnimation();

    linkDetailsViewHolder = new SubmissionLinkHolder(linkDetailsView, submissionPageLayout);
    linkDetailsView.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
    linkDetailsViewHolder.titleSubtitleContainer.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

    // Restore submission if the Activity was recreated.
    if (savedInstanceState != null) {
      onRestoreSavedInstanceState(savedInstanceState);
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (submissionChangeStream.getValue() != null) {
      outState.putString(KEY_SUBMISSION_JSON, Dank.jackson().toJson(submissionChangeStream.getValue()));
      outState.putParcelable(KEY_SUBMISSION_REQUEST, activeSubmissionRequest);
    }
    super.onSaveInstanceState(outState);
  }

  private void onRestoreSavedInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState.containsKey(KEY_SUBMISSION_JSON)) {
      JsonNode jsonNode = Dank.jackson().parseJsonNode(savedInstanceState.getString(KEY_SUBMISSION_JSON));
      if (jsonNode != null) {
        populateUi(new Submission(jsonNode), savedInstanceState.getParcelable(KEY_SUBMISSION_REQUEST));
      }
    }
  }

  private void setupCommentList(DankLinkMovementMethod linkMovementMethod) {
    // Swipe gestures.
    OnLoginRequireListener onLoginRequireListener = () -> LoginActivity.startForResult(getActivity(), SubredditActivity.REQUEST_CODE_LOGIN);
    SubmissionSwipeActionsProvider submissionSwipeActionsProvider = new SubmissionSwipeActionsProvider(
        Dank.submissions(),
        Dank.voting(),
        Dank.userSession(),
        onLoginRequireListener
    );
    CommentSwipeActionsProvider commentSwipeActionsProvider = new CommentSwipeActionsProvider(
        Dank.voting(),
        Dank.userSession(),
        onLoginRequireListener
    );
    commentSwipeActionsProvider.setOnReplySwipeActionListener(parentComment -> {
      if (commentTreeConstructor.isCollapsed(parentComment) || !commentTreeConstructor.isReplyActiveFor(parentComment)) {
        commentTreeConstructor.showReplyAndExpandComments(parentComment);
        inlineReplyStream.accept(parentComment);
      } else {
        commentTreeConstructor.hideReply(parentComment);
      }
    });
    commentList.addOnItemTouchListener(new RecyclerSwipeListener(commentList));

    commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
    int commentItemViewElevation = getResources().getDimensionPixelSize(R.dimen.submission_comment_elevation);
    SlideDownAlphaAnimator itemAnimator = new SlideDownAlphaAnimator(commentItemViewElevation).withInterpolator(Animations.INTERPOLATOR);
    itemAnimator.setRemoveDuration(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION);
    itemAnimator.setAddDuration(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION);
    commentList.setItemAnimator(itemAnimator);

    // Add submission Views as a header so that it scrolls with the list.
    commentsAdapter = new CommentsAdapter(linkMovementMethod, Dank.voting(), Dank.userSession(), Dank.comments(), commentSwipeActionsProvider);
    adapterWithSubmissionHeader = SubmissionAdapterWithHeader.wrap(
        commentsAdapter,
        commentsHeaderView,
        Dank.voting(),
        Dank.comments(),
        submissionSwipeActionsProvider
    );
    commentList.setAdapter(adapterWithSubmissionHeader);

    // Inline reply additions.
    // Wait till the reply's View is added to the list and show keyboard.
    unsubscribeOnDestroy(
        inlineReplyStream
            .switchMap(parentContribution -> scrollToNewlyAddedReplyIfHidden(parentContribution))
            .switchMap(parentContribution -> showKeyboardWhenReplyIsVisible(parentContribution))
            .subscribe()
    );

    // Reply discards.
    unsubscribeOnDestroy(
        commentsAdapter.streamReplyDiscardClicks()
            .subscribe(discardEvent -> {
              Keyboards.hide(getActivity(), commentList);
              commentTreeConstructor.hideReply(discardEvent.parentContribution());
            })
    );

    // Reply fullscreen clicks.
    unsubscribeOnDestroy(
        commentsAdapter.streamReplyFullscreenClicks().subscribe(fullscreenClickEvent -> {
          // TODO.
        })
    );

    // Reply sends.
    unsubscribeOnDestroy(
        commentsAdapter.streamReplySendClicks().subscribe(sendClickEvent -> {
          // Message sending is not a part of the chain so that it does not get unsubscribed on destroy.
          Dank.comments().removeDraft(sendClickEvent.parentContribution())
              .andThen(Dank.reddit().withAuth(Dank.comments().sendReply(
                  sendClickEvent.parentContribution(),
                  submissionChangeStream.getValue().getFullName(),
                  sendClickEvent.replyMessage()))
              )
              .doOnSubscribe(o -> {
                Keyboards.hide(getActivity(), commentList);
                commentTreeConstructor.hideReply(sendClickEvent.parentContribution());
              })
              .compose(applySchedulersCompletable())
              .subscribe(doNothingCompletable(), error -> RetryReplyJobService.scheduleRetry(getActivity()));
        })
    );

    // Reply retry-sends.
    unsubscribeOnDestroy(
        commentsAdapter.streamReplyRetrySendClicks().subscribe(retrySendEvent -> {
          // Re-sending is not a part of the chain so that it does not get unsubscribed on destroy.
          Dank.reddit().withAuth(Dank.comments().reSendReply(retrySendEvent.failedPendingSyncReply()))
              .compose(applySchedulersCompletable())
              .subscribe(doNothingCompletable(), error -> RetryReplyJobService.scheduleRetry(getActivity()));
        })
    );

    // Bottom-spacing for FAB.
    Views.executeOnMeasure(replyFAB, () -> {
      int spaceForFab = replyFAB.getHeight() + ((ViewGroup.MarginLayoutParams) replyFAB.getLayoutParams()).bottomMargin * 2;
      Views.setPaddingBottom(commentList, spaceForFab);
    });
  }

  /**
   * Scroll to <var>parentContribution</var>'s reply if it's not going to be visible because it's located beyond the visible window.
   */
  @CheckResult
  private Observable<PublicContribution> scrollToNewlyAddedReplyIfHidden(PublicContribution parentContribution) {
    if (submissionChangeStream.getValue() == parentContribution) {
      // Submission reply.
      return Observable.just(parentContribution).doOnNext(o -> commentList.smoothScrollToPosition(1));
    }

    return commentsAdapterDatasetUpdatesStream
        .take(1)
        .map(newItems -> {
          int replyPosition = -1;
          for (int i = 0; i < newItems.size(); i++) {
            // Find the reply item's position.
            SubmissionCommentRow commentRow = newItems.get(i);
            if (commentRow instanceof CommentInlineReplyItem) {
              if (((CommentInlineReplyItem) commentRow).parentContribution() == parentContribution) {
                replyPosition = i + adapterWithSubmissionHeader.getVisibleHeaderItemCount();
                break;
              }
            }
          }
          return replyPosition;
        })
        .doOnNext(replyPosition -> {
          RecyclerView.ViewHolder parentContributionItemVH = commentList.findViewHolderForAdapterPosition(replyPosition - 1);
          int parentContributionBottom = parentContributionItemVH.itemView.getBottom() + commentListParentSheet.getTop();
          boolean willReplyBeHidden = parentContributionBottom >= submissionPageLayout.getBottom();
          if (willReplyBeHidden) {
            int dy = parentContributionItemVH.itemView.getHeight();
            commentList.smoothScrollBy(0, dy);
          }
        })
        .map(o -> parentContribution);
  }

  /**
   * Wait for <var>parentContribution</var>'s reply View to bind and show keyboard once it's visible.
   */
  @CheckResult
  private Observable<CommentsAdapter.ReplyItemViewBindEvent> showKeyboardWhenReplyIsVisible(PublicContribution parentContribution) {
    return commentsAdapter.streamReplyItemViewBinds()
        .filter(replyBindEvent -> replyBindEvent.replyItem().parentContribution().getFullName().equals(parentContribution.getFullName()))
        .take(1)
        .delay(COMMENT_LIST_ITEM_CHANGE_ANIM_DURATION, TimeUnit.MILLISECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(replyBindEvent -> commentList.post(() -> Keyboards.show(replyBindEvent.replyField())));
  }

  /**
   * The direction of modifications/updates to comments is unidirectional. All mods are made on
   * {@link CommentTreeConstructor} and {@link CommentsAdapter} subscribes to its updates.
   */
  private void setupCommentTreeConstructor() {
    commentTreeConstructor = new CommentTreeConstructor();

    // Add pending-sync replies to the comment tree.
    unsubscribeOnDestroy(
        submissionChangeStream
            .filter(subm -> subm.getComments() != null)
            .observeOn(Schedulers.io())
            .switchMap(submissionWithComments ->
                Dank.comments().removeSyncPendingPostedRepliesForSubmission(submissionWithComments)
                    .andThen(Dank.comments().streamPendingSyncRepliesForSubmission(submissionWithComments))
                    .map(pendingSyncReplies -> Pair.create(submissionWithComments, pendingSyncReplies))
            )
            .subscribe(submissionRepliesPair ->
                commentTreeConstructor.setComments(submissionRepliesPair.first.getComments(), submissionRepliesPair.second)
            )
    );

    // Animate changes.
    Pair<List<SubmissionCommentRow>, DiffUtil.DiffResult> initialPair = Pair.create(Collections.emptyList(), null);
    unsubscribeOnDestroy(
        commentTreeConstructor.streamTreeUpdates()
            .toFlowable(BackpressureStrategy.LATEST)
            .scan(initialPair, (pair, next) -> {
              CommentsDiffCallback callback = new CommentsDiffCallback(pair.first, next);
              DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, true /* detectMoves */);
              return Pair.create(next, result);
            })
            .skip(1)  // Skip the initial empty value.
            .observeOn(mainThread())
            .subscribe(dataAndDiff -> {
              List<SubmissionCommentRow> newComments = dataAndDiff.first;
              commentsAdapter.updateData(newComments);
              commentsAdapterDatasetUpdatesStream.accept(newComments);

              DiffUtil.DiffResult commentsDiffResult = dataAndDiff.second;
              commentsDiffResult.dispatchUpdatesTo(commentsAdapter);
            }, logError("Error while diff-ing comments"))
    );

    // Comment clicks.
    unsubscribeOnDestroy(
        commentsAdapter.streamCommentCollapseExpandEvents().subscribe(clickEvent -> {
          if (clickEvent.willCollapseOnClick()) {
            int firstCompletelyVisiblePos = ((LinearLayoutManager) commentList.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
            boolean commentExtendsBeyondWindowTopEdge = firstCompletelyVisiblePos == -1 || clickEvent.commentRowPosition() < firstCompletelyVisiblePos;
            if (commentExtendsBeyondWindowTopEdge) {
              float viewTop = clickEvent.commentItemView().getY();
              commentList.smoothScrollBy(0, (int) viewTop);
            }
          }

          commentTreeConstructor.toggleCollapse(clickEvent.commentRow());
        })
    );

    // Load-more-comment clicks.
    unsubscribeOnDestroy(
        // Using an Rx chain ensures that multiple load-more-clicks are executed sequentially.
        commentsAdapter
            .streamLoadMoreCommentsClicks()
            .flatMap(loadMoreClickEvent -> {
              if (loadMoreClickEvent.parentCommentNode().isThreadContinuation()) {
                DankSubmissionRequest continueThreadRequest = activeSubmissionRequest.toBuilder()
                    .focusComment(loadMoreClickEvent.parentCommentNode().getComment().getId())
                    .build();
                Rect expandFromShape = Views.globalVisibleRect(loadMoreClickEvent.loadMoreItemView());
                expandFromShape.top = expandFromShape.bottom;   // Because only expanding from a line is supported so far.
                SubmissionFragmentActivity.start(getContext(), continueThreadRequest, expandFromShape);

                return Observable.empty();

              } else {
                return Observable.just(loadMoreClickEvent.parentCommentNode())
                    .observeOn(io())
                    .doOnNext(commentTreeConstructor.setMoreCommentsLoading(true))
                    .map(Dank.reddit().loadMoreComments())
                    .doOnNext(commentTreeConstructor.setMoreCommentsLoading(false));
              }
            })
            .subscribe(doNothing(), error -> {
              Timber.e(error, "Failed to load more comments");
              if (isAdded()) {
                Toast.makeText(getActivity(), R.string.submission_error_failed_to_load_more_comments, Toast.LENGTH_SHORT).show();
              }
            })
    );
  }

  private void setupContentImageView(View fragmentLayout) {
    // TODO: remove margin and set height manually.
    Views.setMarginBottom(contentImageView, commentsSheetMinimumVisibleHeight);
    contentImageViewHolder = new SubmissionImageHolder(fragmentLayout, contentLoadProgressView, submissionPageLayout, deviceDisplayWidth);
  }

  private void setupContentVideoView() {
    ExoPlayerManager exoPlayerManager = ExoPlayerManager.newInstance(this, contentVideoView);

    contentVideoViewHolder = new SubmissionVideoHolder(
        contentVideoView,
        commentListParentSheet,
        contentLoadProgressView,
        submissionPageLayout,
        exoPlayerManager,
        mediaHostRepository,
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
      // If the sheet cannot scroll up because the top-margin > sheet's peek distance, scroll it to 70%
      // of its height so that the user doesn't get confused upon not seeing the sheet scroll up.
      float mediaVisibleHeight = activeSubmissionContentLink.isImageOrGif()
          ? contentImageView.getVisibleZoomedImageHeight()
          : contentVideoViewContainer.getHeight();

      return (int) Math.min(
          commentListParentSheet.getHeight() * 8 / 10,
          mediaVisibleHeight - commentListParentSheet.getTop()
      );
    };

    // Toggle sheet's collapsed state on image click.
    contentImageView.setOnClickListener(v -> {
      commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.calculate());
    });
    // and on submission title click.
    commentsHeaderView.setOnClickListener(v -> {
      if (activeSubmissionContentLink instanceof MediaLink && commentListParentSheet.isAtMaxScrollY()) {
        commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.calculate());
      }
    });

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
        int minimumGapWithBottom = 0; // TODO.
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
      public void onStateReset(State oldState, State newState) {}
    });
    commentListParentSheet.addOnSheetScrollChangeListener(newScrollY -> {
      isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate();
    });
  }

  private void setupReplyFAB() {
    // Show the FAB while the keyboard is hidden and there's space available.
    Relay<Boolean> spaceAvailabilityChanges = BehaviorRelay.create();
    ScrollingRecyclerViewSheet.SheetScrollChangeListener sheetScrollChangeListener = sheetScrollY -> {
      float bylineBottom = submissionBylineView.getBottom() + sheetScrollY + commentListParentSheet.getTop();
      boolean fabHasSpaceAvailable = bylineBottom < replyFAB.getTop();
      spaceAvailabilityChanges.accept(fabHasSpaceAvailable);
    };
    commentListParentSheet.addOnSheetScrollChangeListener(sheetScrollChangeListener);
    commentListParentSheet.post(() ->
        sheetScrollChangeListener.onScrollChange(commentListParentSheet.currentScrollY())  // Initial value.
    );

    unsubscribeOnDestroy(
        submissionChangeStream
            .doOnNext(o -> replyFAB.show())
            .switchMap(o -> Observable.combineLatest(keyboardVisibilityChangeStream, spaceAvailabilityChanges,
                (keyboardVisibilityChangeEvent, spaceAvailable) -> !keyboardVisibilityChangeEvent.visible() && spaceAvailable)
            )
            .subscribe(canShowReplyFAB -> {
              if (canShowReplyFAB) {
                replyFAB.show();
              } else {
                replyFAB.hide();
              }
            })
    );

    replyFAB.setOnClickListener(o -> {
      if (!Dank.userSession().isUserLoggedIn()) {
        LoginActivity.startForResult(getActivity(), SubredditActivity.REQUEST_CODE_LOGIN);
        return;
      }

      int firstVisiblePosition = ((LinearLayoutManager) commentList.getLayoutManager()).findFirstVisibleItemPosition();
      boolean isSubmissionReplyVisible = firstVisiblePosition <= 1; // 1 == index of reply field.

      if (commentTreeConstructor.isReplyActiveFor(submissionChangeStream.getValue()) && isSubmissionReplyVisible) {
        // Hide reply only if it's visible. Otherwise the user won't understand why the
        // reply FAB did not do anything.
        commentTreeConstructor.hideReply(submissionChangeStream.getValue());
      } else {
        commentTreeConstructor.showReply(submissionChangeStream.getValue());
        inlineReplyStream.accept(submissionChangeStream.getValue());
      }
    });
  }

  private void setupSoftInputModeChangesAnimation() {
    unsubscribeOnDestroy(
        keyboardVisibilityChangeStream.subscribe(new Consumer<KeyboardVisibilityChangeEvent>() {
          private ValueAnimator heightAnimator;
          private ViewGroup contentViewGroup;

          @Override
          public void accept(@NonNull KeyboardVisibilityChangeEvent changeEvent) throws Exception {
            if (contentViewGroup == null) {
              contentViewGroup = (ViewGroup) getActivity().findViewById(Window.ID_ANDROID_CONTENT);
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
            heightAnimator.addUpdateListener(animation -> {
              Views.setHeight(contentViewGroup, (int) animation.getAnimatedValue());
            });
            heightAnimator.setInterpolator(Animations.INTERPOLATOR);
            heightAnimator.setDuration(ACTIVITY_CONTENT_RESIZE_ANIM_DURATION);
            heightAnimator.start();
          }
        })
    );
  }

  private void setupStatusBarTint() {
    int defaultStatusBarColor = ContextCompat.getColor(getActivity(), R.color.color_primary_dark);
    int statusBarHeight = Views.statusBarHeight(getResources());
    Observable<Bitmap> contentBitmapStream = Observable.merge(
        contentImageViewHolder.streamImageBitmaps(),
        contentVideoViewHolder.streamVideoFirstFrameBitmaps(statusBarHeight)
    );

    SubmissionStatusBarTintProvider statusBarTintProvider = new SubmissionStatusBarTintProvider(
        defaultStatusBarColor,
        statusBarHeight,
        deviceDisplayWidth
    );

    // Reset the toolbar icons' tint until the content is loaded.
    unsubscribeOnDestroy(
        submissionContentStream
            .subscribe(o -> toolbarCloseButton.setColorFilter(Color.WHITE))
    );

    // For images and videos.
    unsubscribeOnDestroy(
        statusBarTintProvider.streamStatusBarTintColor(contentBitmapStream, submissionPageLayout, commentListParentSheet)
            .delay(statusBarTint -> Observable.just(statusBarTint).delay(statusBarTint.delayedTransition() ? 100 : 0, TimeUnit.MILLISECONDS))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<StatusBarTint>() {
              public ValueAnimator tintChangeAnimator;

              @Override
              public void accept(StatusBarTint statusBarTint) throws Exception {
                if (tintChangeAnimator != null) {
                  tintChangeAnimator.cancel();
                }
                tintChangeAnimator = ValueAnimator.ofArgb(getActivity().getWindow().getStatusBarColor(), statusBarTint.color());
                tintChangeAnimator.addUpdateListener(animation -> getActivity().getWindow().setStatusBarColor((int) animation.getAnimatedValue()));
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
            }, logError("Wut?"))
    );
  }

  /**
   * Update the submission to be shown. Since this Fragment is retained by {@link SubredditActivity},
   * we only update the UI everytime a new submission is to be shown.
   *
   * @param submissionRequest used for loading the comments of this submission.
   */
  public void populateUi(Submission submission, DankSubmissionRequest submissionRequest) {
    activeSubmissionRequest = submissionRequest;
    submissionChangeStream.accept(submission);

    // Reset everything.
    commentListParentSheet.scrollTo(0);
    commentListParentSheet.setScrollingEnabled(false);
    commentsAdapter.updateDataAndNotifyDatasetChanged(null);  // Comment adapter crashes without this.

    // Update submission information. Everything that
    adapterWithSubmissionHeader.updateSubmission(submission);
    commentsAdapter.updateSubmissionAuthor(submission.getAuthor());

    // Load content
    Link contentLink = UrlParser.parse(submission.getUrl(), submission.getThumbnails());
    submissionContentStream.accept(contentLink);
    loadSubmissionContent(submission, contentLink);

    // Load new comments.
    commentTreeConstructor.setSubmission(submission);
    if (submission.getComments() == null) {
      unsubscribeOnCollapse(Dank.reddit().submission(activeSubmissionRequest)
          .flatMap(retryWithCorrectSortIfNeeded())
          .compose(applySchedulersSingle())
          .compose(doOnSingleStartAndTerminate(start -> commentsLoadProgressView.setVisibility(start ? View.VISIBLE : View.GONE)))
          .subscribe(submissionChangeStream, handleSubmissionLoadError())
      );

    } else {
      submissionChangeStream.accept(submission);
    }
  }

  /**
   * The aim is to always load comments in the sort mode suggested by a subreddit. In case we accidentally
   * load in the wrong mode (maybe because the submission's details were unknown), this function reloads
   * the submission's data using its suggested sort.
   */
  @NonNull
  private Function<Submission, Single<Submission>> retryWithCorrectSortIfNeeded() {
    return submWithComments -> {
      if (submWithComments.getSuggestedSort() != null && submWithComments.getSuggestedSort() != activeSubmissionRequest.commentSort()) {
        activeSubmissionRequest = activeSubmissionRequest.toBuilder()
            .commentSort(submWithComments.getSuggestedSort())
            .build();
        return Dank.reddit().submission(activeSubmissionRequest);

      } else {
        return Single.just(submWithComments);
      }
    };
  }

  public Consumer<Throwable> handleSubmissionLoadError() {
    return error -> Timber.e(error, error.getMessage());
  }

  private void loadSubmissionContent(Submission submission, Link contentLink) {
    activeSubmissionContentLink = contentLink;

//        Timber.d("-------------------------------------------");
//        Timber.i("%s", submission.getTitle());
//        Timber.i("Post hint: %s, URL: %s", submission.getPostHint(), submission.getUrl());
//        Timber.i("Parsed content: %s, type: %s", contentLink, contentLink.type());
//        if (submissionContent.type() == SubmissionContent.Type.IMAGE) {
//            Timber.i("Optimized image: %s", submissionContent.imageContentUrl(deviceDisplayWidth));
//        }

    boolean isImgurAlbum = contentLink instanceof MediaLink.ImgurAlbum;
    linkDetailsViewHolder.setVisible(!isImgurAlbum && contentLink.isExternal() || contentLink.isRedditHosted() && !submission.isSelfPost());
    selfPostTextView.setVisibility(submission.isSelfPost() ? View.VISIBLE : View.GONE);
    contentImageView.setVisibility(contentLink.isImageOrGif() ? View.VISIBLE : View.GONE);
    contentVideoViewContainer.setVisibility(contentLink.isVideo() ? View.VISIBLE : View.GONE);

    // Show shadows behind the toolbar because image/video submissions have a transparent toolbar.
    boolean transparentToolbar = contentLink.isImageOrGif() || contentLink.isVideo();
    toolbarBackground.setSyncScrollEnabled(transparentToolbar);

    if (contentLink instanceof MediaLink.ImgurUnresolvedGallery) {
      contentLoadProgressView.show();
      String redditSuppliedThumbnailUrl = findOptimizedImage(submission.getThumbnails(), linkDetailsViewHolder.getThumbnailWidthForAlbum());

      unsubscribeOnCollapse(
          mediaHostRepository.resolveActualLinkIfNeeded(((MediaLink) contentLink))
              .map(resolvedLink -> resolvedLink.setRedditSuppliedImages(submission.getThumbnails()))
              .map(resolvedLink -> {
                // Replace Imgur's cover image URL with reddit supplied URL, which will already be cached by Glide.
                if (resolvedLink instanceof MediaLink.ImgurAlbum && redditSuppliedThumbnailUrl != null) {
                  return ((MediaLink.ImgurAlbum) resolvedLink).withCoverImageUrl(redditSuppliedThumbnailUrl);
                }
                return resolvedLink;
              })
              .compose(applySchedulersSingle())
              .subscribe(
                  resolvedLink -> {
                    loadSubmissionContent(submission, resolvedLink);
                  },
                  error -> {
                    // Open this album in browser if Imgur rate limits have reached.
                    if (error instanceof ImgurApiRateLimitReachedException) {
                      String albumUrl = ((MediaLink.ImgurUnresolvedGallery) contentLink).albumUrl();
                      loadSubmissionContent(submission, Link.External.create(albumUrl));

                    } else {
                      // TODO: 05/04/17 Handle errors (including InvalidImgurAlbumException).
                      Toast.makeText(getContext(), "Couldn't load image", Toast.LENGTH_SHORT).show();
                      Timber.e(error, "Couldn't load album cover image");
                      contentLoadProgressView.hide();
                    }
                  }
              )
      );
      return;
    }

    switch (contentLink.type()) {
      case IMAGE_OR_GIF:
        contentImageViewHolder.load((MediaLink) contentLink);
        break;

      case REDDIT_HOSTED:
        if (submission.isSelfPost()) {
          contentLoadProgressView.hide();
          String selfTextHtml = submission.getDataNode().get("selftext_html").asText(submission.getSelftext() /* defaultValue */);
          CharSequence markdownHtml = Markdown.parseRedditMarkdownHtml(selfTextHtml, selfPostTextView.getPaint());
          selfPostTextView.setVisibility(markdownHtml.length() > 0 ? View.VISIBLE : View.GONE);
          selfPostTextView.setText(markdownHtml);

        } else {
          contentLoadProgressView.hide();
          //noinspection ConstantConditions
          unsubscribeOnCollapse(linkDetailsViewHolder.populate(((RedditLink) contentLink)));
          linkDetailsView.setOnClickListener(o -> UrlRouter.resolveAndOpen(contentLink, getContext()));
        }
        break;

      case EXTERNAL:
        contentLoadProgressView.hide();
        String redditSuppliedThumbnail = findOptimizedImage(
            submission.getThumbnails(),
            linkDetailsViewHolder.getThumbnailWidthForExternalLink()
        );
        linkDetailsView.setOnClickListener(o -> UrlRouter.resolveAndOpen(contentLink, getContext()));

        if (isImgurAlbum) {
          linkDetailsViewHolder.populate(((MediaLink.ImgurAlbum) contentLink), redditSuppliedThumbnail);
        } else {
          unsubscribeOnCollapse(linkDetailsViewHolder.populate(((Link.External) contentLink), redditSuppliedThumbnail));
        }
        break;

      case VIDEO:
        boolean loadHighQualityVideo = false; // TODO: Get this from user's data preferences.
        //noinspection ConstantConditions
        unsubscribeOnCollapse(
            contentVideoViewHolder.load((MediaLink) contentLink, loadHighQualityVideo)
        );
        break;

      default:
        throw new UnsupportedOperationException("Unknown content: " + contentLink);
    }
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
    for (Runnable runnable : pendingOnExpandRunnables) {
      runnable.run();
      pendingOnExpandRunnables.remove(runnable);
    }
  }

  @Override
  public void onPageAboutToCollapse(long collapseAnimDuration) {
    Keyboards.hide(getActivity(), commentList);
  }

  @Override
  public void onPageCollapsed() {
    contentVideoViewHolder.pausePlayback();
    onCollapseSubscriptions.clear();
    commentTreeConstructor.reset();
  }

  private void unsubscribeOnCollapse(Disposable subscription) {
    onCollapseSubscriptions.add(subscription);
    unsubscribeOnDestroy(subscription);
  }
}
