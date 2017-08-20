package me.saket.dank.ui.media;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.google.common.io.Files;
import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.MediaDownloadService;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.SystemUiHelper;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

public class MediaAlbumViewerActivity extends DankActivity
    implements MediaFragmentCallbacks, FlickGestureListener.GestureCallbacks, SystemUiHelper.OnSystemUiVisibilityChangeListener
{

  @BindView(R.id.mediaalbumviewer_root) ViewGroup rootLayout;
  @BindView(R.id.mediaalbumviewer_pager) ViewPager mediaAlbumPager;
  @BindView(R.id.mediaalbumviewer_options_container) ViewGroup optionButtonsContainer;
  @BindView(R.id.mediaalbumviewer_share) ImageButton shareButton;
  @BindView(R.id.mediaalbumviewer_download) ImageButton downloadButton;
  @BindView(R.id.mediaalbumviewer_open_in_browser) ImageButton openInBrowserButton;
  @BindView(R.id.mediaalbumviewer_reload_in_hd) ImageButton reloadInHighDefButton;

  private SystemUiHelper systemUiHelper;
  private Drawable activityBackgroundDrawable;
  private MediaAlbumPagerAdapter mediaAlbumAdapter;
  private Set<MediaAlbumItem> mediaItemsWithHighDefEnabled = new HashSet<>();

  public static void start(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaAlbumViewerActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_media_album_viewer);
    ButterKnife.bind(this);

    List<MediaAlbumItem> mediaLinks = new ArrayList<>();
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/WhGHrBE.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/01v3bw0.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.imgur.com/rQ7IogD.gifv")));
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://streamable.com/p9by5")));
    mediaAlbumAdapter = new MediaAlbumPagerAdapter(getSupportFragmentManager(), mediaLinks);
    mediaAlbumPager.setAdapter(mediaAlbumAdapter);

    Timber.i("Parsed links:");
    for (MediaAlbumItem item : mediaLinks) {
      Timber.i("%s, isVideo? %s", item.mediaLink().originalUrl(), item.mediaLink().isVideo());
    }

    // TODO: Show media options only when we have adapter data.

    systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, this);

    // Fade in background.
    activityBackgroundDrawable = rootLayout.getBackground().mutate();
    rootLayout.setBackground(activityBackgroundDrawable);
    ValueAnimator fadeInAnimator = ObjectAnimator.ofFloat(1f, 0f);
    fadeInAnimator.setDuration(300);
    fadeInAnimator.setInterpolator(Animations.INTERPOLATOR);
    fadeInAnimator.addUpdateListener(animation -> {
      float transparencyFactor = (float) animation.getAnimatedValue();
      updateBackgroundDimmingAlpha(transparencyFactor);
    });
    fadeInAnimator.start();

    rootLayout.setOnApplyWindowInsetsListener((view, windowInsets) -> {
      int navBarHeight = windowInsets.getSystemWindowInsetBottom();
      Views.setPaddingBottom(optionButtonsContainer, navBarHeight);
      return windowInsets.consumeStableInsets();
    });
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    unsubscribeOnDestroy(
        RxViewPager.pageSelections(mediaAlbumPager)
            .map(currentItem -> mediaAlbumAdapter.getDataSet().get(currentItem))
            .doOnNext(activeMediaItem -> updateContentDescriptionOfOptionButtonsAccordingTo(activeMediaItem))
            .doOnNext(activeMediaItem -> enableHighDefButtonIfPossible(activeMediaItem))
            .subscribe()
    );
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, R.anim.fade_out);
  }

// ======== MEDIA FRAGMENT ======== //

  @Override
  public void onClickMediaItem() {
    systemUiHelper.toggle();
  }

  @Override
  public int getDeviceDisplayWidth() {
    return getResources().getDisplayMetrics().widthPixels;
  }

  @Override
  public void onFlickDismissEnd(long flickAnimationDuration) {
    unsubscribeOnDestroy(
        Observable.timer(flickAnimationDuration, TimeUnit.MILLISECONDS)
            .doOnSubscribe(o -> animateMediaOptionsVisibility(false, Animations.INTERPOLATOR, 100))
            .subscribe(o -> finish())
    );
  }

  @Override
  public void onMoveMedia(@FloatRange(from = -1, to = 1) float moveRatio) {
    updateBackgroundDimmingAlpha(Math.abs(moveRatio));
  }

  /**
   * @param targetTransparencyFactor 1f for maximum transparency. 0f for none.
   */
  private void updateBackgroundDimmingAlpha(@FloatRange(from = -1, to = 1) float targetTransparencyFactor) {
    // Increase dimming exponentially so that the background is fully transparent while the image has been moved by half.
    float dimming = 1f - Math.min(1f, targetTransparencyFactor * 2);
    activityBackgroundDrawable.setAlpha((int) (dimming * 255));
  }

  @Override
  public void onSystemUiVisibilityChange(boolean systemUiVisible) {
    TimeInterpolator interpolator = systemUiVisible ? new DecelerateInterpolator(2f) : new AccelerateInterpolator(2f);
    long animationDuration = 300;
    animateMediaOptionsVisibility(systemUiVisible, interpolator, animationDuration);
  }

// ======== MEDIA OPTIONS ======== //

  void updateContentDescriptionOfOptionButtonsAccordingTo(MediaAlbumItem activeMediaItem) {
    if (activeMediaItem.mediaLink().isVideo()) {
      shareButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_share_video));
      downloadButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_download_video));
      openInBrowserButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_open_video_in_browser));
      reloadInHighDefButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_reload_video_in_high_def));

    } else {
      shareButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_share_image));
      downloadButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_download_image));
      openInBrowserButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_open_image_in_browser));
      reloadInHighDefButton.setContentDescription(getString(R.string.cd_mediaalbumviewer_reload_image_in_high_def));
    }
  }

  @OnClick(R.id.mediaalbumviewer_share)
  void onClickShareMedia() {
    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
    boolean isVideo = activeMediaItem.mediaLink().isVideo();

    PopupMenu shareMenu = new PopupMenu(this, shareButton);
    shareMenu.getMenu().add(isVideo ? R.string.mediaalbumviewer_share_video : R.string.mediaalbumviewer_share_image);
    shareMenu.getMenu().add(isVideo ? R.string.mediaalbumviewer_share_video_link : R.string.mediaalbumviewer_share_image_link);
    shareMenu.setOnMenuItemClickListener(item -> {
      if (item.getTitle().equals(getString(R.string.mediaalbumviewer_share_image))) {
        unsubscribeOnDestroy(
            findHighestResImageFileFromCache(activeMediaItem)
                .map(imageFile -> {
                  // Certain apps like Messaging fail to parse images if there's no file format,
                  // so we'll have to create a copy. ".jpg" seems to also work for gifs.
                  String mediaFileName = Urls.parseFileNameWithExtension(activeMediaItem.mediaLink().originalUrl());
                  File imageFileCopy = new File(imageFile.getParent(), mediaFileName);
                  Files.copy(imageFile, imageFileCopy);
                  return imageFileCopy;
                })
                .compose(RxUtils.applySchedulersSingle())
                .subscribe(
                    imageFile -> {
                      Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), imageFile);
                      Intent intent = Intents.createForSharingImage(this, contentUri);
                      startActivity(Intent.createChooser(intent, getString(R.string.mediaalbumviewer_share_sheet_title)));
                    },
                    error -> {
                      if (error instanceof NoSuchElementException) {
                        @StringRes int mediaNotLoadedYetMessageRes = activeMediaItem.mediaLink().isVideo()
                            ? R.string.mediaalbumviewer_share_video_not_loaded_yet
                            : R.string.mediaalbumviewer_share_image_not_loaded_yet;
                        Toast.makeText(this, mediaNotLoadedYetMessageRes, Toast.LENGTH_SHORT).show();

                      } else {
                        ResolvedError resolvedError = Dank.errors().resolve(error);
                        Toast.makeText(this, resolvedError.errorMessageRes(), Toast.LENGTH_LONG).show();
                      }
                    }
                )
        );
        return true;

      }
      if (item.getTitle().equals(getString(R.string.mediaalbumviewer_share_image_link))
          || item.getTitle().equals(getString(R.string.mediaalbumviewer_share_video_link))) {
        Intent shareUrlIntent = Intents.createForSharingUrl(null, activeMediaItem.mediaLink().originalUrl());
        startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.webview_share_sheet_title)));
        return true;

      } else {
        throw new AssertionError();
      }
    });
    shareMenu.show();
  }

  @CheckResult
  private Single<File> findHighestResImageFileFromCache(MediaAlbumItem albumItem) {
    Observable<File> highResImageFileStream = Observable.create(emitter -> {
      FutureTarget<File> highResolutionImageTarget = Glide.with(this)
          .download(albumItem.mediaLink().originalUrl())
          .apply(new RequestOptions().onlyRetrieveFromCache(true))
          .submit();

      File highResImageFile = highResolutionImageTarget.get();

      if (highResImageFile != null) {
        emitter.onNext(highResImageFile);
      } else {
        emitter.onComplete();
      }
    });

    Observable<File> optimizedResImageFileStream = Observable.create(emitter -> {
      FutureTarget<File> lowerResolutionImageTarget = Glide.with(this)
          .download(albumItem.mediaLink().optimizedImageUrl(getDeviceDisplayWidth()))
          .apply(new RequestOptions().onlyRetrieveFromCache(true))
          .submit();

      File optimizedResImageFile = lowerResolutionImageTarget.get();

      if (optimizedResImageFile != null) {
        emitter.onNext(optimizedResImageFile);
      } else {
        emitter.onComplete();
      }
    });

    return highResImageFileStream
        .onErrorResumeNext(Observable.empty())
        .concatWith(optimizedResImageFileStream.onErrorResumeNext(Observable.empty()))
        .firstOrError();
  }

  @OnClick(R.id.mediaalbumviewer_download)
  void onClickDownloadMedia() {
    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
    MediaDownloadService.enqueueDownload(this, activeMediaItem.mediaLink());
  }

  @OnClick(R.id.mediaalbumviewer_open_in_browser)
  void onClickOpenMediaInBrowser() {
    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
    startActivity(Intents.createForOpeningUrl(activeMediaItem.mediaLink().originalUrl()));

    if (mediaAlbumAdapter.getCount() == 1) {
      // User prefers viewing this media in the browser.
      finish();
    }
  }

  @OnClick(R.id.mediaalbumviewer_reload_in_hd)
  void onClickReloadMediaInHighDefinition() {
    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
    if (mediaItemsWithHighDefEnabled.contains(activeMediaItem)) {
      mediaItemsWithHighDefEnabled.remove(activeMediaItem);
    } else {
      mediaItemsWithHighDefEnabled.add(activeMediaItem);
    }

    // TODO: Update data-set.
  }

  private void animateMediaOptionsVisibility(boolean showOptions, TimeInterpolator interpolator, long animationDuration) {
    for (int i = 0; i < optionButtonsContainer.getChildCount(); i++) {
      View childView = optionButtonsContainer.getChildAt(i);
      childView.animate().cancel();
      childView.animate()
          .translationY(showOptions ? 0f : childView.getHeight())
          .alpha(showOptions ? 1f : 0f)
          .setDuration(animationDuration)
          .setInterpolator(interpolator)
          .start();
    }
  }

  /**
   * Enable HD button if a higher-res version can be shown and is not already visible.
   */
  private void enableHighDefButtonIfPossible(MediaAlbumItem activeMediaItem) {
    String highQualityUrl;
    String optimizedUrl;

    if (activeMediaItem.mediaLink().isVideo()) {
      highQualityUrl = activeMediaItem.mediaLink().highQualityVideoUrl();
      optimizedUrl = activeMediaItem.mediaLink().lowQualityVideoUrl();

    } else {
      highQualityUrl = activeMediaItem.mediaLink().originalUrl();
      optimizedUrl = activeMediaItem.mediaLink().optimizedImageUrl(getDeviceDisplayWidth());
    }

    boolean hasHighDefVersion = !optimizedUrl.equals(highQualityUrl);
    boolean isAlreadyShowingHighDefVersion = mediaItemsWithHighDefEnabled.contains(activeMediaItem);
    reloadInHighDefButton.setEnabled(hasHighDefVersion && !isAlreadyShowingHighDefVersion);
  }
}
