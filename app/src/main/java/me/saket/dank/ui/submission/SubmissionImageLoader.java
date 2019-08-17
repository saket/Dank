package me.saket.dank.ui.submission;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.util.Size;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.SubmissionPreview;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.glide.GlidePaddingTransformation;
import me.saket.dank.walkthrough.SyntheticData;

public class SubmissionImageLoader {

  private final Lazy<Preference<NetworkStrategy>> hdMediaNetworkStrategyPref;
  private final Lazy<NetworkStateListener> networkStateListener;

  private final GlidePaddingTransformation glidePaddingTransformation;
  private final Size deviceDisplaySize;

  @Inject
  public SubmissionImageLoader(
      Application appContext,
      Lazy<NetworkStateListener> networkStateListener,
      @Named("hd_media_in_submissions") Lazy<Preference<NetworkStrategy>> hdMediaNetworkStrategyPref)
  {
    this.hdMediaNetworkStrategyPref = hdMediaNetworkStrategyPref;
    this.networkStateListener = networkStateListener;

    Resources res = appContext.getResources();
    deviceDisplaySize = new Size(res.getDisplayMetrics().widthPixels, res.getDisplayMetrics().heightPixels);

    // This transformation adds padding to images that are way too small (presently < 2 x toolbar height).
    glidePaddingTransformation = createGlidePaddingTransformation(appContext);
  }

  private GlidePaddingTransformation createGlidePaddingTransformation(Application appContext) {
    // getColor() with Application's context is not recommended, but I guess it's okay for this padding color.
    int paddingColorForSmallImages = ContextCompat.getColor(appContext, R.color.submission_media_content_background_padding);
    int minimumDesiredImageHeight = appContext.getResources().getDimensionPixelSize(R.dimen.submission_minimum_image_height);

    return new GlidePaddingTransformation(appContext, paddingColorForSmallImages) {
      @Override
      public Size getPadding(int imageWidth, int imageHeight) {
        // Because ZoomableImageView will resize the image to fill space.
        float widthResizeFactor = deviceDisplaySize.getWidth() / (float) imageWidth;
        float resizedHeight = imageHeight * widthResizeFactor;

        if (resizedHeight < minimumDesiredImageHeight) {
          // Image is too small to be displayed.
          return new Size(0, (int) ((minimumDesiredImageHeight - resizedHeight) / 2));
        } else {
          return new Size(0, 0);
        }
      }
    };
  }

  public Single<Drawable> load(
      Context context,
      MediaLink mediaLink,
      Optional<SubmissionPreview> redditPreviews,
      Optional<Scheduler> scheduler,
      RequestOptions options)
  {
    return hdMediaNetworkStrategyPref.get().asObservable()
        .switchMap(strategy -> networkStateListener.get().streamNetworkInternetCapability(strategy, scheduler))
        .firstOrError()
        .map(canLoadHighDef -> imageUrlSuitableForNetwork(mediaLink, redditPreviews, canLoadHighDef))
        .flatMap(imageUrl -> loadImage(context, imageUrl, options));
  }

  public Single<Drawable> load(
      Context context,
      MediaLink mediaLink,
      @Nullable SubmissionPreview redditPreviews,
      Scheduler scheduler,
      RequestOptions options)
  {
    return load(context, mediaLink, Optional.ofNullable(redditPreviews), Optional.of(scheduler), options);
  }

  public Single<Drawable> load(Context context, MediaLink mediaLink, @Nullable SubmissionPreview redditPreviews, RequestOptions options) {
    return load(context, mediaLink, Optional.ofNullable(redditPreviews), Optional.empty(), options);
  }

  public Single<Drawable> load(Context context, MediaLink mediaLink, RequestOptions options) {
    return load(context, mediaLink, Optional.empty(), Optional.empty(), options);
  }

  private String imageUrlSuitableForNetwork(MediaLink mediaLink, Optional<SubmissionPreview> redditPreviews, boolean canLoadHighDef) {
    if (canLoadHighDef) {
      return mediaLink.highQualityUrl();

    } else {
      // Images supplied by Reddit are static, so cannot optimize for GIFs.
      String defaultImageUrl = mediaLink.lowQualityUrl();
      return mediaLink.isGif()
          ? defaultImageUrl
          : ImageWithMultipleVariants.Companion.of(redditPreviews).findNearestFor(deviceDisplaySize.getWidth(), defaultImageUrl);
    }
  }

  public Single<Drawable> loadImage(Context context, String imageUrl, RequestOptions options) {
    if (SyntheticData.Companion.getSUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH().equalsIgnoreCase(imageUrl)) {
      //noinspection ConstantConditions
      return Single.just(context.getDrawable(R.drawable.dank_cat));
    }

    RequestOptions optionsWithTransform = options.transform(glidePaddingTransformation);
    RequestOptions optionsWithSample = applyDownsamplingStrategy(optionsWithTransform, deviceDisplaySize);

    return Single.create(emitter -> {
      //Timber.i("Loading image %s", imageUrl);
      FutureTarget<Drawable> target = Glide.with(context)
          .load(imageUrl)
          .apply(optionsWithSample)
          .submit();
      emitter.onSuccess(target.get());
    });
  }

  // TODO: Use this in MediaImageFragment to keep options in sync.
  public static RequestOptions applyDownsamplingStrategy(RequestOptions options, Size deviceDisplaySize) {
    return options
        .downsample(DownsampleStrategy.AT_LEAST)
        .override(deviceDisplaySize.getWidth(), deviceDisplaySize.getHeight());
  }
}
