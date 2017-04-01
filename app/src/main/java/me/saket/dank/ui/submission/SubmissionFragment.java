package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.Views.executeOnMeasure;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.statusBarHeight;
import static me.saket.dank.utils.Views.touchLiesOn;
import static rx.Observable.just;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.Submission;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.RedditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.ui.OpenUrlActivity;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.DankUrlParser;
import me.saket.dank.utils.ExoPlayerManager;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import me.saket.dank.widgets.ZoomableImageView;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends DankFragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    private static final String KEY_SUBMISSION_JSON = "submissionJson";
    private static final String KEY_SUBMISSION_REQUEST = "submissionRequest";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_toolbar_shadow) View toolbarShadows;
    @BindView(R.id.submission_toolbar_background) AnimatedToolbarBackground toolbarBackground;
    @BindView(R.id.submission_content_progress) SubmissionAnimatedProgressBar contentLoadProgressView;
    @BindView(R.id.submission_image) ZoomableImageView contentImageView;
    @BindView(R.id.submission_video) VideoView contentVideoView;
    @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
    @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
    @BindView(R.id.submission_link_container) ViewGroup linkDetailsView;
    @BindView(R.id.submission_comment_list) RecyclerView commentList;
    @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;

    @BindDrawable(R.drawable.ic_toolbar_close_24dp) Drawable closeIconDrawable;
    @BindDimen(R.dimen.submission_commentssheet_minimum_visible_height) int commentsSheetMinimumVisibleHeight;

    private ExpandablePageLayout submissionPageLayout;
    private CommentsAdapter commentsAdapter;
    private CompositeSubscription onCollapseSubscriptions = new CompositeSubscription();
    private CommentsHelper commentsHelper;
    private Submission activeSubmission;
    private DankSubmissionRequest activeSubmissionRequest;
    private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();
    private SubmissionLinkDetailsViewHolder linkDetailsViewHolder;
    private Link activeSubmissionContentLink;

    private SubmissionVideoViewHolder contentVideoViewHolder;
    private SubmissionImageViewHolder contentImageViewHolder;

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

        int statusBarHeight = statusBarHeight(getResources());
        setMarginTop(toolbar, statusBarHeight);
        executeOnMeasure(toolbar, () -> setHeight(toolbarBackground, toolbar.getHeight() + statusBarHeight));
        toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onClickSubmissionToolbarUp());

        DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
        linkMovementMethod.setOnLinkClickListener((textView, url) -> {
            // TODO: 18/03/17 Remove try/catch block
            try {
                Link parsedLink = DankUrlParser.parse(url);
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

        // TODO: 01/02/17 Should we preload Views for adapter rows?
        // Setup comment list and its adapter.
        commentsAdapter = new CommentsAdapter(getResources(), linkMovementMethod);
        commentList.setAdapter(RecyclerAdapterWithHeader.wrap(commentsAdapter, commentsHeaderView));
        commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
        commentList.setItemAnimator(new DefaultItemAnimator());

        // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
        deviceDisplayWidth = fragmentLayout.getResources().getDisplayMetrics().widthPixels;

        linkDetailsViewHolder = new SubmissionLinkDetailsViewHolder(linkDetailsView);
        return fragmentLayout;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        submissionPageLayout = ((ExpandablePageLayout) view.getParent());
        submissionPageLayout.addCallbacks(this);
        submissionPageLayout.setPullToCollapseIntercepter(this);

        setupCommentsHelper();
        setupContentImageView();
        setupContentVideoView();
        setupCommentsSheet();

        // Restore submission if the Activity was recreated.
        if (savedInstanceState != null) {
            onRestoreSavedInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onDestroy() {
        onCollapseSubscriptions.clear();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (activeSubmission != null) {
            JsonNode dataNode = activeSubmission.getDataNode();
            outState.putString(KEY_SUBMISSION_JSON, dataNode.toString());
            outState.putParcelable(KEY_SUBMISSION_REQUEST, activeSubmissionRequest);
        }
        super.onSaveInstanceState(outState);
    }

    private void onRestoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_SUBMISSION_JSON)) {
            String submissionJson = savedInstanceState.getString(KEY_SUBMISSION_JSON);
            try {
                JsonNode jsonNode = new ObjectMapper().readTree(submissionJson);
                populateUi(new Submission(jsonNode), savedInstanceState.getParcelable(KEY_SUBMISSION_REQUEST));

            } catch (Exception e) {
                Timber.e(e, "Couldn't deserialize Submission: %s", submissionJson);
            }
        }
    }

    /**
     * {@link CommentsHelper} helps in collapsing comments and helping {@link CommentsAdapter} in indicating
     * progress when more comments are being fetched for a CommentNode.
     * <p>
     * The direction of modifications/updates to comments is unidirectional. All mods are made on
     * {@link CommentsHelper} and {@link CommentsAdapter} subscribes to its updates.
     */
    private void setupCommentsHelper() {
        commentsHelper = new CommentsHelper();

        unsubscribeOnDestroy(
                commentsHelper.updates().observeOn(mainThread()).subscribe(commentsAdapter)
        );

        // Comment clicks.
        unsubscribeOnDestroy(
                commentsAdapter.commentClicks().subscribe(commentsHelper.toggleCollapse())
        );

        // Load-more-comment clicks.
        unsubscribeOnDestroy(
                // Using an Rx chain ensures that multiple load-more-clicks are executed sequentially.
                commentsAdapter
                        .loadMoreCommentsClicks()
                        .flatMap(loadMoreClickEvent -> {
                            if (loadMoreClickEvent.parentCommentNode.isThreadContinuation()) {
                                DankSubmissionRequest continueThreadRequest = activeSubmissionRequest.toBuilder()
                                        .focusComment(loadMoreClickEvent.parentCommentNode.getComment().getId())
                                        .build();
                                Rect expandFromShape = Views.globalVisibleRect(loadMoreClickEvent.loadMoreItemView);
                                expandFromShape.top = expandFromShape.bottom;   // Because only expanding from a line is supported so far.
                                SubmissionFragmentActivity.start(getContext(), continueThreadRequest, expandFromShape);

                                return Observable.empty();

                            } else {
                                return Observable.just(loadMoreClickEvent.parentCommentNode)
                                        .observeOn(io())
                                        .doOnNext(commentsHelper.setMoreCommentsLoading(true))
                                        .map(Dank.reddit().loadMoreComments())
                                        .doOnNext(commentsHelper.setMoreCommentsLoading(false));
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

    private void setupContentImageView() {
        Views.setMarginBottom(contentImageView, commentsSheetMinimumVisibleHeight);

        contentImageViewHolder = new SubmissionImageViewHolder(
                submissionPageLayout,
                contentLoadProgressView,
                contentImageView,
                commentListParentSheet,
                deviceDisplayWidth
        );
        contentImageViewHolder.setup();
    }

    private void setupContentVideoView() {
        Views.setMarginBottom(contentVideoView, commentsSheetMinimumVisibleHeight);

        contentVideoViewHolder = new SubmissionVideoViewHolder(
                submissionPageLayout,
                contentLoadProgressView,
                contentVideoView,
                commentListParentSheet,
                ExoPlayerManager.newInstance(this, contentVideoView)
        );
    }

    private void setupCommentsSheet() {
        toolbarBackground.syncPositionWithSheet(commentListParentSheet);
        contentLoadProgressView.syncPositionWithSheet(commentListParentSheet);
        commentListParentSheet.setScrollingEnabled(false);

        Func1<?, Integer> mediaRevealDistanceFunc = __ -> {
            // If the sheet cannot scroll up because the top-margin > sheet's peek distance, scroll it to 70%
            // of its height so that the user doesn't get confused upon not seeing the sheet scroll up.
            float mediaVisibleHeight = activeSubmissionContentLink.isImageOrGif()
                    ? contentImageView.getVisibleZoomedImageHeight()
                    : contentVideoView.getHeight();

            return (int) Math.min(
                    commentListParentSheet.getHeight() * 8 / 10,
                    mediaVisibleHeight - commentListParentSheet.getTop()
            );
        };

        // Toggle sheet's collapsed state on image click.
        contentImageView.setOnClickListener(v -> {
            commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.call(null));
        });

        // and on submission title click.
        commentsHeaderView.setOnClickListener(v -> {
            if (activeSubmissionContentLink instanceof MediaLink && commentListParentSheet.isAtPeekHeightState()) {
                commentListParentSheet.smoothScrollTo(mediaRevealDistanceFunc.call(null));
            }
        });

        // Calculates if the top of the comment sheet is directly below the image.
        Func0<Boolean> isCommentSheetBeneathImageFunc = () -> {
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
                isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.call();
            }

            @Override
            public void onStateReset(State oldState, State newState) {

            }
        });

        commentListParentSheet.addOnSheetScrollChangeListener(newScrollY -> {
            isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.call();
        });
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
        activeSubmissionContentLink = DankUrlParser.parse(submission);

        // Reset everything.
        contentLoadProgressView.setProgress(0);
        commentListParentSheet.scrollTo(0);
        commentListParentSheet.setScrollingEnabled(false);
        commentsHelper.reset();
        commentsAdapter.updateData(null);

        // Update submission information.
        //noinspection deprecation
        titleView.setText(Html.fromHtml(submission.getTitle()));
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));

        // Load self-text/media/webpage.
        loadSubmissionContent(submission, activeSubmissionContentLink);

        // Load new comments.
        if (submission.getComments() == null) {
            unsubscribeOnCollapse(
                    Dank.reddit()
                            .withAuth(Dank.reddit().submission(activeSubmissionRequest))
                            .flatMap(retryWithCorrectSortIfNeeded())
                            .compose(applySchedulers())
                            .compose(doOnStartAndFinish(start -> commentsLoadProgressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                            .doOnNext(submWithComments -> activeSubmission = submWithComments)
                            .subscribe(commentsHelper.setup(), handleSubmissionLoadError())
            );

        } else {
            commentsHelper.setup().call(submission);
        }
    }

    /**
     * The aim is to always load comments in the sort mode suggested by a subreddit. In case we accidentally
     * load in the wrong mode (maybe because the submission's details were unknown), this function reloads
     * the submission's data using its suggested sort.
     */
    @NonNull
    private Func1<Submission, Observable<Submission>> retryWithCorrectSortIfNeeded() {
        return submWithComments -> {
            if (submWithComments.getSuggestedSort() != null && submWithComments.getSuggestedSort() != activeSubmissionRequest.commentSort()) {
                activeSubmissionRequest = activeSubmissionRequest.toBuilder()
                        .commentSort(submWithComments.getSuggestedSort())
                        .build();
                return Dank.reddit().withAuth(Dank.reddit().submission(activeSubmissionRequest));

            } else {
                return just(submWithComments);
            }
        };
    }

    public Action1<Throwable> handleSubmissionLoadError() {
        return error -> Timber.e(error, error.getMessage());
    }

    private void loadSubmissionContent(Submission submission, Link contentLink) {
//        Timber.d("-------------------------------------------");
//        Timber.i("%s", submission.getTitle());
//        Timber.i("Post hint: %s, URL: %s", submission.getPostHint(), submission.getUrl());
//        Timber.i("Parsed content: %s, type: %s", contentLink, contentLink.type());
//        if (submissionContent.type() == SubmissionContent.Type.IMAGE) {
//            Timber.i("Optimized image: %s", submissionContent.imageContentUrl(deviceDisplayWidth));
//        }

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
                    unsubscribeOnCollapse(
                            linkDetailsViewHolder.populate(((RedditLink) contentLink))
                    );
                    linkDetailsView.setOnClickListener(__ -> OpenUrlActivity.handle(getContext(), contentLink, null));
                }
                break;

            case EXTERNAL:
                contentLoadProgressView.hide();
                String redditSuppliedThumbnail = activeSubmission.getThumbnail();
                unsubscribeOnCollapse(linkDetailsViewHolder.populate(((Link.External) contentLink), redditSuppliedThumbnail));
                linkDetailsView.setOnClickListener(__ -> OpenUrlActivity.handle(getContext(), contentLink, null));
                break;

            case VIDEO:
                String videoUrl = ((MediaLink) contentLink).lowQualityVideoUrl();
                Timber.i("videoUrl: %s", videoUrl);
                contentVideoViewHolder.load(Dank.httpProxyCacheServer().getProxyUrl(videoUrl));
                break;

            default:
                throw new UnsupportedOperationException("Unknown content: " + contentLink);
        }

        linkDetailsViewHolder.setVisible(contentLink.isExternal() || contentLink.isRedditHosted() && !submission.isSelfPost());
        selfPostTextView.setVisibility(contentLink.isRedditHosted() && submission.isSelfPost() ? View.VISIBLE : View.GONE);
        contentImageView.setVisibility(contentLink.isImageOrGif() ? View.VISIBLE : View.GONE);
        contentVideoView.setVisibility(contentLink.isVideo() ? View.VISIBLE : View.GONE);

        // Show shadows behind the toolbar because image/video submissions have a transparent toolbar.
        boolean transparentToolbar = contentLink.type() == Link.Type.IMAGE_OR_GIF || contentLink.type() == Link.Type.VIDEO;
        toolbarBackground.setSyncScrollEnabled(transparentToolbar);
        toolbarShadows.setVisibility(transparentToolbar ? View.VISIBLE : View.GONE);

        // Stick the content progress bar below the toolbar if it's an external link. Otherwise, make
        // it scroll with the comments sheet.
        contentLoadProgressView.setSyncScrollEnabled(contentLink.type() != Link.Type.EXTERNAL);
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

    }

    @Override
    public void onPageCollapsed() {
        Glide.clear(contentImageView);
        onCollapseSubscriptions.clear();
    }

    private void unsubscribeOnCollapse(Subscription subscription) {
        onCollapseSubscriptions.add(subscription);
    }

}
