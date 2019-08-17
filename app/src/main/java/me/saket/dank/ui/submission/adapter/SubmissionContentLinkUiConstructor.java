package me.saket.dank.ui.submission.adapter;

import static io.reactivex.schedulers.Schedulers.io;
import static io.reactivex.schedulers.Schedulers.single;
import static java.util.Arrays.asList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.urlparser.ExternalLink;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.LinkMetadata;
import me.saket.dank.urlparser.RedditLink;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.RedditUserLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import timber.log.Timber;

/**
 * Loads thumbnail, favicon and generates tint for a {@link Link}.
 */
public class SubmissionContentLinkUiConstructor {

  public static final boolean UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS = true;
  private static final TintDetails DEFAULT_TINT_DETAILS = TintDetails.create(Optional.of(Color.DKGRAY), R.color.submission_link_title, R.color.submission_link_byline);
  private static final boolean PROGRESS_VISIBLE = true;
  private static final boolean PROGRESS_HIDDEN = false;

  private final LinkMetadataRepository linkMetadataRepository;
  private final BitmapPool bitmapPool;
  private final Lazy<ErrorResolver> errorResolver;
  private final Map<Target, Drawable> targetsToDispose = new HashMap<>(8);

  @Inject
  public SubmissionContentLinkUiConstructor(LinkMetadataRepository linkMetadataRepository, BitmapPool bitmapPool, Lazy<ErrorResolver> errorResolver) {
    this.linkMetadataRepository = linkMetadataRepository;
    this.bitmapPool = bitmapPool;
    this.errorResolver = errorResolver;
  }

  /**
   * Warning: this method manages its own threading.
   * <p>
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

  /**
   * Since ImageView targets cannot be passed to Glide, the custom targets
   * are manually cleared so that the bitmaps get recycled.
   */
  public void clearGlideTargets(Context c) {
    Map<Target, Drawable> targets = new HashMap<>(targetsToDispose);
    targetsToDispose.clear();

    for (Map.Entry<Target, Drawable> entry : targets.entrySet()) {
      Target target = entry.getKey();
      Drawable value = entry.getValue();

      if (c instanceof Activity && ((Activity) c).isDestroyed()) {
        // Glide.with() crashes if the Activity is destroyed.
        if (value instanceof BitmapDrawable) {
          bitmapPool.put(((BitmapDrawable) value).getBitmap());

        } else if (value instanceof GifDrawable) {
          // TODO.
        }
      } else {
        Glide.with(c).clear(target);
      }
    }
  }

  private Observable<SubmissionContentLinkUiModel> streamLoadExternalLink(
      Context context,
      ExternalLink link,
      int windowBackgroundColor,
      ImageWithMultipleVariants redditSuppliedThumbnails)
  {
    Observable<LinkMetadata> sharedLinkMetadataStream = linkMetadataRepository.unfurl(link)
        .subscribeOn(io())
        .toObservable()
        .onErrorResumeNext(Observable.empty())
        .replay()
        .refCount();

    Observable<String> sharedTitleStream = fetchTitle(link, sharedLinkMetadataStream)
        .replay()
        .refCount();
    Observable<Optional<Drawable>> sharedFaviconStream = fetchFavicon(context, sharedLinkMetadataStream)
        .replay()
        .refCount();
    Observable<Optional<String>> thumbnailUrlStream = sharedLinkMetadataStream.map(linkMetadata -> Optional.ofNullable(linkMetadata.imageUrl()));
    Observable<Optional<Drawable>> sharedThumbnailStream = fetchThumbnail(context, redditSuppliedThumbnails, thumbnailUrlStream)
        .replay()
        .refCount();
    Observable<TintDetails> tintDetailsStream = streamTintDetails(link, windowBackgroundColor, sharedFaviconStream, sharedThumbnailStream);

    Observable<Boolean> progressVisibleStream = Completable
        .mergeDelayError(asList(
            sharedTitleStream.ignoreElements(),
            sharedFaviconStream.ignoreElements(),
            sharedThumbnailStream.ignoreElements()
        ))
        .andThen(Observable.just(PROGRESS_HIDDEN))
        .onErrorReturnItem(PROGRESS_HIDDEN)
        .startWith(PROGRESS_VISIBLE);

    // If neither thumbnail nor icon are present, this gets a default icon.
    // This is used only for showing the favicon and not for generating tint.
    Observable<Optional<Drawable>> faviconStreamWithDefault = Observable
        .combineLatest(sharedFaviconStream, sharedThumbnailStream.map(Optional::isPresent), Pair::create)
        .map(pair -> {
          Optional<Drawable> optionalFavicon = pair.first();
          boolean hasThumbnail = pair.second();

          //noinspection ConstantConditions
          if (optionalFavicon.isPresent() || hasThumbnail) {
            return optionalFavicon;
          } else {
            //noinspection ConstantConditions
            return Optional.of(context.getDrawable(R.drawable.ic_link_24dp));
          }
        })
        .startWith(Optional.empty());

    return Observable.combineLatest(
        sharedTitleStream,
        faviconStreamWithDefault,
        sharedThumbnailStream,
        tintDetailsStream,
        progressVisibleStream,
        (title, optionalFavicon, optionalThumbnail, tintDetails, progressVisible) -> {
          //noinspection ConstantConditions
          Optional<Integer> optionalFaviconBackground = optionalThumbnail.isPresent()
              ? Optional.of(R.drawable.background_submission_link_favicon_circle)
              : Optional.empty();

          return SubmissionContentLinkUiModel.builder()
              .title(title)
              .titleMaxLines(2)
              .titleTextColorRes(tintDetails.titleTextColorRes())
              .byline(Urls.parseDomainName(link.unparsedUrl()))
              .bylineTextColorRes(tintDetails.bylineTextColorRes())
              .icon(optionalFavicon)
              .iconBackgroundRes(optionalFaviconBackground)
              .thumbnail(optionalThumbnail)
              .progressVisible(progressVisible)
              .backgroundTintColor(tintDetails.backgroundTint())
              .link(link)
              .build();
        });
  }

  private Observable<SubmissionContentLinkUiModel> streamLoadRedditLink(Context context, RedditLink redditLink) {
    Observable<String> titleStream;
    Observable<String> bylineStream;
    Observable<Optional<Drawable>> thumbnailStream;
    Observable<Drawable> faviconStream;
    Observable<Boolean> progressVisibleStream;
    Observable<String> iconContentDescriptionStream;

    if (redditLink instanceof RedditSubredditLink) {
      titleStream = Observable.just(context.getString(R.string.subreddit_name_r_prefix, ((RedditSubredditLink) redditLink).name()));
      bylineStream = Observable.just(context.getString(R.string.submission_link_tap_to_open_subreddit));
      progressVisibleStream = Observable.just(false);
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = Observable.fromCallable(() -> context.getDrawable(R.drawable.ic_subreddits_24dp));
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_subreddit));

    } else if (redditLink instanceof RedditUserLink) {
      titleStream = Observable.just(context.getString(R.string.user_name_u_prefix, ((RedditUserLink) redditLink).name()));
      bylineStream = Observable.just(context.getString(R.string.submission_link_tap_to_open_profile));
      progressVisibleStream = Observable.just(false);
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = Observable.fromCallable(() -> context.getDrawable(R.drawable.ic_user_profile_24dp));
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_profile));

    } else if (redditLink instanceof RedditSubmissionLink) {
      if (!UNFURL_REDDIT_PAGES_AS_EXTERNAL_LINKS) {
        throw new UnsupportedOperationException();
      }
      RedditSubmissionLink submissionLink = (RedditSubmissionLink) redditLink;
      thumbnailStream = Observable.just(Optional.empty());
      faviconStream = Observable.fromCallable(() -> context.getDrawable(R.drawable.ic_submission_24dp));
      iconContentDescriptionStream = Observable.just(context.getString(R.string.submission_link_linked_submission));

      Observable<LinkMetadata> sharedLinkMetadataStream = linkMetadataRepository.unfurl(submissionLink)
          .subscribeOn(io())
          .toObservable()
          .onErrorResumeNext(Observable.empty())
          .replay()
          .refCount();

      progressVisibleStream = sharedLinkMetadataStream
          .ignoreElements()
          .onErrorComplete()
          .andThen(Observable.just(false))
          .startWith(true);

      titleStream = sharedLinkMetadataStream
          .observeOn(io())
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

      bylineStream = sharedLinkMetadataStream
          .map(linkMetadata -> context.getString(R.string.subreddit_name_r_prefix, submissionLink.subredditName()))
          .startWith(context.getString(R.string.submission_link_tap_to_open_submission));

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
                .iconBackgroundRes(Optional.empty())
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .link(redditLink)
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
    Observable<Optional<Drawable>> sharedThumbnailStream = fetchThumbnail(context, redditSuppliedThumbnails, albumThumbnailStream)
        .replay()
        .refCount();
    Observable<Optional<Drawable>> sharedFaviconStream = Observable.fromCallable(() -> context.getDrawable(R.drawable.ic_photo_library_24dp))
        .as(Optional.of())
        .replay()
        .refCount();

    Observable<TintDetails> tintDetailsStream = streamTintDetails(albumLink, windowBackgroundColor, sharedFaviconStream, sharedThumbnailStream);
    Observable<Boolean> progressVisibleStream = sharedThumbnailStream
        .map(o -> false)
        .onErrorReturnItem(false)
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
                .iconBackgroundRes(Optional.empty())
                .thumbnail(optionalThumbnail)
                .progressVisible(progressVisible)
                .backgroundTintColor(tintDetails.backgroundTint())
                .link(albumLink)
                .build()
    );
  }

  private Observable<String> fetchTitle(ExternalLink link, Observable<LinkMetadata> linkMetadataSingle) {
    return linkMetadataSingle
        .map(metadata -> metadata.title())
        .startWith(link.unparsedUrl());
  }

  /**
   * @param redditSuppliedThumbnails   Default source for images.
   * @param fallbackThumbnailUrlStream Fallback in case reddit didn't supply any images.
   */
  private Observable<Optional<Drawable>> fetchThumbnail(
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
                .flatMap(optionalUrl -> optionalUrl.isPresent() ? Observable.just(optionalUrl.get()) : Observable.empty());
          }
        })
        .observeOn(io())
        .flatMap(imageUrl -> {
          FutureTarget<Drawable> imageTarget = Glide.with(context)
              .load(imageUrl)
              .submit();
          return loadImage(imageTarget);
        })
        .map(image -> Optional.of(image))
        .startWith(Optional.empty());
  }

  private Observable<Optional<Drawable>> fetchFavicon(Context context, Observable<LinkMetadata> linkMetadataStream) {
    //noinspection ConstantConditions
    return linkMetadataStream
        .observeOn(io())
        .flatMap(metadata -> metadata.hasFavicon() ? Observable.just(metadata.faviconUrl()) : Observable.empty())
        .flatMap(faviconUrl -> {
          // Keep this context in sync with the one used in loadImage() for clearing this load on dispose.
          FutureTarget<Drawable> iconTarget = Glide.with(context)
              .load(faviconUrl)
              .apply(RequestOptions.bitmapTransform(GlideCircularTransformation.INSTANCE))
              .submit();
          return loadImage(iconTarget);
        })
        .map(favicon -> Optional.of(favicon))
        .startWith(Optional.empty());
  }

  private Observable<Drawable> loadImage(FutureTarget<Drawable> futureTarget) {
    return Observable
        .<Drawable>create(emitter -> {
          Drawable drawable = futureTarget.get();
          //noinspection ConstantConditions
          targetsToDispose.put(futureTarget, drawable);

          emitter.onNext(drawable);
          emitter.onComplete();
        })
        .onErrorResumeNext(error -> {
          ResolvedError resolvedError = errorResolver.get().resolve(error);
          resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't load image using glide"));
          return Observable.empty();
        });
  }

  private Observable<TintDetails> streamTintDetails(
      Link link,
      int windowBackgroundColor,
      Observable<Optional<Drawable>> sharedFaviconStream,
      Observable<Optional<Drawable>> sharedThumbnailStream)
  {
    boolean isGooglePlayThumbnail = UrlParser.isGooglePlayUrl(Uri.parse(link.unparsedUrl()));

    return Observable.concat(sharedThumbnailStream, sharedFaviconStream)
        .filter(Optional::isPresent)
        .take(1)
        .map(imageOptional -> imageOptional.get())
        .observeOn(single())
        .flatMapSingle(image -> generateTint(image, isGooglePlayThumbnail, windowBackgroundColor))
        .startWith(DEFAULT_TINT_DETAILS);
  }

  private Single<TintDetails> generateTint(Drawable drawable, boolean isGooglePlayThumbnail, int windowBackgroundColor) {
    return bitmapFromDrawable(drawable)
        .flatMap(bitmap -> Single.fromCallable(() -> Palette.from(bitmap)
            .maximumColorCount(Integer.MAX_VALUE)    // Don't understand why, but this changes the darkness of the colors.
            .generate()
        ))
        .map(palette -> {
          int tint = -1;
          if (isGooglePlayThumbnail) {
            tint = palette.getLightVibrantColor(-1);
          }
          if (tint == -1 && palette.getDarkMutedColor(-1) != -1 && palette.getMutedColor(-1) != -1) {
            tint = Colors.mix(palette.getMutedColor(-1), palette.getDarkMutedColor(-1));
          }
          if (tint == -1) {
            // Mix the color with the window's background color to neutralize possibly strong colors.
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

  private Single<Bitmap> bitmapFromDrawable(Drawable drawable) {
    return Single.create(emitter -> {
      if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
        throw new AssertionError();
      }

      Bitmap bitmap = bitmapPool.get(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
      Canvas canvas = new Canvas(bitmap);
      drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
      drawable.draw(canvas);

      emitter.onSuccess(bitmap);
      emitter.setCancellable(() -> bitmapPool.put(bitmap));
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
      return new AutoValue_SubmissionContentLinkUiConstructor_TintDetails(backgroundTint, titleTextColorRes, bylineTextColorRes);
    }
  }
}
