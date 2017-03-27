package me.saket.dank.ui.submission;

import static me.saket.dank.utils.Images.drawableToBitmap;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinishSingle;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.setDimensions;

import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.Locale;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.data.LinkMetadata;
import me.saket.dank.data.RedditLink;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.GlideCircularTransformation;
import me.saket.dank.utils.GlideUtils;
import me.saket.dank.utils.UrlMetadataParser;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.SubmissionAnimatedProgressBar;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Used when a submission points to a Reddit hosted URL, which can be another submission or a subreddit or a user.
 * Manages Views for showing details of the linked URL.
 */
public class SubmissionLinkDetailsViewHolder {

    @BindView(R.id.submission_link_icon_container) ViewGroup iconContainer;
    @BindView(R.id.submission_link_thumbnail) ImageView thumbnailView;
    @BindView(R.id.submission_link_icon) ImageView iconView;
    @BindView(R.id.submission_link_title) TextView titleView;
    @BindView(R.id.submission_link_subtitle) TextView subtitleView;
    @BindView(R.id.submission_link_progress) SubmissionAnimatedProgressBar progressView;

    @BindDimen(R.dimen.submission_link_icon_width_without_thumbnail) int iconWidthWithoutThumbnailPx;
    @BindDimen(R.dimen.submission_link_icon_width_with_thumbnail) int iconWidthWithThumbnailPx;
    @BindDimen(R.dimen.submission_link_favicon_min_size) int faviconMinSizePx;
    @BindDimen(R.dimen.submission_link_favicon_max_size) int faviconMaxSizePx;

    @BindColor(R.color.window_background) int windowBackgroundColor;
    @BindColor(R.color.submission_link_background_color) int linkDetailsContainerBackgroundColor;
    @BindColor(R.color.gray_400) int redditLinkIconTintColor;
    @BindColor(R.color.submission_link_title) int titleTextColor;
    @BindColor(R.color.submission_link_subtitle) int subtitleTextColor;
    @BindColor(R.color.submission_link_title_dark) int titleTextColorForDarkBackground;
    @BindColor(R.color.submission_link_subtitle_dark) int subtitleTextColorForDarkBackground;

    private ViewGroup linkDetailsContainer;

    public SubmissionLinkDetailsViewHolder(ViewGroup linkedRedditLinkView) {
        this.linkDetailsContainer = linkedRedditLinkView;
        ButterKnife.bind(this, linkedRedditLinkView);
        linkedRedditLinkView.setClipToOutline(true);
    }

    public void setVisible(boolean visible) {
        linkDetailsContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void resetViews() {
        // Stop loading of any pending images.
        Glide.clear(thumbnailView);
        Glide.clear(iconView);

        linkDetailsContainer.setBackgroundTintList(null);
        thumbnailView.setImageDrawable(null);
        iconView.setImageDrawable(null);
        iconView.setBackground(null);
        setDimensions(iconView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        titleView.setMaxLines(1);
        titleView.setTextColor(titleTextColor);
        subtitleView.setTextColor(subtitleTextColor);

        linkDetailsContainer.setBackgroundResource(R.drawable.background_submission_link);
    }

    /**
     * Load and show title of user/subreddit/submission.
     */
    public Subscription populate(RedditLink redditLink) {
        resetViews();
        Views.setWidth(iconContainer, iconWidthWithoutThumbnailPx);
        Resources resources = titleView.getResources();
        iconView.setImageTintList(ColorStateList.valueOf(redditLinkIconTintColor));

        if (redditLink instanceof RedditLink.Subreddit) {
            iconView.setContentDescription(resources.getString(R.string.submission_link_linked_subreddit));
            iconView.setImageResource(R.drawable.ic_subreddits_24dp);
            titleView.setText(resources.getString(R.string.subreddit_name_r_prefix, ((RedditLink.Subreddit) redditLink).name));
            subtitleView.setText(R.string.submission_link_tap_to_open_subreddit);
            progressView.setVisibility(View.GONE);
            return Subscriptions.unsubscribed();

        } else if (redditLink instanceof RedditLink.User) {
            iconView.setContentDescription(resources.getString(R.string.submission_link_linked_profile));
            iconView.setImageResource(R.drawable.ic_user_profile_24dp);
            titleView.setText(resources.getString(R.string.user_name_u_prefix, ((RedditLink.User) redditLink).name));
            subtitleView.setText(R.string.submission_link_tap_to_open_profile);
            progressView.setVisibility(View.GONE);
            return Subscriptions.unsubscribed();

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

    private Subscription populateSubmissionTitle(RedditLink.Submission submissionLink) {
        // Downloading the page's HTML to get the title is faster than getting the submission's data from the API.
        return UrlMetadataParser.parse(submissionLink.url, true)
                .compose(applySchedulersSingle())
                .compose(doOnStartAndFinishSingle(start -> progressView.setVisibility(start ? View.VISIBLE : View.GONE)))
                .subscribe(linkMetadata -> {
                    String submissionPageTitle = linkMetadata.title();

                    if (!TextUtils.isEmpty(submissionPageTitle)) {
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

    /**
     * Show information of an external link. Extracts meta-data from the URL to get the favicon and the title.
     */
    public Subscription populate(Link.External externalLink) {
        resetViews();
        Views.setWidth(iconContainer, iconWidthWithThumbnailPx);

        Resources resources = titleView.getResources();
        thumbnailView.setContentDescription(null);
        iconView.setContentDescription(resources.getString(R.string.submission_link_linked_url));
        iconView.setImageTintList(ColorStateList.valueOf(redditLinkIconTintColor));
        iconView.setImageResource(R.drawable.ic_link_black_24dp);
        titleView.setText(externalLink.url);
        subtitleView.setText(Urls.parseDomainName(externalLink.url));

        progressView.setVisibility(View.VISIBLE);

        return UrlMetadataParser.parse(externalLink.url, false)
                .compose(applySchedulersSingle())
                .doOnError(__ -> progressView.setVisibility(View.GONE))
                .subscribe(linkMetadata -> {
                    titleView.setMaxLines(Integer.MAX_VALUE);
                    //noinspection deprecation
                    titleView.setText(Html.fromHtml(linkMetadata.title()));
                    thumbnailView.setContentDescription(titleView.getText());

                    if (linkMetadata.hasImage()) {
                        loadLinkThumbnail(externalLink, linkMetadata);
                    }
                    loadLinkFavicon(externalLink, linkMetadata, linkMetadata.hasImage());

                }, logError("Couldn't get link's meta-data: " + externalLink.url));
    }

    private void loadLinkThumbnail(Link.External externalLink, LinkMetadata linkMetadata) {
        // Resize down large images.
        Uri imageURI = Uri.parse(linkMetadata.imageUrl());
        String linkImageUrl = String.format(Locale.ENGLISH,
                "https://rsz.io/%s?width=%d",
                imageURI.getHost() + imageURI.getPath(), iconWidthWithThumbnailPx
        );

        Glide.with(thumbnailView.getContext())
                .load(linkImageUrl)
                .listener(new GlideUtils.SimpleRequestListener<String, GlideDrawable>() {
                    @Override
                    public void onException(Exception e) {
                        if (linkMetadata.hasFavicon()) {
                            loadLinkFavicon(externalLink, linkMetadata, false);
                        }
                    }
                })
                .crossFade()
                .into(new ImageViewTarget<GlideDrawable>(thumbnailView) {
                    @Override
                    protected void setResource(GlideDrawable resource) {
                        generateTintColorFromImage(
                                externalLink.url,
                                resource,
                                tintColor -> {
                                    if (!UrlMetadataParser.isGooglePlayLink(externalLink.url)) {
                                        thumbnailView.setColorFilter(Colors.applyAlpha(tintColor, 0.4f));
                                        thumbnailView.setImageDrawable(resource);
                                    }
                                    tintViews(tintColor);
                                });
                        progressView.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * @param isThumbnailPresent When true, tinting of the Views will be done using the favicon and the progress bar
     *                           will be shown while the favicon is fetched. This is used when the thumbnail couldn't
     *                           be loaded.
     */
    private void loadLinkFavicon(Link.External externalLink, LinkMetadata linkMetadata, boolean isThumbnailPresent) {
        iconView.setImageTintList(null);

        if (!isThumbnailPresent) {
            progressView.setVisibility(View.VISIBLE);
        }

        Glide.with(iconView.getContext())
                .load(linkMetadata.faviconUrl())
                .crossFade()
                .listener(new GlideUtils.SimpleRequestListener<String, GlideDrawable>() {
                    @Override
                    public void onResourceReady(GlideDrawable resource) {
                        if (!isThumbnailPresent) {
                            // Tint with favicon in case the thumbnail couldn't be fetched.
                            generateTintColorFromImage(externalLink.url, resource, tintColor -> tintViews(tintColor));
                            progressView.setVisibility(View.GONE);
                        }

                        iconView.setVisibility(View.VISIBLE);
                        iconView.setBackgroundResource(R.drawable.background_submission_link_favicon_circle);
                    }

                    @Override
                    public void onException(Exception e) {
                        if (!isThumbnailPresent) {
                            progressView.setVisibility(View.GONE);
                        }
                    }
                })
                .bitmapTransform(new GlideCircularTransformation(iconView.getContext()))
                .into(new ImageViewTarget<GlideDrawable>(iconView) {
                    @Override
                    protected void setResource(GlideDrawable resource) {
                        iconView.setImageDrawable(resource);

                        if (resource.getIntrinsicWidth() < faviconMinSizePx) {
                            setDimensions(iconView, faviconMinSizePx, faviconMinSizePx);

                        } else if (resource.getIntrinsicWidth() > faviconMaxSizePx) {
                            setDimensions(iconView, faviconMaxSizePx, faviconMaxSizePx);
                        }
                    }
                });
    }

    private interface OnTintColorGenerateListener {
        void onTintColorGenerate(int tintColor);
    }

    private void generateTintColorFromImage(String url, Drawable resource, OnTintColorGenerateListener listener) {
        Palette.from(drawableToBitmap(resource))
                .maximumColorCount(Integer.MAX_VALUE)    // Don't understand why, but this changes the darkness of the colors.
                .generate(palette -> {
                    int tint = -1;
                    if (UrlMetadataParser.isGooglePlayLink(url)) {
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
        colorAnimator.setDuration(300L);
        colorAnimator.setInterpolator(new FastOutSlowInInterpolator());
        colorAnimator.start();
    }

}
