package me.saket.dank.ui.submission;

import static android.text.TextUtils.isEmpty;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doOnSingleStartAndEnd;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Urls.parseDomainName;
import static me.saket.dank.utils.Views.setDimensions;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.setPaddingVertical;
import static me.saket.dank.utils.Views.setWidth;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.SizeReadyCallback;

import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.LinkMetadata;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.RedditLink;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.GlideCircularTransformation;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.UrlMetadataParser;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatedProgressBar;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.SimpleExpandablePageCallbacks;
import timber.log.Timber;

/**
 * Used when a submission points to a Reddit hosted URL, which can be another submission or a subreddit or a user.
 * Manages Views for showing details of the linked URL.
 */
public class SubmissionLinkHolder {

    private static final int TINT_TRANSITION_ANIMATION_DURATION = 300;

    @BindView(R.id.submission_link_icon_container) ViewGroup iconContainer;
    @BindView(R.id.submission_link_thumbnail) ImageView thumbnailView;
    @BindView(R.id.submission_link_icon) ImageView iconView;
    @BindView(R.id.submission_link_title) TextView titleView;
    @BindView(R.id.submission_link_subtitle) TextView subtitleView;
    @BindView(R.id.submission_link_title_container) ViewGroup titleSubtitleContainer;
    @BindView(R.id.submission_link_progress) AnimatedProgressBar progressView;

    @BindDimen(R.dimen.submission_link_thumbnail_width_reddit_link) int thumbnailWidthForRedditLink;
    @BindDimen(R.dimen.submission_link_thumbnail_width_external_link) int thumbnailWidthForExternalLink;
    @BindDimen(R.dimen.submission_link_thumbnail_width_album) int thumbnailWidthForAlbum;
    @BindDimen(R.dimen.submission_link_favicon_min_size) int faviconMinSizePx;
    @BindDimen(R.dimen.submission_link_favicon_max_size) int faviconMaxSizePx;
    @BindDimen(R.dimen.submission_link_title_container_vert_padding_album) int titleContainerVertPaddingForAlbum;
    @BindDimen(R.dimen.submission_link_title_container_vert_padding_link) int titleContainerVertPaddingForLink;

    @BindColor(R.color.window_background) int windowBackgroundColor;
    @BindColor(R.color.submission_link_background_color) int linkDetailsContainerBackgroundColor;
    @BindColor(R.color.gray_400) int redditLinkIconTintColor;
    @BindColor(R.color.submission_link_title) int titleTextColor;
    @BindColor(R.color.submission_link_subtitle) int subtitleTextColor;
    @BindColor(R.color.submission_link_title_dark) int titleTextColorForDarkBackground;
    @BindColor(R.color.submission_link_subtitle_dark) int subtitleTextColorForDarkBackground;

    private final ViewGroup linkDetailsContainer;
    private ValueAnimator holderHeightAnimator;

    public SubmissionLinkHolder(ViewGroup linkedRedditLinkView, ExpandablePageLayout submissionPageLayout) {
        this.linkDetailsContainer = linkedRedditLinkView;
        ButterKnife.bind(this, linkedRedditLinkView);
        linkedRedditLinkView.setClipToOutline(true);

        submissionPageLayout.addCallbacks(new SimpleExpandablePageCallbacks() {
            @Override
            public void onPageCollapsed() {
                resetViews();
            }
        });
    }

    public void setVisible(boolean visible) {
        linkDetailsContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public int thumbnailWidthForExternalLink() {
        return thumbnailWidthForExternalLink;
    }

    public int getThumbnailWidthForAlbum() {
        return thumbnailWidthForAlbum;
    }

    private void resetViews() {
        // Stop loading of any pending images.
        Glide.clear(thumbnailView);
        Glide.clear(iconView);

        linkDetailsContainer.setBackgroundTintList(null);
        thumbnailView.setImageDrawable(null);
        thumbnailView.setAlpha(0f);
        iconView.setImageDrawable(null);
        iconView.setBackground(null);
        setDimensions(iconView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        titleView.setMaxLines(1);
        titleView.setTextColor(titleTextColor);
        subtitleView.setTextColor(subtitleTextColor);
        setPaddingVertical(titleSubtitleContainer, titleContainerVertPaddingForLink);

        linkDetailsContainer.setBackgroundResource(R.drawable.background_submission_link);

        if (holderHeightAnimator != null && holderHeightAnimator.isStarted()) {
            holderHeightAnimator.cancel();
        }
        setHeight(linkDetailsContainer, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /**
     * Load and show title of user/subreddit/submission.
     */
    public Disposable populate(RedditLink redditLink) {
        setWidth(iconContainer, thumbnailWidthForRedditLink);
        setPaddingVertical(titleSubtitleContainer, titleContainerVertPaddingForLink);

        Resources resources = titleView.getResources();
        iconView.setImageTintList(ColorStateList.valueOf(redditLinkIconTintColor));

        if (redditLink instanceof RedditLink.Subreddit) {
            iconView.setContentDescription(resources.getString(R.string.submission_link_linked_subreddit));
            iconView.setImageResource(R.drawable.ic_subreddits_24dp);
            titleView.setText(resources.getString(R.string.subreddit_name_r_prefix, ((RedditLink.Subreddit) redditLink).name));
            subtitleView.setText(R.string.submission_link_tap_to_open_subreddit);
            progressView.setVisibility(View.GONE);
            return Disposables.disposed();

        } else if (redditLink instanceof RedditLink.User) {
            iconView.setContentDescription(resources.getString(R.string.submission_link_linked_profile));
            iconView.setImageResource(R.drawable.ic_user_profile_24dp);
            titleView.setText(resources.getString(R.string.user_name_u_prefix, ((RedditLink.User) redditLink).name));
            subtitleView.setText(R.string.submission_link_tap_to_open_profile);
            progressView.setVisibility(View.GONE);
            return Disposables.disposed();

        } else if (redditLink instanceof RedditLink.Submission) {
            RedditLink.Submission submissionLink = (RedditLink.Submission) redditLink;
            iconView.setContentDescription(resources.getString(R.string.submission_link_linked_submission));
            iconView.setImageResource(R.drawable.ic_submission_24dp);
            titleView.setText(Uri.parse(submissionLink.url).getPath());
            subtitleView.setText(R.string.submission_link_tap_to_open_submission);
            return populateSubmissionTitle(submissionLink);

        } else {
            throw new UnsupportedOperationException("Unknown reddit link: " + redditLink);
        }
    }

    private Disposable populateSubmissionTitle(RedditLink.Submission submissionLink) {
        // Downloading the page's HTML to get the title is faster than getting the submission's data from the API.
        return UrlMetadataParser.parse(submissionLink.url, true)
                .compose(applySchedulersSingle())
                .compose(doOnSingleStartAndEnd(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(linkMetadata -> {
                    String submissionPageTitle = linkMetadata.title();

                    if (!isEmpty(submissionPageTitle)) {
                        // Reddit prefixes page titles with the linked commentator's name (if any). We don't want that.
                        int userNameSeparatorIndex = submissionPageTitle.indexOf("comments on");
                        if (userNameSeparatorIndex > -1) {
                            submissionPageTitle = submissionPageTitle.substring(userNameSeparatorIndex + "comments on".length());
                        }

                        titleView.setMaxLines(Integer.MAX_VALUE);
                        //noinspection deprecation
                        titleView.setText(Html.fromHtml(submissionPageTitle));
                        subtitleView.setText(subtitleView.getResources().getString(R.string.subreddit_name_r_prefix, submissionLink.subredditName));
                    }

                }, logError("Couldn't get link's meta-data: " + submissionLink.url));
    }

    public void populate(MediaLink.ImgurAlbum imgurAlbumLink, String redditSuppliedThumbnail) {
        // Animate the holder's entry. This block of code is really fragile and the animation only
        // works if these lines are called in their current order. Animating the dimensions of a
        // View is sadly difficult to do the right way.
        linkDetailsContainer.setVisibility(View.INVISIBLE);
        Views.executeOnMeasure(titleSubtitleContainer, true /* consumeOnPreDraw (This is important to avoid glitches) */, () -> {
            linkDetailsContainer.setVisibility(View.VISIBLE);

            holderHeightAnimator = ObjectAnimator.ofInt(0, titleSubtitleContainer.getHeight());
            holderHeightAnimator.addUpdateListener(animation -> setHeight(linkDetailsContainer, (int) animation.getAnimatedValue()));
            holderHeightAnimator.setDuration(300);
            holderHeightAnimator.setInterpolator(new FastOutSlowInInterpolator());
            holderHeightAnimator.start();
        });

        setWidth(iconContainer, thumbnailWidthForAlbum);
        setPaddingVertical(titleSubtitleContainer, titleContainerVertPaddingForAlbum);
        progressView.setVisibility(View.VISIBLE);

        Resources resources = titleView.getResources();
        thumbnailView.setContentDescription(titleView.getText());
        if (isEmpty(imgurAlbumLink.albumTitle())) {
            titleView.setText(R.string.submission_image_album);
            subtitleView.setText(resources.getString(R.string.submission_image_album_image_count, imgurAlbumLink.imageCount()));
        } else {
            titleView.setText(imgurAlbumLink.albumTitle());
            subtitleView.setText(resources.getString(R.string.submission_image_album_label_with_image_count, imgurAlbumLink.imageCount()));
            titleView.setMaxLines(Integer.MAX_VALUE);
        }

        iconView.setContentDescription(resources.getString(R.string.submission_link_imgur_gallery));
        iconView.setImageResource(R.drawable.ic_photo_library_24dp);

        String thumbnailUrl = isEmpty(redditSuppliedThumbnail) ? imgurAlbumLink.coverImageUrl() : redditSuppliedThumbnail;
        loadLinkThumbnail(false, thumbnailUrl, null, true /* hideProgressBarOnLoad */);
    }

    /**
     * Show information of an external link. Extracts meta-data from the URL to get the favicon and the title.
     */
    public Disposable populate(Link.External externalLink, @Nullable String redditSuppliedThumbnail) {
        setWidth(iconContainer, thumbnailWidthForExternalLink);
        setPaddingVertical(titleSubtitleContainer, titleContainerVertPaddingForLink);

        Resources resources = titleView.getResources();
        thumbnailView.setContentDescription(null);
        iconView.setContentDescription(resources.getString(R.string.submission_link_linked_url));
        iconView.setImageTintList(ColorStateList.valueOf(redditLinkIconTintColor));
        titleView.setText(externalLink.url);
        subtitleView.setText(parseDomainName(externalLink.url));
        progressView.setVisibility(View.VISIBLE);

        boolean isGooglePlayLink = UrlMetadataParser.isGooglePlayLink(externalLink.url);

        // Attempt to load image provided by Reddit. By doing this, we'll be able to load the image and
        // the URL meta-data in parallel. Additionally, reddit's supplied image will also be optimized
        // for the thumbnail size.
        if (isEmpty(redditSuppliedThumbnail)) {
            iconView.setImageResource(R.drawable.ic_link_black_24dp);
        } else {
            loadLinkThumbnail(isGooglePlayLink, redditSuppliedThumbnail, null, false /* hideProgressOnLoad */);
        }

        return UrlMetadataParser.parse(externalLink.url, false)
                .compose(applySchedulersSingle())
                .doOnError(__ -> progressView.setVisibility(View.GONE))
                .subscribe(linkMetadata -> {
                    if (isEmpty(linkMetadata.title())) {
                        titleView.setText(linkMetadata.url());
                    } else {
                        //noinspection deprecation
                        titleView.setText(Html.fromHtml(linkMetadata.title()));
                        titleView.setMaxLines(Integer.MAX_VALUE);
                    }
                    thumbnailView.setContentDescription(titleView.getText());

                    // Use link's image if Reddit did not supply with anything.
                    if (isEmpty(redditSuppliedThumbnail) && linkMetadata.hasImage()) {
                        // Resize down large images. This is not required for reddit supplied images
                        // because they're already chosen according to their size.
                        String linkImageUrl = getResizedImageUrl(linkMetadata.imageUrl());
                        loadLinkThumbnail(isGooglePlayLink, linkImageUrl, linkMetadata, true /* hideProgressOnLoad */);

                    } else {
                        progressView.setVisibility(View.GONE);
                    }
                    boolean hasLinkThumbnail = linkMetadata.hasImage() || !isEmpty(redditSuppliedThumbnail);
                    loadLinkFavicon(linkMetadata, hasLinkThumbnail, isGooglePlayLink);

                }, error -> {
                    if (!(error instanceof IllegalArgumentException) || !error.getMessage().contains("String must not be empty")) {
                        Timber.e(error, "Couldn't get link's meta-data: " + externalLink.url);
                    }
                    // Else, Link wasn't a webpage. Probably an image or a video.
                });
    }

    private String getResizedImageUrl(String imageUrl) {
        return String.format(Locale.ENGLISH,
                "http://rsz.io/%s?width=%d",
                imageUrl.substring((Uri.parse(imageUrl).getScheme() + "://").length(), imageUrl.length()),
                thumbnailWidthForExternalLink
        );
    }

    /**
     * @param linkMetadata For loading the favicon. Can be null to ignore favicon.
     */
    private void loadLinkThumbnail(boolean isGooglePlayLink, String thumbnailUrl, @Nullable LinkMetadata linkMetadata, boolean hideProgressBarOnLoad) {
        Glide.with(thumbnailView.getContext())
                .load(thumbnailUrl)
                .asBitmap()
                .listener(new GlideUtils.SimpleRequestListener<String, Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource) {
                        generateTintColorFromImage(isGooglePlayLink, resource, tintColor -> {
                            if (!isGooglePlayLink) {
                                thumbnailView.setColorFilter(Colors.applyAlpha(tintColor, 0.4f));
                            }
                            tintViews(tintColor);
                        });

                        if (hideProgressBarOnLoad) {
                            progressView.setVisibility(View.GONE);
                        }
                        thumbnailView.animate().alpha(1f).setDuration(TINT_TRANSITION_ANIMATION_DURATION).start();
                    }

                    @Override
                    public void onException(Exception e) {
                        if (linkMetadata != null && linkMetadata.hasFavicon()) {
                            loadLinkFavicon(linkMetadata, false, isGooglePlayLink);
                        }
                    }
                })
                .into(thumbnailView);
    }

    /**
     * @param hasLinkThumbnail When true, tinting of the Views will be done using the favicon and the progress bar
     *                         will be shown while the favicon is fetched. This is used when the thumbnail couldn't
     *                         be loaded.
     */
    private void loadLinkFavicon(LinkMetadata linkMetadata, boolean hasLinkThumbnail, boolean isGooglePlayLink) {
        iconView.setImageTintList(null);

        if (!hasLinkThumbnail) {
            progressView.setVisibility(View.VISIBLE);
        }

        Glide.with(iconView.getContext())
                .load(linkMetadata.faviconUrl())
                .asBitmap()
                .listener(new GlideUtils.SimpleRequestListener<String, Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource) {
                        if (!hasLinkThumbnail) {
                            // Tint with favicon in case the thumbnail couldn't be fetched.
                            generateTintColorFromImage(isGooglePlayLink, resource, tintColor -> tintViews(tintColor));
                            progressView.setVisibility(View.GONE);
                        }

                        iconView.setVisibility(View.VISIBLE);
                        iconView.setBackgroundResource(R.drawable.background_submission_link_favicon_circle);

                        if (resource.getWidth() < faviconMinSizePx) {
                            setDimensions(iconView, faviconMinSizePx, faviconMinSizePx);

                        } else if (resource.getHeight() > faviconMaxSizePx) {
                            setDimensions(iconView, faviconMaxSizePx, faviconMaxSizePx);
                        }
                    }

                    @Override
                    public void onException(Exception e) {
                        if (e != null) {
                            Timber.e(e, "Couldn't load favicon");
                        }

                        // Show a generic icon if the favicon couldn't be fetched.
                        if (!hasLinkThumbnail) {
                            iconView.setImageResource(R.drawable.ic_link_black_24dp);
                        }
                    }
                })
                .transform(new GlideCircularTransformation(iconView.getContext()))
                .into(new ImageViewTarget<Bitmap>(iconView) {
                    @Override
                    protected void setResource(Bitmap resource) {
                        iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iconView.setImageBitmap(resource);
                    }

                    @Override
                    public void getSize(SizeReadyCallback cb) {
                        cb.onSizeReady(faviconMaxSizePx, faviconMaxSizePx);
                    }
                });
    }

    private interface OnTintColorGenerateListener {
        void onTintColorGenerate(int tintColor);
    }

    private void generateTintColorFromImage(boolean isGooglePlayLink, Bitmap bitmap, OnTintColorGenerateListener listener) {
        Palette.from(bitmap)
                .maximumColorCount(Integer.MAX_VALUE)    // Don't understand why, but this changes the darkness of the colors.
                .generate(palette -> {
                    int tint = -1;
                    if (isGooglePlayLink) {
                        // The color taken from Play Store's
                        tint = palette.getLightVibrantColor(-1);
                    }
                    if (tint == -1) {
                        // Mix the color with the window's background color to neutralize any possibly strong
                        // colors (e.g., strong blue, pink, etc.)
                        tint = Colors.mix(windowBackgroundColor, palette.getVibrantColor(-1));
                    }
                    if (tint == -1) {
                        tint = Colors.mix(windowBackgroundColor, palette.getMutedColor(-1));
                    }

                    if (tint != -1) {
                        listener.onTintColorGenerate(tint);
                    }
                });
    }

    private void tintViews(int tintColor) {
        // Animate transition of colors!
        Drawable linkDetailsContainerBackground = linkDetailsContainer.getBackground().mutate();
        animateColor(linkDetailsContainerBackgroundColor, tintColor, animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            linkDetailsContainerBackground.setTint(animatedValue);
        });

        if (Colors.isLight(tintColor)) {
            titleView.setTextColor(titleTextColorForDarkBackground);
            subtitleView.setTextColor(subtitleTextColorForDarkBackground);
        }
    }

    private void animateColor(int fromColor, int toColor, ValueAnimator.AnimatorUpdateListener updateListener) {
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(fromColor, toColor);
        colorAnimator.addUpdateListener(updateListener);
        colorAnimator.setDuration(TINT_TRANSITION_ANIMATION_DURATION);
        colorAnimator.setInterpolator(new FastOutSlowInInterpolator());
        colorAnimator.start();
    }

}
