package me.saket.dank.ui.submission;

import static butterknife.ButterKnife.findById;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.logError;
import static rx.Observable.just;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.alexvasilkov.gestures.GestureController;
import com.alexvasilkov.gestures.State;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.SubmissionContent;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.utils.DeviceUtils;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.SubmissionContentParser;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatableProgressBar;
import me.saket.dank.widgets.AnimatableToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SubmissionParallaxImageContainer;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;
import rx.Subscription;
import rx.functions.Func1;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends Fragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    private static final String KEY_SUBMISSION_JSON = "submissionJson";
    private static final String BLANK_PAGE_URL = "about:blank";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_toolbar_shadow) View toolbarShadows;
    @BindView(R.id.submission_toolbar_background) AnimatableToolbarBackground toolbarBackground;
    @BindView(R.id.submission_content_progress) AnimatableProgressBar contentLoadProgressView;
    @BindView(R.id.submission_webview_container) ViewGroup contentWebViewContainer;
    @BindView(R.id.submission_webview) WebView contentWebView;
    @BindView(R.id.submission_image_container) SubmissionParallaxImageContainer contentImageContainer;
    @BindView(R.id.submission_image) ZoomableImageView contentImageView;
    @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
    @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
    @BindView(R.id.submission_comment_list) RecyclerView commentList;
    @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;

    @BindDrawable(R.drawable.ic_close_black_24dp) Drawable closeIconDrawable;
    @BindDimen(R.dimen.submission_commentssheet_minimum_visible_height) int commentsSheetMinimumVisibleHeight;

    private ExpandablePageLayout submissionPageLayout;
    private CommentsAdapter commentsAdapter;
    private Subscription commentsSubscription;
    private CommentsCollapseHelper commentsCollapseHelper;
    private Submission currentSubmission;
    private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();

    private int deviceDisplayWidth;
    private int deviceDisplayHeight;

    public interface Callbacks {
        void onSubmissionToolbarUpClick();
    }

    public static SubmissionFragment create() {
        return new SubmissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
        ButterKnife.bind(this, fragmentLayout);

        submissionPageLayout = findById(getActivity(), R.id.subreddit_submission_page);
        submissionPageLayout.addCallbacks(this);
        submissionPageLayout.setPullToCollapseIntercepter(this);

        fragmentLayout.setOnApplyWindowInsetsListener((v, insets) -> {
            int statusBarHeight = insets.getSystemWindowInsetTop();
            Views.setMarginTop(toolbar, statusBarHeight);
            Views.executeOnMeasure(toolbar, () -> {
                Views.setHeight(toolbarBackground, toolbar.getHeight() + statusBarHeight);
            });
            return insets;
        });

        // Add a close icon to the toolbar.
        closeIconDrawable = closeIconDrawable.mutate();
        closeIconDrawable.setTint(ContextCompat.getColor(getActivity(), R.color.white));
        toolbar.setNavigationIcon(closeIconDrawable);
        toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onSubmissionToolbarUpClick());
        toolbar.setBackground(null);

        // TODO: 01/02/17 Should we preload Views for adapter rows?
        // Setup comment list and its adapter.
        commentsAdapter = new CommentsAdapter();
        commentsAdapter.setOnCommentClickListener(comment -> {
            // Collapse/expand on tap.
            commentsAdapter.updateData(commentsCollapseHelper.toggleCollapseAndGet(comment));
        });
        commentList.setAdapter(RecyclerAdapterWithHeader.wrap(commentsAdapter, commentsHeaderView));
        commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
        commentList.setItemAnimator(new DefaultItemAnimator());

        commentsCollapseHelper = new CommentsCollapseHelper();
        setupContentWebView();
        setupContentImageView();
        setupCommentsSheet();

        // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        deviceDisplayWidth = metrics.widthPixels;
        deviceDisplayHeight = metrics.heightPixels;

        // Restore submission if the Activity was recreated.
        if (savedInstanceState != null) {
            onRestoreSavedInstanceState(savedInstanceState);
        }
        return fragmentLayout;
    }

    private void setupContentWebView() {
        contentWebView.getSettings().setJavaScriptEnabled(true);
        contentWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                contentLoadProgressView.setIndeterminate(false);
                contentLoadProgressView.setProgressWithAnimation(newProgress);
                contentLoadProgressView.setVisible(newProgress < 100);
            }
        });

        contentWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String urlScheme = request.getUrl().normalizeScheme().getScheme();
                if ("http".equals(urlScheme) || "https".equals(urlScheme)) {
                    // Let the WebView load this URL.
                    return false;

                } else {
                    // A deep-link was clicked. Send an Intent instead.
                    Intent deepLinkIntent = Intents.createForUrl(request.getUrl().toString());
                    if (Intents.hasAppToHandleIntent(getActivity(), deepLinkIntent)) {
                        startActivity(deepLinkIntent);

                    } else {
                        Toast.makeText(getActivity(), R.string.error_no_app_to_handle_intent, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
        });
    }

    private void setupContentImageView() {
        contentImageView.setGravity(Gravity.TOP);
        Views.setMarginBottom(contentImageContainer, commentsSheetMinimumVisibleHeight);
    }

    private boolean isCommentSheetBeneathImage;

    private void setupCommentsSheet() {
        toolbarBackground.syncBottomWithViewTop(commentListParentSheet);
        //contentImageContainer.syncParallaxWith(commentListParentSheet);

        Func1<?, Integer> revealDistanceFunc = __ -> {
            // If the sheet cannot scroll up because the top-margin > sheet's peek distance, scroll it to 70%
            // of its height so that the user doesn't get confused upon not seeing the sheet scroll up.
            return (int) Math.min(
                    commentListParentSheet.getHeight() * 8 / 10,
                    contentImageView.getZoomedImageHeight() - commentListParentSheet.getTop()
            );
        };

        // Toggle sheet's collapsed state on image click.
        contentImageView.setOnClickListener(v -> {
            commentListParentSheet.smoothScrollTo(revealDistanceFunc.call(null));
        });

        // and on submission title click.
        commentsHeaderView.setOnClickListener(v -> {
            if (commentListParentSheet.isAtPeekHeightState()) {
                commentListParentSheet.smoothScrollTo(revealDistanceFunc.call(null));
            }
        });

        // TODO: 25/02/17 Document.
        Func1<?, Boolean> isCommentSheetBeneathImageFunc = __ -> {
            return (int) commentListParentSheet.getY() == (int) contentImageView.getZoomedImageHeight();
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

                int imageRevealDistance = (int) Math.min(
                        commentListParentSheet.getHeight() - commentsSheetMinimumVisibleHeight,
                        contentImageView.getZoomedImageHeight() - commentListParentSheet.getTop()
                );

                if (isCommentSheetBeneathImage
                        // This is a hacky workaround: when zooming out, the received callbacks are very discrete and
                        // it becomes difficult to lock the comments sheet beneath the image.
                        || (isZoomingOut && contentImageView.getZoomedImageHeight() < commentListParentSheet.getY())) {
                    commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - imageRevealDistance);
                    commentListParentSheet.scrollTo(imageRevealDistance);
                }
            }

            @Override
            public void onStateReset(State oldState, State newState) {

            }
        });

        commentListParentSheet.addOnSheetScrollChangeListener(newScrollY -> {
            isCommentSheetBeneathImage = isCommentSheetBeneathImageFunc.call(null);
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (currentSubmission != null) {
            JsonNode dataNode = currentSubmission.getDataNode();
            outState.putString(KEY_SUBMISSION_JSON, dataNode.toString());
        }
        super.onSaveInstanceState(outState);
    }

    private void onRestoreSavedInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(KEY_SUBMISSION_JSON)) {
            try {
                String submissionJson = savedInstanceState.getString(KEY_SUBMISSION_JSON);
                JsonNode jsonNode = new ObjectMapper().readTree(submissionJson);
                populateUi(new Submission(jsonNode));

            } catch (IOException e) {
                Timber.e(e, "Couldn't deserialize Submission for state restoration");
            }
        }
    }

    /**
     * Update the submission to be shown. Since this Fragment is retained by {@link SubredditActivity},
     * we only update the UI everytime a new submission is to be shown.
     */
    public void populateUi(Submission submission) {
        currentSubmission = submission;

        // Reset everything.
        contentLoadProgressView.setProgress(0);
        commentListParentSheet.scrollTo(0);
        commentListParentSheet.setScrollingEnabled(false);
        commentsCollapseHelper.reset();
        commentsAdapter.updateData(null);

        SubmissionContent submissionContent = SubmissionContentParser.parse(submission);

        // Show shadows behind the toolbar because image/video submissions have a transparent toolbar.
        toolbarBackground.setEnabled(submissionContent.isImageOrVideo());
        toolbarShadows.setVisibility(submissionContent.isImageOrVideo() ? View.VISIBLE : View.GONE);

        // Update submission information.
        titleView.setText(submission.getTitle());
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));

        // Load self-text/media/webpage.
        loadSubmissionContent(submission, submissionContent);

        // Load new comments.
        commentsLoadProgressView.setVisibility(View.VISIBLE);
        commentsSubscription = Dank.reddit()
                .authenticateIfNeeded()
                .flatMap(__ -> just(Dank.reddit().fullSubmissionData(submission.getId())))
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired())
                .map(submissionData -> {
                    CommentNode commentNode = submissionData.getComments();
                    commentsCollapseHelper.setupWith(commentNode);
                    return commentsCollapseHelper.flattenExpandedComments();
                })
                .compose(applySchedulers())
                .doOnTerminate(() -> commentsLoadProgressView.setVisibility(View.GONE))
                .subscribe(commentsAdapter, logError("Couldn't get comments"));
    }

    private void loadSubmissionContent(Submission submission, SubmissionContent submissionContent) {
        Timber.d("-------------------------------------------");
        Timber.i("%s", submission.getTitle());
        Timber.i("Post hint: %s, URL: %s", submission.getPostHint(), submission.getUrl());
        Timber.i("Parsed content: %s", submissionContent);
        if (submissionContent.type() == SubmissionContent.Type.IMAGE) {
            Timber.i("Optimized image: %s", submissionContent.imageContentUrl(deviceDisplayWidth));
        }

        switch (submissionContent.type()) {
            case IMAGE:
                if (submissionContent.host() == SubmissionContent.Host.IMGUR) {
                    SubmissionContent.Imgur imgurContent = submissionContent.hostContent();
                    if (imgurContent.isAlbum()) {
                        contentLoadProgressView.hide();
                        Toast.makeText(getActivity(), "Imgur album", Toast.LENGTH_SHORT).show();
                        break;
                    }
                } else if (submissionContent.host() == SubmissionContent.Host.GFYCAT) {
                    contentLoadProgressView.hide();
                    Toast.makeText(getActivity(), "GFYCAT", Toast.LENGTH_SHORT).show();
                    break;
                }

                contentLoadProgressView.setIndeterminate(true);
                contentLoadProgressView.show();

                Glide.with(this)
                        .load(submissionContent.imageContentUrl(deviceDisplayWidth))
                        .priority(Priority.IMMEDIATE)
                        .into(new ImageViewTarget<GlideDrawable>(contentImageView) {
                            @Override
                            protected void setResource(GlideDrawable resource) {
                                contentLoadProgressView.hide();

                                int imageMaxVisibleHeight = deviceDisplayHeight - commentsSheetMinimumVisibleHeight;
                                float widthResizeFactor = deviceDisplayWidth / (float) resource.getIntrinsicWidth();
                                int visibleImageHeight = Math.min((int) (resource.getIntrinsicHeight() * widthResizeFactor), imageMaxVisibleHeight);
                                //contentImageContainer.setThresholdHeightForParallax(visibleImageHeight);

                                // If the image is longer than the visible window, zoom in to fill the width on double tap.
                                boolean isImageLongerThanVisibleWindow = imageMaxVisibleHeight < resource.getIntrinsicHeight();
                                if (isImageLongerThanVisibleWindow && resource.getIntrinsicWidth() <= resource.getIntrinsicHeight()) {
                                    //contentImageView.setDoubleTapZoomScale(widthResizeFactor);
                                }

                                contentImageView.setImageDrawable(resource);

//                                contentImageView.setImage(
//                                        ImageSource.cachedBitmap(resource),
//                                        new ImageViewState(widthResizeFactor, new PointF(0, 0), SubsamplingScaleImageView.ORIENTATION_0)
//                                );

                                // Reveal the image smoothly or right away depending upon whether or not this
                                // page is already expanded and visible.
                                Views.executeOnNextLayout(commentListParentSheet, () -> {
                                    commentListParentSheet.setScrollingEnabled(true);

                                    // TODO: 25/02/17 calculate this revealdistance using revealDistanceFunc.
                                    int revealDistance = visibleImageHeight - commentListParentSheet.getTop();
                                    commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);

                                    if (submissionPageLayout.isExpanded()) {
                                        // Smoothly reveal the image.
                                        commentListParentSheet.smoothScrollTo(revealDistance);
                                    } else {
                                        commentListParentSheet.scrollTo(revealDistance);
                                    }
                                });
                            }
                        });
                break;

            case SELF:
                contentLoadProgressView.hide();
                selfPostTextView.setText(submission.getSelftext());
                break;

            case LINK:
                // Reset the page before loading the new submission so that the last submission isn't visible.
                contentWebView.loadUrl(BLANK_PAGE_URL);
                contentWebView.loadUrl(submission.getUrl());

                Views.executeOnMeasure(commentListParentSheet, () -> {
                    commentListParentSheet.setScrollingEnabled(true);
                    commentListParentSheet.scrollTo(commentListParentSheet.getHeight() * 3 / 10);
                });
                break;

            case VIDEO:
            case UNKNOWN:
                // TODO: 12/02/17.
                contentLoadProgressView.hide();
                break;

            default:
                throw new UnsupportedOperationException("Unknown content: " + submissionContent);
        }

        selfPostTextView.setVisibility(submissionContent.isSelfText() ? View.VISIBLE : View.GONE);
        contentImageView.setVisibility(submissionContent.isImage() ? View.VISIBLE : View.GONE);

        if (submissionContent.isLink()) {
            // Show the WebView only when this page is fully expanded or else it'll interfere with the entry animation.
            // Also ignore loading the WebView on the emulator. It is very expensive and greatly slow down the emulator.
            if (!BuildConfig.DEBUG || !DeviceUtils.isEmulator()) {
                pendingOnExpandRunnables.add(() -> {
                    contentWebView.setVisibility(View.VISIBLE);
                });
            }
            contentWebViewContainer.setVisibility(View.VISIBLE);

        } else {
            contentWebView.setVisibility(View.GONE);
            contentWebViewContainer.setVisibility(View.GONE);
        }
    }

// ======== EXPANDABLE PAGE CALLBACKS ======== //

    /**
     * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward pull == downward scroll and vice versa.
     * @return True to consume this touch event. False otherwise.
     */
    @Override
    public boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull) {
        Rect commentSheetBounds = new Rect();
        commentListParentSheet.getGlobalVisibleRect(commentSheetBounds);
        boolean touchInsideCommentsSheet = commentSheetBounds.contains((int) downX, (int) downY);

        //noinspection SimplifiableIfStatement
        if (touchInsideCommentsSheet) {
            return upwardPagePull
                    ? commentListParentSheet.canScrollUpwardsAnyFurther()
                    : commentListParentSheet.canScrollDownwardsAnyFurther();
        } else {
            Rect imageBounds = new Rect();
            contentImageView.getGlobalVisibleRect(imageBounds);
            boolean touchInsideImageView = imageBounds.contains((int) downX, (int) downY);

            //noinspection RedundantIfStatement
            if (touchInsideImageView && contentImageView.canPanVertically(!upwardPagePull)) {
                return true;
            }

            return false;
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
        if (commentsSubscription != null) {
            commentsSubscription.unsubscribe();
        }

        contentWebView.stopLoading();
    }

// ======== BACK-PRESS ======== //

    /**
     * @return true if the back press should be intercepted. False otherwise.
     */
    public boolean handleBackPress() {
        if (currentSubmission != null && !commentListParentSheet.isExpanded() && contentWebView.getVisibility() == View.VISIBLE) {
            if (contentWebView.canGoBack() && !BLANK_PAGE_URL.equals(Views.previousUrlInHistory(contentWebView))) {
                // WebView is visible and can go back.
                contentWebView.goBack();
                return true;
            }
        }

        return false;
    }

}
