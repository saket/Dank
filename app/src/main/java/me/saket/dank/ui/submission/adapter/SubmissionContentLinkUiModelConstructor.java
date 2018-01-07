package me.saket.dank.ui.submission.adapter;

import static java.util.Arrays.asList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.google.auto.value.AutoValue;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.links.ExternalLink;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.LinkMetadata;
import me.saket.dank.data.links.RedditLink;
import me.saket.dank.data.links.RedditSubmissionLink;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import timber.log.Timber;

/**
 * Loads thumbnail, favicon and generates tint for a {@link Link}.
 */
public class SubmissionContentLinkUiModelConstructor {

  private static final TintDetails DEFAULT_TINT_DETAILS = TintDetails.create(Optional.empty(), R.color.submission_link_title, R.color.submission_link_byline);
  public static final boolean UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS = true;
  private static final boolean PROGRESS_VISIBLE = true;
  private static final boolean PROGRESS_HIDDEN = false;
  private final LinkMetadataRepository linkMetadataRepository;

  @Inject
  public SubmissionContentLinkUiModelConstructor(LinkMetadataRepository linkMetadataRepository) {
    this.linkMetadataRepository = linkMetadataRepository;
  }

  /**
   * TODO: Content description.
   * <p>
   * Emits multiple times:
   * - Initially, with unparsed values.
   * - When title is loaded.
   * - When favicon is loaded.
   * - When thumbnail is loaded.
   * - When tint is generated.
   */
  public Observable<SubmissionContentLinkUiModel> streamLoad(Context context, Link link, ImageWithMultipleVariants redditSuppliedThumbnails) {
    int windowBackgroundColor = ContextCompat.getColor(context, R.color.window_background);

    if (link.isExternal()) {
      return streamLoadExternalLink(context, (ExternalLink) link, windowBackgroundColor, redditSuppliedThumbnails);

    } else if (link.isRedditPage()) {
      return streamLoadRedditLink(context, ((RedditLink) link));

    } else if (link.isMediaAlbum() && link instanceof ImgurAlbumLink) {
      return streamLoadImgurAlbum(context, ((ImgurAlbumLink) link), windowBackgroundColor, redditSuppliedThumbnails);

    } else {
      throw new AssertionError("Unknown link: " + link);
    }
  }

  private Observable<SubmissionContentLinkUiModel> streamLoadExternalLink(
      Context context,
      ExternalLink link,
      int windowBackgroundColor,
      ImageWithMultipleVariants redditSuppliedThumbnails)
  {
    Observable<LinkMetadata> sharedLinkMetadataStream = linkMetadataRepository.unfurl(link)
        .subscribeOn(Schedulers.io())
        .toObservable()
        .onErrorResumeNext(Observable.empty())
        .share();

    Observable<String> sharedTitleStream = fetchTitle(link, sharedLinkMetadataStream);
    Observable<Optional<Bitmap>> sharedFaviconStream = fetchFavicon(context, sharedLinkMetadataStream).share();
    Observable<Optional<String>> thumbnailUrlStream = sharedLinkMetadataStream.map(linkMetadata -> Optional.ofNullable(linkMetadata.imageUrl()));
    Observable<Optional<Bitmap>> sharedThumbnailStream = fetchThumbnail(context, redditSuppliedThumbnails, thumbnailUrlStream).share();
    Observable<TintDetails> tintDetailsStream = streamTintDetails(link, windowBackgroundColor, sharedFaviconStream, sharedThumbnailStream);

    Observable<Boolean> progressVisibleStream = Completable
        .mergeDelayError(asList(
            sharedTitleStream.ignoreElements(),
            // take(2): Not sure why, but sometimes this stream never completes. This is sort of a hack to force complete.
            sharedFaviconStream.take(2).ignoreElements(),
            sharedThumbnailStream.ignoreElements()
        ))
        .andThen(Observable.just(PROGRESS_HIDDEN))
        .onErrorReturnItem(PROGRESS_HIDDEN)
        .startWith(PROGRESS_VISIBLE);

    return Observable.combineLatest(
        sharedTitleStream,
        sharedFaviconStream,
        sharedThumbnailStream,
        tintDetailsStream,
        progressVisibleStream,
        (title, optionalFavicon, optionalThumbnail, tintDetails, progressVisible) ->
            SubmissionContentLinkUiModel.builder()
                .title(title)
                .titleMaxLines(2)
                .titleTextColorRes(tintDetails.titleTextColorRes())
                .byline(Urls.parseDomainName(link.unparsedUrl()))
                .bylineTextColorRes(tintDetails.bylineTextColorRes())
                .icon(optionalFavicon)
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .build());
  }

  private Observable<SubmissionContentLinkUiModel> streamLoadRedditLink(Context context, RedditLink redditLink) {
    Observable<String> titleStream;
    Observable<String> bylineStream;
    Observable<Optional<Bitmap>> thumbnailStream;
    Observable<Bitmap> faviconStream;
    Observable<Boolean> progressVisibleStream;
    Observable<String> iconContentDescriptionStream;

    if (redditLink instanceof RedditSubredditLink) {
      titleStream = Observable.just(context.getString(R.string.subreddit_name_r_prefix, ((RedditSubredditLink) redditLink).name()));
      bylineStream = Observable.just(context.getString(R.string.submission_link_tap_to_open_subreddit));
      progressVisibleStream = Observable.just(false);
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = loadBitmapFromResource(context, R.drawable.ic_subreddits_24dp);
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_subreddit));

    } else if (redditLink instanceof RedditUserLink) {
      titleStream = Observable.just(context.getString(R.string.user_name_u_prefix, ((RedditUserLink) redditLink).name()));
      bylineStream = Observable.just(context.getString(R.string.submission_link_tap_to_open_profile));
      progressVisibleStream = Observable.just(false);
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = loadBitmapFromResource(context, R.drawable.ic_user_profile_24dp);
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_profile));

    } else if (redditLink instanceof RedditSubmissionLink) {
      if (!UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS) {
        throw new UnsupportedOperationException();
      }
      RedditSubmissionLink submissionLink = (RedditSubmissionLink) redditLink;
      bylineStream = Observable.just(context.getString(R.string.submission_link_tap_to_open_submission));
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = loadBitmapFromResource(context, R.drawable.ic_submission_24dp);
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_submission));

      Observable<LinkMetadata> sharedLinkMetadataStream = linkMetadataRepository.unfurl(submissionLink)
          .subscribeOn(Schedulers.io())
          .toObservable()
          .share();

      progressVisibleStream = sharedLinkMetadataStream
          .map(o -> false)
          .startWith(true);

      titleStream = sharedLinkMetadataStream
          .observeOn(Schedulers.io())
          .map(linkMetadata -> {
            String submissionPageTitle = linkMetadata.title();
            if (submissionPageTitle != null) {
              // Reddit prefixes page titles with the linked commentator's name (if any). We don't want that.
              String prefix = "comments on ";
              int userNameSeparatorIndex = submissionPageTitle.indexOf(prefix);
              if (userNameSeparatorIndex > -1) {
                submissionPageTitle = submissionPageTitle.substring(userNameSeparatorIndex + prefix.length());
              }
            }
            return submissionPageTitle;
          })
          .startWith(redditLink.unparsedUrl());

    } else {
      throw new UnsupportedOperationException("Unknown reddit link: " + redditLink);
    }

    return Observable.combineLatest(
        titleStream,
        bylineStream,
        faviconStream,
        thumbnailStream,
        Observable.just(DEFAULT_TINT_DETAILS),
        progressVisibleStream,
        (title, byline, favicon, optionalThumbnail, tintDetails, progressVisible) ->
            SubmissionContentLinkUiModel.builder()
                .title(title)
                .titleMaxLines(2)
                .titleTextColorRes(tintDetails.titleTextColorRes())
                .byline(byline)
                .bylineTextColorRes(tintDetails.bylineTextColorRes())
                .icon(Optional.of(favicon))
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .build()
    );
  }

  public Observable<SubmissionContentLinkUiModel> streamLoadImgurAlbum(
      Context context,
      ImgurAlbumLink albumLink,
      int windowBackgroundColor,
      ImageWithMultipleVariants redditSuppliedThumbnails)
  {
    Observable<String> iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_imgur_gallery));

    //noinspection ConstantConditions
    Observable<String> titleStream = Observable.just(
        albumLink.hasAlbumTitle()
            ? albumLink.albumTitle()
            : context.getString(R.string.submission_image_album_title));

    Observable<String> bylineStream = Observable.just(
        albumLink.hasAlbumTitle()
            ? context.getString(R.string.submission_image_album_label_with_image_count, albumLink.images().size())
            : context.getString(R.string.submission_image_album_image_count, albumLink.images().size()));

    Observable<Optional<String>> albumThumbnailStream = Observable.just(Optional.of(albumLink.coverImageUrl()));
    Observable<Optional<Bitmap>> sharedThumbnailStream = fetchThumbnail(context, redditSuppliedThumbnails, albumThumbnailStream).share();
    Observable<Optional<Bitmap>> sharedFaviconStream = loadBitmapFromResource(context, R.drawable.ic_photo_library_24dp)
        .as(Optional.of())
        .share();

    Observable<TintDetails> tintDetailsStream = streamTintDetails(albumLink, windowBackgroundColor, sharedFaviconStream, sharedThumbnailStream);
    Observable<Boolean> progressVisibleStream = sharedThumbnailStream
        .map(o -> false)
        .startWith(true);

    return Observable.combineLatest(
        titleStream,
        bylineStream,
        sharedFaviconStream,
        sharedThumbnailStream,
        tintDetailsStream,
        progressVisibleStream,
        (title, byline, optionalFavicon, optionalThumbnail, tintDetails, progressVisible) ->
            SubmissionContentLinkUiModel.builder()
                .title(title)
                .titleMaxLines(2)
                .titleTextColorRes(tintDetails.titleTextColorRes())
                .byline(byline)
                .bylineTextColorRes(tintDetails.bylineTextColorRes())
                .icon(optionalFavicon)
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .build()
    );
  }

  private Observable<Bitmap> loadBitmapFromResource(Context context, @DrawableRes int drawableRes) {
    return Observable.fromCallable(() -> {
      Drawable drawable = context.getDrawable(drawableRes);
      //noinspection ConstantConditions
      if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
        throw new AssertionError();
      }
      Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(bitmap);
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
      drawable.draw(canvas);
      return bitmap;
    });
  }

  private Observable<String> fetchTitle(ExternalLink link, Observable<LinkMetadata> linkMetadataSingle) {
    return linkMetadataSingle
        .observeOn(Schedulers.io())
        .map(metadata -> metadata.title())
        .startWith(link.unparsedUrl())
        .share();
  }

  /**
   * @param redditSuppliedThumbnails   Default source for images.
   * @param fallbackThumbnailUrlStream Fallback in case reddit didn't supply any images.
   */
  private Observable<Optional<Bitmap>> fetchThumbnail(
      Context context,
      ImageWithMultipleVariants redditSuppliedThumbnails,
      Observable<Optional<String>> fallbackThumbnailUrlStream)
  {
    return Observable.just(redditSuppliedThumbnails.isNonEmpty())
        .flatMap(hasRedditSuppliedImages -> {
          if (hasRedditSuppliedImages) {
            int thumbnailWidth = SubmissionCommentsHeader.getWidthForAlbumContentLinkThumbnail(context);
            return Observable.just(redditSuppliedThumbnails.findNearestFor(thumbnailWidth));
          } else {
            return fallbackThumbnailUrlStream
                .observeOn(Schedulers.io())
                .flatMap(optionalUrl ->
                    optionalUrl.isPresent()
                        ? Observable.just(optionalUrl.get())
                        : Observable.empty()
                );
          }
        })
        .flatMap(imageUrl -> {
          FutureTarget<Bitmap> imageTarget = Glide.with(context)
              .asBitmap()
              .load(imageUrl)
              .submit();
          return loadImage(context, imageTarget);
        })
        .delay(BuildConfig.DEBUG ? 2 : 0, TimeUnit.SECONDS)   // TODO: REMOVEEEE!!
        .map(image -> Optional.of(image))
        .startWith(Optional.empty());
  }

  private Observable<Optional<Bitmap>> fetchFavicon(Context context, Observable<LinkMetadata> linkMetadataStream) {
    //noinspection ConstantConditions
    return linkMetadataStream
        .observeOn(Schedulers.io())
        .flatMap(metadata -> metadata.hasFavicon() ? Observable.just(metadata.faviconUrl()) : Observable.empty())
        .flatMap(faviconUrl -> {
          // Keep this context in sync with the one used in loadImage() for clearing this load on dispose.
          FutureTarget<Bitmap> iconTarget = Glide.with(context)
              .asBitmap()
              .load(faviconUrl)
              .apply(RequestOptions.bitmapTransform(GlideCircularTransformation.INSTANCE))
              .submit();
          return loadImage(context, iconTarget);
        })
        .map(favicon -> Optional.of(favicon))
        .startWith(Optional.empty());
  }

  private Observable<Bitmap> loadImage(Context context, FutureTarget<Bitmap> futureTarget) {
    return Observable
        .<Bitmap>create(emitter -> {
          emitter.onNext(futureTarget.get());
          emitter.onComplete();
          emitter.setCancellable(() -> Glide.with(context).clear(futureTarget));
        })
        .onErrorResumeNext(error -> {
          Timber.e(error.getMessage());
          return Observable.empty();
        });
  }

  private Observable<TintDetails> streamTintDetails(
      Link link,
      int windowBackgroundColor,
      Observable<Optional<Bitmap>> sharedFaviconStream,
      Observable<Optional<Bitmap>> sharedThumbnailStream)
  {
    boolean isGooglePlayThumbnail = UrlParser.isGooglePlayUrl(Uri.parse(link.unparsedUrl()));

    return Observable.concat(sharedThumbnailStream, sharedFaviconStream)
        .filter(imageOptional -> imageOptional.isPresent())
        .take(1)
        .map(imageOptional -> imageOptional.get())
        .flatMapSingle(image -> generateTint(image, isGooglePlayThumbnail, windowBackgroundColor))
        .startWith(DEFAULT_TINT_DETAILS);
  }

  private Single<TintDetails> generateTint(Bitmap bitmap, boolean isGooglePlayThumbnail, int windowBackgroundColor) {
    return Single.fromCallable(() -> Palette.from(bitmap)
        .maximumColorCount(Integer.MAX_VALUE)    // Don't understand why, but this changes the darkness of the colors.
        .generate())
        .map(palette -> {
          int tint = -1;
          if (isGooglePlayThumbnail) {
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
          return tint != -1
              ? Optional.of(tint)
              : Optional.<Integer>empty();
        })
        .map(tintColorOptional -> {
          // Inverse title and byline colors when the background tint is light.
          boolean isLightBackgroundTint = tintColorOptional.isPresent() && Colors.isLight(tintColorOptional.get());
          int titleColorRes = isLightBackgroundTint
              ? R.color.submission_link_title_light_background
              : R.color.submission_link_title;
          int bylineColorRes = isLightBackgroundTint
              ? R.color.submission_link_byline_light_background
              : R.color.submission_link_byline;
          return TintDetails.create(tintColorOptional, titleColorRes, bylineColorRes);
        });
  }

  @AutoValue
  abstract static class TintDetails {

    public abstract Optional<Integer> backgroundTint();

    @ColorRes
    public abstract int titleTextColorRes();

    @ColorRes
    public abstract int bylineTextColorRes();

    public static TintDetails create(Optional<Integer> backgroundTint, @ColorRes int titleTextColorRes, @ColorRes int bylineTextColorRes) {
      return new AutoValue_SubmissionContentLinkUiModelConstructor_TintDetails(backgroundTint, titleTextColorRes, bylineTextColorRes);
    }

  }
}
