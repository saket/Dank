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
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.OnLoginRequireListener;
import me.saket.dank.data.RedditLink;
import me.saket.dank.data.exceptions.ImgurApiRateLimitReachedException;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.ui.OpenUrlActivity;
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
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SlideDownAlphaAnimator;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends DankFragment implements ExpandablePageLayout.StateChangeCallbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

  private static final String KEY_SUBMISSION_JSON = "submissionJson";
  private static final String KEY_SUBMISSION_REQUEST = "submissionRequest";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.submission_toolbar_background) AnimatedToolbarBackground toolbarBackground;
  @BindView(R.id.submission_content_progress_bar) SubmissionAnimatedProgressBar contentLoadProgressView;
  @BindView(R.id.submission_image) ZoomableImageView contentImageView;
  @BindView(R.id.submission_image_scroll_hint) View contentImageScrollHintView;
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

  private ExpandablePageLayout submissionPageLayout;
  private CommentsAdapter commentsAdapter;
  private SubmissionAdapterWithHeader adapterWithSubmissionHeader;
  private CompositeDisposable onCollapseSubscriptions = new CompositeDisposable();
  private CommentTreeConstructor commentTreeConstructor;
  private Submission activeSubmission;
  private DankSubmissionRequest activeSubmissionRequest;
  private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();
  private Link activeSubmissionContentLink;
  private Relay<Submission> submissionWithCommentsStream = PublishRelay.create();

  private SubmissionVideoHolder contentVideoViewHolder;
  private SubmissionImageHolder contentImageViewHolder;
  private SubmissionLinkHolder linkDetailsViewHolder;

  private int deviceDisplayWidth;
  private boolean isCommentSheetBeneathImage;

  public interface Callbacks {
    void onClickSubmissionToolbarUp();
  }

  public static SubmissionFragment create() {
    return new SubmissionFragment();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
    ButterKnife.bind(this, fragmentLayout);

    // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
    deviceDisplayWidth = fragmentLayout.getResources().getDisplayMetrics().widthPixels;

    return fragmentLayout;
  }

  @Override
  public void onViewCreated(View fragmentLayout, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(fragmentLayout, savedInstanceState);

    executeOnMeasure(toolbar, () -> setHeight(toolbarBackground, toolbar.getHeight()));
    toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onClickSubmissionToolbarUp());

    DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
    linkMovementMethod.setOnLinkClickListener((textView, url) -> {
      // TODO: 18/03/17 Remove try/catch block
      try {
        Link parsedLink = UrlParser.parse(url);
        Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();
        Rect clickedUrlCoordinatesRect = new Rect(0, clickedUrlCoordinates.y, deviceDisplayWidth, clickedUrlCoordinates.y);
        OpenUrlActivity.handle(getActivity(), parsedLink, clickedUrlCoordinatesRect);
        return true;

      } catch (Exception e) {
        Timber.i(e, "Couldn't parse URL: %s", url);
        return false;
      }
    });
    selfPostTextView.setMovementMethod(linkMovementMethod);

    submissionPageLayout = ((ExpandablePageLayout) fragmentLayout.getParent());
    submissionPageLayout.addStateChangeCallbacks(this);
    submissionPageLayout.setPullToCollapseIntercepter(this);

    setupCommentList(linkMovementMethod);
    setupCommentTreeConstructor();
    setupContentImageView(fragmentLayout);
    setupContentVideoView(fragmentLayout);
    setupCommentsSheet();
    setupReplyFAB();
    setupStatusBarTint();

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
    if (activeSubmission != null) {
      outState.putString(KEY_SUBMISSION_JSON, Dank.jackson().toJson(activeSubmission));
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
      } else {
        commentTreeConstructor.hideReply(parentComment);
      }
    });
    commentList.addOnItemTouchListener(new RecyclerSwipeListener(commentList));

    commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
    int commentItemViewElevation = getResources().getDimensionPixelSize(R.dimen.submission_comment_elevation);
    SlideDownAlphaAnimator itemAnimator = new SlideDownAlphaAnimator(commentItemViewElevation).withInterpolator(Animations.INTERPOLATOR);
    itemAnimator.setRemoveDuration(250);
    itemAnimator.setAddDuration(250);
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

    commentsAdapter.setReplyActionsListener(new CommentsAdapter.ReplyActionsListener() {
      @Override
      public void onClickDiscardReply(PublicContribution parentContribution) {
        commentTreeConstructor.hideReply(parentContribution);
      }

      @Override
      public void onClickEditReplyInFullscreenMode(PublicContribution parentContribution) {
        // TODO.
      }

      @Override
      public void onClickSendReply(PublicContribution parentContribution, String replyMessage) {
        Dank.comments().removeDraft(parentContribution)
            .andThen(Dank.reddit().withAuth(Dank.comments().sendReply(parentContribution, activeSubmission.getFullName(), replyMessage)))
            .doOnSubscribe(o -> commentTreeConstructor.hideReply(parentContribution))
            .compose(applySchedulersCompletable())
            .subscribe(doNothingCompletable(), error -> RetryReplyJobService.scheduleRetry(getActivity()));
      }

      @Override
      public void onClickRetrySendingReply(PendingSyncReply pendingSyncReply) {
        Dank.reddit().withAuth(Dank.comments().reSendReply(pendingSyncReply))
            .compose(applySchedulersCompletable())
            .subscribe(doNothingCompletable(), error -> RetryReplyJobService.scheduleRetry(getActivity()));
      }
    });
  }

  /**
   * The direction of modifications/updates to comments is unidirectional. All mods are made on
   * {@link CommentTreeConstructor} and {@link CommentsAdapter} subscribes to its updates.
   */
  private void setupCommentTreeConstructor() {
    commentTreeConstructor = new CommentTreeConstructor();

    // Add pending-sync replies to the comment tree.
    unsubscribeOnDestroy(
        submissionWithCommentsStream
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
    Views.setMarginBottom(contentImageView, commentsSheetMinimumVisibleHeight);
    contentImageViewHolder = new SubmissionImageHolder(fragmentLayout, contentLoadProgressView, submissionPageLayout, deviceDisplayWidth);
  }

  private void setupContentVideoView(View fragmentLayout) {
    Views.setMarginBottom(contentVideoViewContainer, commentsSheetMinimumVisibleHeight);
    ExoPlayerManager exoPlayerManager = ExoPlayerManager.newInstance(this, contentVideoView);
    contentVideoViewHolder = new SubmissionVideoHolder(fragmentLayout, contentLoadProgressView, submissionPageLayout, exoPlayerManager);
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
      if (activeSubmissionContentLink instanceof MediaLink && commentListParentSheet.isAtPeekHeightState()) {
        commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.calculate());
      }
    });

    // Calculates if the top of the comment sheet is directly below the image.
    Function0<Boolean> isCommentSheetBeneathImageFunc = () -> {
      //noinspection CodeBlock2Expr
      return (int) commentListParentSheet.getY() == (int) contentImageView.getVisibleZoomedImageHeight();
    };

    // Keep the comments sheet always beneath the image.
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

        int boundedVisibleImageHeight = (int) Math.min(contentImageView.getHeight(), contentImageView.getVisibleZoomedImageHeight());
        int imageRevealDistance = boundedVisibleImageHeight - commentListParentSheet.getTop();
        commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - imageRevealDistance);

        if (isCommentSheetBeneathImage
            // This is a hacky workaround: when zooming out, the received callbacks are very discrete and
            // it becomes difficult to lock the comments sheet beneath the image.
            || (isZoomingOut && contentImageView.getVisibleZoomedImageHeight() <= commentListParentSheet.getY()))
        {
          commentListParentSheet.scrollTo(imageRevealDistance);
        }
        isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate();
      }

      @Override
      public void onStateReset(State oldState, State newState) {

      }
    });

    commentListParentSheet.addOnSheetScrollChangeListener(newScrollY -> {
      isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.calculate();
    });
  }

  private void setupReplyFAB() {
    // Show only if it'll not overlap the submission byline.
    commentListParentSheet.addOnSheetScrollChangeListener(sheetScrollY -> {
      float bylineBottom = submissionBylineView.getBottom() + sheetScrollY + commentListParentSheet.getTop();
      if (bylineBottom >= replyFAB.getTop()) {
        replyFAB.hide();
      } else {
        replyFAB.show();
      }
    });

    replyFAB.setOnClickListener(o -> {
      if (!Dank.userSession().isUserLoggedIn()) {
        LoginActivity.startForResult(getActivity(), SubredditActivity.REQUEST_CODE_LOGIN);
        return;
      }

      int firstVisiblePosition = ((LinearLayoutManager) commentList.getLayoutManager()).findFirstVisibleItemPosition();
      boolean isSubmissionReplyVisible = firstVisiblePosition <= 1; // 1 == index of reply field.

      if (commentTreeConstructor.isReplyActiveFor(activeSubmission) && isSubmissionReplyVisible) {
        // Hide reply only if it's visible. Otherwise the user won't understand why the
        // reply FAB did not do anything.
        commentTreeConstructor.hideReply(activeSubmission);
      } else {
        commentList.smoothScrollToPosition(0);
        commentTreeConstructor.showReply(activeSubmission);
      }
    });
  }

  private void setupStatusBarTint() {
    Observable<Bitmap> contentBitmapStream = contentImageViewHolder.streamImageBitmaps();
    int defaultStatusBarColor = ContextCompat.getColor(getActivity(), R.color.color_primary_dark);
    int statusBarHeight = Views.statusBarHeight(getResources());
    SubmissionStatusBarTintProvider statusBarTintProvider = new SubmissionStatusBarTintProvider(defaultStatusBarColor, statusBarHeight);

    unsubscribeOnDestroy(
        statusBarTintProvider.streamStatusBarTintColor(contentBitmapStream, submissionPageLayout, commentListParentSheet)
            .delay(statusBarTint -> Observable.just(statusBarTint).delay(statusBarTint.delayedTransition() ? 100 : 0, TimeUnit.MILLISECONDS))
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(statusBarTint -> {
              ValueAnimator tintChangeAnim = ValueAnimator.ofArgb(getActivity().getWindow().getStatusBarColor(), statusBarTint.color());
              tintChangeAnim.addUpdateListener(animation -> getActivity().getWindow().setStatusBarColor((int) animation.getAnimatedValue()));
              tintChangeAnim.setDuration(150L);
              tintChangeAnim.setInterpolator(Animations.INTERPOLATOR);
              tintChangeAnim.start();

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

              if (!statusBarTint.isDarkColor()) {
                // TODO: Use darker colors on light images.
                //back.setColorFilter(ContextCompat.getColor(DribbbleShot.this, R.color.dark_icon));
              }
            })
    );
  }

  /**
   * Update the submission to be shown. Since this Fragment is retained by {@link SubredditActivity},
   * we only update the UI everytime a new submission is to be shown.
   *
   * @param submissionRequest used for loading the comments of this submission.
   */
  public void populateUi(Submission submission, DankSubmissionRequest submissionRequest) {
    activeSubmission = submission;
    activeSubmissionRequest = submissionRequest;

    // Reset everything.
    commentListParentSheet.scrollTo(0);
    commentListParentSheet.setScrollingEnabled(false);
    commentTreeConstructor.reset();
    commentsAdapter.updateDataAndNotifyDatasetChanged(null);
    replyFAB.show();

    // Update submission information. Everything that
    adapterWithSubmissionHeader.updateSubmission(submission);
    commentsAdapter.updateSubmissionAuthor(submission.getAuthor());

    // Load content
    Link contentLink = UrlParser.parse(submission.getUrl(), submission.getThumbnails());
    loadSubmissionContent(submission, contentLink);

    // Load new comments.
    commentTreeConstructor.setSubmission(submission);
    if (submission.getComments() == null) {
      unsubscribeOnCollapse(Dank.reddit().submission(activeSubmissionRequest)
          .flatMap(retryWithCorrectSortIfNeeded())
          .compose(applySchedulersSingle())
          .compose(doOnSingleStartAndTerminate(start -> commentsLoadProgressView.setVisibility(start ? View.VISIBLE : View.GONE)))
          .doOnSuccess(submissionWithComments -> activeSubmission = submissionWithComments)
          .subscribe(submissionWithCommentsStream, handleSubmissionLoadError())
      );

    } else {
      submissionWithCommentsStream.accept(submission);
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
      String redditSuppliedThumbnail = findOptimizedImage(activeSubmission.getThumbnails(), linkDetailsViewHolder.getThumbnailWidthForAlbum());

      unsubscribeOnCollapse(Dank.imgur()
          .gallery((MediaLink.ImgurUnresolvedGallery) contentLink)
          .compose(applySchedulersSingle())
          .subscribe(imgurResponse -> {
            if (imgurResponse.isAlbum()) {
              String coverImageUrl;
              if (redditSuppliedThumbnail != null) {
                coverImageUrl = redditSuppliedThumbnail;
              } else {
                coverImageUrl = imgurResponse.images().get(0).url();
              }

              String albumUrl = ((MediaLink.ImgurUnresolvedGallery) contentLink).albumUrl();
              int imageCount = imgurResponse.images().size();
              MediaLink.ImgurAlbum albumLink = MediaLink.ImgurAlbum.create(albumUrl, imgurResponse.albumTitle(), coverImageUrl, imageCount);
              loadSubmissionContent(submission, albumLink);

            } else {
              Link coverImageLink = UrlParser.parse(imgurResponse.images().get(0).url(), submission.getThumbnails());
              loadSubmissionContent(submission, coverImageLink);
            }

          }, error -> {
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
          }));
      return;
    }

    switch (contentLink.type()) {
      case IMAGE_OR_GIF:
        contentImageViewHolder.load((MediaLink) contentLink);
        break;

      case REDDIT_HOSTED:
        if (submission.isSelfPost()) {
          contentLoadProgressView.hide();
          String selfTextHtml = submission.getDataNode().get("selftext_html").asText("");
          CharSequence markdownHtml = Markdown.parseRedditMarkdownHtml(selfTextHtml, selfPostTextView.getPaint());
          selfPostTextView.setVisibility(markdownHtml.length() > 0 ? View.VISIBLE : View.GONE);
          selfPostTextView.setText(markdownHtml);

        } else {
          contentLoadProgressView.hide();
          //noinspection ConstantConditions
          unsubscribeOnCollapse(linkDetailsViewHolder.populate(((RedditLink) contentLink)));
          linkDetailsView.setOnClickListener(__ -> OpenUrlActivity.handle(getContext(), contentLink, null));
        }
        break;

      case EXTERNAL:
        contentLoadProgressView.hide();
        String redditSuppliedThumbnail = findOptimizedImage(
            activeSubmission.getThumbnails(),
            linkDetailsViewHolder.thumbnailWidthForExternalLink()
        );
        linkDetailsView.setOnClickListener(__ -> OpenUrlActivity.handle(getContext(), contentLink, null));

        if (isImgurAlbum) {
          linkDetailsViewHolder.populate(((MediaLink.ImgurAlbum) contentLink), redditSuppliedThumbnail);
        } else {
          unsubscribeOnCollapse(linkDetailsViewHolder.populate(((Link.External) contentLink), redditSuppliedThumbnail));
        }
        break;

      case VIDEO:
        unsubscribeOnCollapse(contentVideoViewHolder.load((MediaLink) contentLink));
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
      return touchLiesOn(contentImageView, downX, downY) && contentImageView.canPanVertically(!upwardPagePull);
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
  }

  private void unsubscribeOnCollapse(Disposable subscription) {
    onCollapseSubscriptions.add(subscription);
    unsubscribeOnDestroy(subscription);
  }
}
