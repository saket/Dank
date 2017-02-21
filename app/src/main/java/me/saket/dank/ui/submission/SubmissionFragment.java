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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.SubmissionContent;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddits.SubRedditActivity;
import me.saket.dank.utils.DeviceUtils;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.SubmissionContentParser;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatableProgressBar;
import me.saket.dank.widgets.AnimatableToolbarBackground;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import rx.Subscription;
import timber.log.Timber;

@SuppressLint("SetJavaScriptEnabled")
public class SubmissionFragment extends Fragment implements ExpandablePageLayout.Callbacks, ExpandablePageLayout.OnPullToCollapseIntercepter {

    private static final String KEY_SUBMISSION_JSON = "submissionJson";
    private static final java.lang.String BLANK_PAGE_URL = "about:blank";

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_toolbar_shadow) View toolbarShadows;
    @BindView(R.id.submission_toolbar_background) AnimatableToolbarBackground toolbarBackground;
    @BindView(R.id.submission_content_progress) AnimatableProgressBar contentLoadProgressView;
    @BindView(R.id.submission_webview_container) ViewGroup contentWebViewContainer;
    @BindView(R.id.submission_webview) WebView contentWebView;
    @BindView(R.id.submission_linked_image) ImageView contentImageView;
    @BindView(R.id.submission_comment_list_parent_sheet) ScrollingRecyclerViewSheet commentListParentSheet;
    @BindView(R.id.submission_comments_header) ViewGroup commentsHeaderView;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;
    @BindView(R.id.submission_selfpost_text) TextView selfPostTextView;
    @BindView(R.id.submission_comment_list) RecyclerView commentList;
    @BindView(R.id.submission_comments_progress) View commentsLoadProgressView;

    @BindDrawable(R.drawable.ic_close_black_24dp) Drawable closeIconDrawable;

    private ExpandablePageLayout submissionPageLayout;
    private CommentsAdapter commentsAdapter;
    private Subscription commentsSubscription;
    private CommentsCollapseHelper commentsCollapseHelper;
    private Submission currentSubmission;
    private List<Runnable> pendingOnExpandRunnables = new LinkedList<>();

    private int deviceDisplayWidth;

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
        toolbarBackground.syncBottomWithViewTop(commentListParentSheet);

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

        // Get the display width, that will be used in populateUi() for loading an optimized image for the user.
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
        deviceDisplayWidth = metrics.widthPixels;

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
     * Update the submission to be shown. Since this Fragment is retained by {@link SubRedditActivity},
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
                        .into(new GlideDrawableImageViewTarget(contentImageView) {
                            @Override
                            protected void setResource(GlideDrawable resource) {
                                super.setResource(resource);
                                // TransitionDrawable (used by Glide for fading-in) messes up the scaleType. Set it everytime.
                                contentImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                contentLoadProgressView.hide();

                                // Reveal the image smoothly or right away depending upon whether or not
                                // this page is already expanded and visible.
                                Views.executeOnNextLayout(contentImageView, () -> {
                                    int revealDistance = contentImageView.getHeight() - toolbar.getBottom();
                                    if (submissionPageLayout.isExpanded()) {
                                        // Smoothly reveal the image.
                                        commentListParentSheet.setScrollingEnabled(true);
                                        commentListParentSheet.smoothScrollTo(revealDistance);

                                    } else {
                                        commentListParentSheet.setScrollingEnabled(true);
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
