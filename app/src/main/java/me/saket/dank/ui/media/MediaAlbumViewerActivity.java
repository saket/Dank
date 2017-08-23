package me.saket.dank.ui.media;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.common.io.Files;
import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;
import com.tbruyelle.rxpermissions2.RxPermissions;

import junit.framework.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

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
import me.saket.dank.utils.VideoHostRepository;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

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
  @BindView(R.id.mediaalbumviewer_options_background_gradient) View optionButtonsBackgroundGradientView;

  @Inject VideoHostRepository videoHostRepository;
  @Inject HttpProxyCacheServer videoCacheServer;

  private SystemUiHelper systemUiHelper;
  private Drawable activityBackgroundDrawable;
  private MediaAlbumPagerAdapter mediaAlbumAdapter;
  private Set<MediaAlbumItem> mediaItemsWithHighDefEnabled = new HashSet<>();
  private PopupMenu sharePopupMenu;
  private RxPermissions rxPermissions;

  public static void start(Context context, MediaLink mediaLink) {
    Intent intent = new Intent(context, MediaAlbumViewerActivity.class);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

    super.onCreate(savedInstanceState);
    overridePendingTransition(R.anim.fade_in, 0);

    setContentView(R.layout.activity_media_album_viewer);
    ButterKnife.bind(this);

    List<MediaAlbumItem> mediaLinks = new ArrayList<>();
    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/WhGHrBE.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/eYveT4s.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.redd.it/mis36nxfe5hz.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("http://i.imgur.com/QtD7iMr.jpg")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://streamable.com/fcill")));
//    mediaLinks.add(MediaAlbumItem.create((MediaLink) UrlParser.parse("https://i.imgur.com/Cj9XeIY.gifv")));
    mediaAlbumAdapter = new MediaAlbumPagerAdapter(getSupportFragmentManager(), mediaLinks);
    mediaAlbumPager.setAdapter(mediaAlbumAdapter);

    // TODO: Show media options only when we have adapter data.

    systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, this);

    // Fade in background dimming.
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

    int navBarHeight = Views.getNavigationBarSize(this).y;
    Views.setPaddingBottom(optionButtonsContainer, navBarHeight);
    Views.executeOnMeasure(optionButtonsContainer, () -> {
      int shareButtonTop = Views.globalVisibleRect(shareButton).top + shareButton.getPaddingTop();
      int gradientHeight = optionButtonsBackgroundGradientView.getTop() - shareButtonTop;
      Views.setHeight(optionButtonsBackgroundGradientView, gradientHeight);
      animateMediaOptionsVisibility(true, Animations.INTERPOLATOR, 200, true);
    });

    rxPermissions = new RxPermissions(this);
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    if (mediaAlbumAdapter.getCount() > 1) {
      List<MediaAlbumItem> items = mediaAlbumAdapter.getDataSet();
      for (MediaAlbumItem item : items) {
        if (item.mediaLink().isVideo()) {
          // For adding support for multiple videos, we need to:
          // 1. Intercept ViewPager scroll if seek-bar is being touched.
          // 2. Think about immersive mode. What if immersive mode was enabled from a previous image. Should we disable it now?
          throw new UnsupportedOperationException("Only one video is supported right now");
        }
      }
    }

    sharePopupMenu = createSharePopupMenu();
    shareButton.setOnTouchListener(sharePopupMenu.getDragToOpenListener());

    unsubscribeOnDestroy(
        RxViewPager.pageSelections(mediaAlbumPager)
            .map(currentItem -> mediaAlbumAdapter.getDataSet().get(currentItem))
            .doOnNext(activeMediaItem -> updateContentDescriptionOfOptionButtonsAccordingTo(activeMediaItem))
            .doOnNext(activeMediaItem -> enableHighDefButtonIfPossible(activeMediaItem))
            .doOnNext(activeMediaItem -> {
              sharePopupMenu.getMenu().clear();
              getMenuInflater().inflate(
                  activeMediaItem.mediaLink().isVideo() ? R.menu.menu_share_video : R.menu.menu_share_image,
                  sharePopupMenu.getMenu()
              );
            })
            .subscribe()
    );
  }

  /**
   * Menu items get inflated in {@link #onPostCreate(Bundle)} on page change depending upon the current media's type (image or video).
   */
  private PopupMenu createSharePopupMenu() {
    // Note: the style sets a negative top offset so that the popup appears on top of the share button.
    // Unfortunately, setting WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS on the Window causes
    // PopupMenu to not position itself within the window limits, regardless of using top gravity.
    PopupMenu sharePopupMenu = new PopupMenu(this, shareButton, Gravity.TOP, 0, R.style.DankPopupMenu_AlbumViewerOption);
    sharePopupMenu.setOnMenuItemClickListener(item -> {
      MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());

      switch (item.getItemId()) {
        case R.id.action_share_image:
          unsubscribeOnDestroy(
              findHighestResImageFileFromCache(activeMediaItem)
                  .map(imageFile -> {
                    // Glide uses random file names, without any extensions. Certain apps like Messaging
                    // fail to parse images if there's no file format, so we'll have to create a copy.
                    String mediaFileName = Urls.parseFileNameWithExtension(activeMediaItem.mediaLink().originalUrl());
                    File imageFileWithExtension = new File(imageFile.getParent(), mediaFileName);
                    Files.copy(imageFile, imageFileWithExtension);
                    return imageFileWithExtension;
                  })
                  .compose(RxUtils.applySchedulersSingle())
                  .subscribe(
                      imageFile -> {
                        Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), imageFile);
                        Intent intent = Intents.createForSharingMedia(this, contentUri);
                        startActivity(Intent.createChooser(intent, getString(R.string.mediaalbumviewer_share_sheet_title)));
                      },
                      error -> {
                        if (error instanceof NoSuchElementException) {
                          Assert.assertEquals(activeMediaItem.mediaLink().isVideo(), false);
                          Toast.makeText(this, R.string.mediaalbumviewer_share_image_not_loaded_yet, Toast.LENGTH_SHORT).show();

                        } else {
                          ResolvedError resolvedError = Dank.errors().resolve(error);
                          Toast.makeText(this, resolvedError.errorMessageRes(), Toast.LENGTH_LONG).show();
                        }
                      }
                  )
          );
          break;

        case R.id.action_share_video:
          unsubscribeOnDestroy(
              // TODO: Once we resolve all links at the beginning, this shouldn't be needed:
              videoHostRepository.fetchActualVideoUrlIfNeeded(activeMediaItem.mediaLink())
                  .compose(RxUtils.applySchedulersSingle())
                  .map(resolvedVideoLink -> {
                    if (videoCacheServer.isCached(resolvedVideoLink.highQualityVideoUrl())) {
                      String cachedVideoFileUrl = videoCacheServer.getProxyUrl(resolvedVideoLink.highQualityVideoUrl());
                      return new File(Uri.parse(cachedVideoFileUrl).getPath());

                    } else if (videoCacheServer.isCached(resolvedVideoLink.lowQualityVideoUrl())) {
                      String cachedVideoFileUrl = videoCacheServer.getProxyUrl(resolvedVideoLink.lowQualityVideoUrl());
                      return new File(Uri.parse(cachedVideoFileUrl).getPath());

                    } else {
                      throw new NoSuchElementException();
                    }
                  })
                  .subscribe(
                      videoFile -> {
                        Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), videoFile);
                        Intent intent = Intents.createForSharingMedia(this, contentUri);
                        startActivity(Intent.createChooser(intent, getString(R.string.mediaalbumviewer_share_sheet_title)));
                      },
                      error -> {
                        if (error instanceof NoSuchElementException) {
                          Toast.makeText(this, R.string.mediaalbumviewer_share_video_not_loaded_yet, Toast.LENGTH_SHORT).show();
                        } else {
                          ResolvedError resolvedError = Dank.errors().resolve(error);
                          Toast.makeText(this, resolvedError.errorMessageRes(), Toast.LENGTH_LONG).show();
                        }
                      }
                  )
          );
          break;

        case R.id.action_share_image_url:
        case R.id.action_share_video_url:
          Intent shareUrlIntent = Intents.createForSharingUrl(null, activeMediaItem.mediaLink().originalUrl());
          startActivity(Intent.createChooser(shareUrlIntent, getString(R.string.webview_share_sheet_title)));
          break;

        default:
          throw new AssertionError();
      }

      return true;
    });

    return sharePopupMenu;
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
            .doOnSubscribe(o -> animateMediaOptionsVisibility(false, Animations.INTERPOLATOR, 100, false))
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
    animateMediaOptionsVisibility(systemUiVisible, interpolator, animationDuration, false);
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

  @OnClick(R.id.mediaalbumviewer_share)
  void onClickShareMedia() {
    sharePopupMenu.show();
  }

  @OnClick(R.id.mediaalbumviewer_download)
  void onClickDownloadMedia() {
    rxPermissions
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .filter(permissionGranted -> permissionGranted)
        .subscribe(o -> {
          MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
          MediaDownloadService.enqueueDownload(this, activeMediaItem.mediaLink());
        });
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

  private void animateMediaOptionsVisibility(boolean showOptions, TimeInterpolator interpolator, long animationDuration, boolean setInitialValues) {
    if (setInitialValues) {
      optionButtonsBackgroundGradientView.setAlpha(showOptions ? 0f : 1f);
      optionButtonsBackgroundGradientView.setTranslationY(showOptions ? optionButtonsBackgroundGradientView.getHeight() : 0f);
    }

    optionButtonsBackgroundGradientView.animate().cancel();
    optionButtonsBackgroundGradientView.animate()
        .translationY(showOptions ? 0f : optionButtonsBackgroundGradientView.getHeight())
        .alpha(showOptions ? 1f : 0f)
        .setDuration((long) (animationDuration * (showOptions ? 1.5f : 1)))
        .setInterpolator(interpolator)
        .start();

    // Animating the child Views so that they get clipped by their parent container.
    for (int i = 0; i < optionButtonsContainer.getChildCount(); i++) {
      View childView = optionButtonsContainer.getChildAt(i);

      if (setInitialValues) {
        childView.setAlpha(showOptions ? 0f : 1f);
        childView.setTranslationY(showOptions ? childView.getHeight() : 0f);
      }

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
