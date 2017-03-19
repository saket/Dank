package me.saket.dank.ui.submission;

import static butterknife.ButterKnife.findById;
import static me.saket.dank.utils.GlideUtils.simpleImageViewTarget;
import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.touchLiesOn;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.Submission;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindDimen;
import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.BuildConfig;
import me.saket.dank.OpenRedditUrlActivity;
import me.saket.dank.R;
import me.saket.dank.data.RedditUrl;
import me.saket.dank.data.SubmissionContent;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.ui.subreddits.SubredditActivity;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.DeviceUtils;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RedditUrlParser;
import me.saket.dank.utils.SubmissionContentParser;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatableProgressBar;
import me.saket.dank.widgets.AnimatedToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.ZoomableImageView;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func1;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends DankFragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    private static final String KEY_SUBMISSION_JSON = "submissionJson";
    private static final String BLANK_PAGE_URL = "about:blank";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_toolbar_shadow) View toolbarShadows;
    @BindView(R.id.submission_toolbar_background) AnimatedToolbarBackground toolbarBackground;
    @BindView(R.id.submission_content_progress) AnimatableProgressBar contentLoadProgressView;
    @BindView(R.id.submission_webview_container) ViewGroup contentWebViewContainer;
    @BindView(R.id.submission_webview) WebView contentWebView;
    @BindView(R.id.submission_image) ZoomableImageView contentImageView;
    @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
    @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
    @BindView(R.id.submission_comment_list) RecyclerView commentList;
    @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;

    @BindDrawable(R.drawable.ic_toolbar_close_24dp) Drawable closeIconDrawable;
    @BindDimen(R.dimen.submission_commentssheet_minimum_visible_height) int commentsSheetMinimumVisibleHeight;

    private ExpandablePageLayout submissionPageLayout;
    private CommentsAdapter commentsAdapter;
    private Subscription commentsSubscription;
    private CommentsHelper commentsHelper;
    private Submission currentSubmission;
    private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();

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
        View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
        ButterKnife.bind(this, fragmentLayout);

        submissionPageLayout = findById(getActivity(), R.id.subreddit_submission_page);
        submissionPageLayout.addCallbacks(this);
        submissionPageLayout.setPullToCollapseIntercepter(this);

        Views.getStatusBarHeight(fragmentLayout, statusBarHeight -> {
            Views.setMarginTop(toolbar, statusBarHeight);
            Views.executeOnMeasure(toolbar, () -> {
                Views.setHeight(toolbarBackground, toolbar.getHeight() + statusBarHeight);
            });
        });
        toolbar.setNavigationOnClickListener(v -> ((Callbacks) getActivity()).onClickSubmissionToolbarUp());

        DankLinkMovementMethod linkMovementMethod = DankLinkMovementMethod.newInstance();
        linkMovementMethod.setOnLinkClickListener((textView, url) -> {
            // TODO: 18/03/17 Remove try/catch block
            try {
                RedditUrl redditUrl = RedditUrlParser.parse(url);
                if (redditUrl != null) {
                    Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();
                    Rect clickedUrlCoordinatesRect = new Rect(0, clickedUrlCoordinates.y, deviceDisplayWidth, clickedUrlCoordinates.y);
                    OpenRedditUrlActivity.handle(getActivity(), redditUrl, clickedUrlCoordinatesRect);
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            Timber.i("Unknown link");
            return false;
        });
        selfPostTextView.setMovementMethod(linkMovementMethod);

        // TODO: 01/02/17 Should we preload Views for adapter rows?
        // Setup comment list and its adapter.
        commentsAdapter = new CommentsAdapter(getResources(), linkMovementMethod);
        commentList.setAdapter(RecyclerAdapterWithHeader.wrap(commentsAdapter, commentsHeaderView));
        commentList.setLayoutManager(new LinearLayoutManager(getActivity()));
        commentList.setItemAnimator(new DefaultItemAnimator());

        setupCommentsHelper();
        setupContentWebView();
        setupContentImageView();
        setupCommentsSheet();

        // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
        deviceDisplayWidth = fragmentLayout.getResources().getDisplayMetrics().widthPixels;

        // Restore submission if the Activity was recreated.
        if (savedInstanceState != null) {
            onRestoreSavedInstanceState(savedInstanceState);
        }
        return fragmentLayout;
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
                        .doOnNext(commentsHelper.setMoreCommentsLoading(true))
                        .observeOn(io())
                        .map(Dank.reddit().loadMoreComments())
                        .subscribe(commentsHelper.setMoreCommentsLoading(false), error -> {
                            Timber.e(error, "Failed to load more comments");
                            if (isAdded()) {
                                Toast.makeText(getActivity(), R.string.submission_error_failed_to_load_more_comments, Toast.LENGTH_SHORT).show();
                            }
                        })
        );
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

        // TODO: 04/03/17 How do we find out if there's a registered app for certain URLs?
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
                        Toast.makeText(getActivity(), R.string.submission_error_no_app_to_handle_intent, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            }
        });
    }

    private void setupContentImageView() {
        contentImageView.setGravity(Gravity.TOP);
        Views.setMarginBottom(contentImageView, commentsSheetMinimumVisibleHeight);
    }

    private void setupCommentsSheet() {
        toolbarBackground.syncBottomWithViewTop(commentListParentSheet);

        Func1<?, Integer> revealDistanceFunc = __ -> {
            // If the sheet cannot scroll up because the top-margin > sheet's peek distance, scroll it to 70%
            // of its height so that the user doesn't get confused upon not seeing the sheet scroll up.
            return (int) Math.min(
                    commentListParentSheet.getHeight() * 8 / 10,
                    contentImageView.getVisibleZoomedImageHeight() - commentListParentSheet.getTop()
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

        // Calculates if the top of the comment sheet is directly below the image.
        Func0<Boolean> isCommentSheetBeneathImageFunc = () -> {
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
                        || (isZoomingOut && contentImageView.getVisibleZoomedImageHeight() <= commentListParentSheet.getY())) {
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
        commentsHelper.reset();
        commentsAdapter.updateData(null);

        SubmissionContent submissionContent = SubmissionContentParser.parse(submission);

        // Show shadows behind the toolbar because image/video submissions have a transparent toolbar.
        toolbarBackground.setEnabled(submissionContent.isImageOrVideo());
        toolbarShadows.setVisibility(submissionContent.isImageOrVideo() ? View.VISIBLE : View.GONE);

        // Update submission information.
        //noinspection deprecation
        titleView.setText(Html.fromHtml(submission.getTitle()));
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));

        // Load self-text/media/webpage.
        loadSubmissionContent(submission, submissionContent);

        // Load new comments.
        commentsLoadProgressView.setVisibility(View.VISIBLE);

        commentsSubscription = Dank.reddit()
                .withAuth(Dank.reddit().fullSubmissionData(submission))
                .compose(applySchedulers())
                .doOnTerminate(() -> commentsLoadProgressView.setVisibility(View.GONE))
                .subscribe(commentsHelper.setup(), logError("Couldn't get comments"));
    }

    private void loadSubmissionContent(Submission submission, SubmissionContent submissionContent) {
//        Timber.d("-------------------------------------------");
//        Timber.i("%s", submission.getTitle());
//        Timber.i("Post hint: %s, URL: %s", submission.getPostHint(), submission.getUrl());
//        Timber.i("Parsed content: %s", submissionContent);
//        if (submissionContent.type() == SubmissionContent.Type.IMAGE) {
//            Timber.i("Optimized image: %s", submissionContent.imageContentUrl(deviceDisplayWidth));
//        }

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
                        .into(simpleImageViewTarget(contentImageView, resource -> {
                            Views.executeOnMeasure(contentImageView, () -> {
                                contentLoadProgressView.hide();

                                int imageMaxVisibleHeight = contentImageView.getHeight();
                                float widthResizeFactor = deviceDisplayWidth / (float) resource.getMinimumWidth();
                                int visibleImageHeight = Math.min((int) (resource.getIntrinsicHeight() * widthResizeFactor), imageMaxVisibleHeight);

                                contentImageView.setImageDrawable(resource);

                                // Reveal the image smoothly or right away depending upon whether or not this
                                // page is already expanded and visible.
                                Views.executeOnNextLayout(commentListParentSheet, () -> {
                                    int revealDistance = visibleImageHeight - commentListParentSheet.getTop();
                                    commentListParentSheet.setPeekHeight(commentListParentSheet.getHeight() - revealDistance);

                                    commentListParentSheet.setScrollingEnabled(true);
                                    if (submissionPageLayout.isExpanded()) {
                                        // Smoothly reveal the image.
                                        commentListParentSheet.smoothScrollTo(revealDistance);
                                    } else {
                                        commentListParentSheet.scrollTo(revealDistance);
                                    }
                                });
                            });
                        }));
                break;

            case SELF:
                contentLoadProgressView.hide();
                String selfTextHtml = submission.getDataNode().get("selftext_html").asText();
                CharSequence markdownHtml = Markdown.parseRedditMarkdownHtml(selfTextHtml, selfPostTextView.getPaint());
                selfPostTextView.setText(markdownHtml);

//                Timber.d("Html: %s", submission.getDataNode().toString());
//                selfPostTextView.setText(Html.fromHtml("X<sup>2</sup>"));
                break;

            case LINK:
                // Reset the page before loading the new submission so that the last submission isn't visible.
                contentWebView.loadUrl(BLANK_PAGE_URL);
                contentWebView.loadUrl(submission.getUrl());

                Views.executeOnMeasure(commentListParentSheet, () -> {
                    commentListParentSheet.setScrollingEnabled(true);
                    commentListParentSheet.setPeekHeight(commentsSheetMinimumVisibleHeight);
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
