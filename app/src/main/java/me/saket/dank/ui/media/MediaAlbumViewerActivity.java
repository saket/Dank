package me.saket.dank.ui.media;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.CheckResult;
import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.PopupMenu;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.common.io.Files;
import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;
import com.tbruyelle.rxpermissions2.RxPermissions;

import net.dean.jraw.models.Thumbnails;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
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
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.links.ImgurAlbumLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.MediaDownloadService;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.JacksonHelper;
import me.saket.dank.utils.MediaHostRepository;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.SystemUiHelper;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.ScrollInterceptibleViewPager;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

public class MediaAlbumViewerActivity extends DankActivity implements MediaFragmentCallbacks, FlickGestureListener.GestureCallbacks {

  private static final String KEY_MEDIA_LINK_TO_SHOW = "mediaLinkToShow";
  private static final String KEY_REDDIT_SUPPLIED_IMAGE_JSON = "redditSuppliedImageJson";

  @BindView(R.id.mediaalbumviewer_root) ViewGroup rootLayout;
  @BindView(R.id.mediaalbumviewer_pager) ScrollInterceptibleViewPager mediaAlbumPager;
  @BindView(R.id.mediaalbumviewer_media_options_container) ViewGroup optionButtonsContainerView;
  @BindView(R.id.mediaalbumviewer_position_and_options_container) ViewGroup contentOptionButtonsContainerView;
  @BindView(R.id.mediaalbumviewer_share) ImageButton shareButton;
  @BindView(R.id.mediaalbumviewer_download) ImageButton downloadButton;
  @BindView(R.id.mediaalbumviewer_open_in_browser) ImageButton openInBrowserButton;
  @BindView(R.id.mediaalbumviewer_reload_in_hd) ImageButton reloadInHighDefButton;
  @BindView(R.id.mediaalbumviewer_options_background_gradient) View contentInfoBackgroundGradientView;
  @BindView(R.id.mediaalbumviewer_flick_dismiss_layout) FlickDismissLayout flickDismissLayout;
  @BindView(R.id.mediaalbumviewer_media_position) TextView mediaPositionTextView;
  @BindView(R.id.mediaalbumviewer_progress) View resolveProgressView;
  @BindView(R.id.mediaalbumviewer_error_container) ViewGroup resolveErrorViewContainer;
  @BindView(R.id.mediaalbumviewer_error) ErrorStateView resolveErrorView;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject HttpProxyCacheServer videoCacheServer;
  @Inject JacksonHelper jacksonHelper;
  @Inject UrlRouter urlRouter;
  @Inject ErrorResolver errorResolver;

  private SystemUiHelper systemUiHelper;
  private Drawable activityBackgroundDrawable;
  private MediaAlbumPagerAdapter mediaAlbumAdapter;
  private PopupMenu sharePopupMenu;
  private RxPermissions rxPermissions;
  private Relay<Boolean> systemUiVisibilityStream = BehaviorRelay.create();
  private MediaLink resolvedMediaLink;
  private DankLinkMovementMethod linkMovementMethod;
  private Set<MediaLink> hdEnabledMediaLinks = new HashSet<>();
  private Relay<Set<MediaLink>> hdEnabledMediaLinksStream = BehaviorRelay.create();
  private Relay<MediaAlbumItem> viewpagerPageChangeStream = BehaviorRelay.create();

  private enum ScreenState {
    /**
     * Hitting the API of Imgur, Streamable, etc to fetch image links in case it's an album link.
     */
    RESOLVING_LINK,

    /**
     * Images/videos ready to be loaded.
     */
    LINK_RESOLVED,

    FAILED,
  }

  public static void start(Context context, MediaLink mediaLink, @Nullable Thumbnails redditSuppliedImages, JacksonHelper jacksonHelper) {
    Intent intent = new Intent(context, MediaAlbumViewerActivity.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_SHOW, mediaLink);
    if (redditSuppliedImages != null) {
      String redditSuppliedImageJson = jacksonHelper.toJson(redditSuppliedImages.getDataNode());
      intent.putExtra(KEY_REDDIT_SUPPLIED_IMAGE_JSON, redditSuppliedImageJson);
    }
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

    moveToScreenState(ScreenState.RESOLVING_LINK);

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

    // Animated once images are fetched.
    optionButtonsContainerView.setVisibility(View.INVISIBLE);
    ((ViewGroup) optionButtonsContainerView.getParent()).getLayoutTransition().setDuration(200);

    // Show the option buttons above the navigation bar.
    int navBarHeight = Views.getNavigationBarSize(this).y;
    Views.setPaddingBottom(contentOptionButtonsContainerView, navBarHeight);

    rxPermissions = new RxPermissions(this);
    systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, systemUiVisible -> {
      systemUiVisibilityStream.accept(systemUiVisible);
    });
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, R.anim.fade_out);
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    sharePopupMenu = createSharePopupMenu();
    shareButton.setOnTouchListener(sharePopupMenu.getDragToOpenListener());

    mediaAlbumAdapter = new MediaAlbumPagerAdapter(getSupportFragmentManager());
    mediaAlbumPager.setAdapter(mediaAlbumAdapter);
    hdEnabledMediaLinksStream.accept(hdEnabledMediaLinks);  // Initial value.

    // Since only the image/video is flick-dismissible and not the entire Activity, we
    // have another flick-dismiss container for the initial progress View.
    setupFlickDismissForProgressView();

    MediaLink mediaLinkToDisplay = getIntent().getParcelableExtra(KEY_MEDIA_LINK_TO_SHOW);
    resolveMediaLinkAndDisplayContent(mediaLinkToDisplay);

    // Hide all content when Activity goes immersive.
    unsubscribeOnDestroy(
        systemUiVisibilityStream
            .subscribe(systemUiVisible -> {
              TimeInterpolator interpolator = systemUiVisible ? new DecelerateInterpolator(2f) : new AccelerateInterpolator(2f);
              long animationDuration = 300;
              animateMediaOptionsVisibility(systemUiVisible, interpolator, animationDuration, false);
            })
    );

    // Toggle HD button's visibility if a higher-res version can be shown and is not already visible.
    unsubscribeOnDestroy(
        viewpagerPageChangeStream
            .concatMap(activeMediaItem -> hdEnabledMediaLinksStream.map(o -> activeMediaItem))
            .subscribe(activeMediaItem -> {
              String highQualityUrl = activeMediaItem.mediaLink().highQualityUrl();
              String optimizedQualityUrl = mediaHostRepository.findOptimizedQualityImageForDisplay(
                  getRedditSuppliedImages(),
                  getResources().getDisplayMetrics().widthPixels,
                  activeMediaItem.mediaLink().lowQualityUrl() /* defaultValue */
              );

              boolean hasHighDefVersion = !optimizedQualityUrl.equals(highQualityUrl);
              boolean isAlreadyShowingHighDefVersion = hdEnabledMediaLinks.contains(activeMediaItem.mediaLink());
              reloadInHighDefButton.setEnabled(hasHighDefVersion && !isAlreadyShowingHighDefVersion);
            })
    );

    mediaAlbumPager.setOnInterceptScrollListener((view, deltaX, touchX, touchY) -> {
      if (view instanceof ZoomableImageView) {
        return ((ZoomableImageView) view).canPanAnyFurtherHorizontally(deltaX);
      } else {
        // Avoid paging when a video's SeekBar is being used.
        return view.getId() == R.id.exomedia_controls_video_seek || view.getId() == R.id.exomedia_controls_video_seek_container;
      }
    });
  }

  private void resolveMediaLinkAndDisplayContent(MediaLink mediaLinkToDisplay) {
    unsubscribeOnDestroy(
        mediaHostRepository.resolveActualLinkIfNeeded(mediaLinkToDisplay)
            .doOnSuccess(resolvedMediaLink -> this.resolvedMediaLink = resolvedMediaLink)
            .map(resolvedMediaLink -> {
              // Find all child images under an album.
              if (resolvedMediaLink.isMediaAlbum()) {
                return ((ImgurAlbumLink) resolvedMediaLink).images();
              } else {
                return Collections.singletonList(resolvedMediaLink);
              }
            })
            .compose(RxUtils.applySchedulersSingle())
            .doOnSuccess(mediaLinks -> {
              // Toggle HD for all images with the default value.
              boolean loadHighQualityImageByDefault = false; // TODO: Get this from user's mobile-data preferences.

              if (loadHighQualityImageByDefault) {
                hdEnabledMediaLinks.addAll(mediaLinks);
                hdEnabledMediaLinksStream.accept(hdEnabledMediaLinks);
              }
            })
            .flatMapObservable(mediaLinks -> {
              // Enable HD flag if it's turned on.
              return hdEnabledMediaLinksStream
                  .map(hdEnabledMediaLinks -> {
                    List<MediaAlbumItem> mediaAlbumItems = new ArrayList<>(mediaLinks.size());
                    for (MediaLink mediaLink : mediaLinks) {
                      boolean highDefEnabled = hdEnabledMediaLinks.contains(mediaLink);

                      mediaAlbumItems.add(MediaAlbumItem.create(mediaLink, highDefEnabled));
                    }
                    return mediaAlbumItems;
                  });
            })
            .subscribe(
                mediaAlbumItems -> {
                  boolean isFirstDataChange = mediaAlbumAdapter.getCount() == 0;
                  mediaAlbumAdapter.setAlbumItems(mediaAlbumItems);
                  moveToScreenState(ScreenState.LINK_RESOLVED);

                  if (isFirstDataChange) {
                    startListeningToViewPagerPageChanges();

                    // Show media options now that we have adapter data.
                    optionButtonsContainerView.setVisibility(View.VISIBLE);
                    Views.executeOnMeasure(optionButtonsContainerView, () -> {
                      animateMediaOptionsVisibility(true, Animations.INTERPOLATOR, 200, true);
                    });

                  } else {
                    // Note: FragmentStatePagerAdapter does not refresh any active Fragments so doing it manually.
                    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
                    mediaAlbumAdapter.getActiveFragment().handleMediaItemUpdate(activeMediaItem);
                  }
                },
                error -> {
                  moveToScreenState(ScreenState.FAILED);

                  ResolvedError resolvedError = errorResolver.resolve(error);
                  resolveErrorView.applyFrom(resolvedError);
                  resolveErrorView.setOnRetryClickListener(o -> resolveMediaLinkAndDisplayContent(mediaLinkToDisplay));
                }
            )
    );
  }

  private void moveToScreenState(ScreenState screenState) {
    resolveProgressView.setVisibility(screenState == ScreenState.RESOLVING_LINK ? View.VISIBLE : View.GONE);
    resolveErrorViewContainer.setVisibility(screenState == ScreenState.FAILED ? View.VISIBLE : View.GONE);
    mediaAlbumPager.setVisibility(screenState == ScreenState.LINK_RESOLVED ? View.VISIBLE : View.GONE);
  }

  private void startListeningToViewPagerPageChanges() {
    unsubscribeOnDestroy(
        RxViewPager.pageSelections(mediaAlbumPager)
            .map(currentItem -> mediaAlbumAdapter.getDataSet().get(currentItem))
            .doOnNext(activeMediaItem -> updateContentDescriptionOfOptionButtonsAccordingTo(activeMediaItem))
            .doOnNext(activeMediaItem -> updateShareMenuFor(activeMediaItem))
            .doOnNext(activeMediaItem -> updateMediaDisplayPosition())
            .subscribe(viewpagerPageChangeStream)
    );
  }

  private void updateMediaDisplayPosition() {
    int totalMediaItems = mediaAlbumAdapter.getCount();
    mediaPositionTextView.setVisibility(totalMediaItems > 1 ? View.VISIBLE : View.INVISIBLE);

    if (totalMediaItems > 1) {
      // We're dealing with an album.
      mediaPositionTextView.setText(getString(R.string.mediaalbumviewer_media_position, mediaAlbumPager.getCurrentItem() + 1, totalMediaItems
      ));
    }
  }

  private void setupFlickDismissForProgressView() {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(this));
    flickListener.setFlickThresholdSlop(.5f);    // Dismiss once the image is swiped 50% away.
    flickListener.setGestureCallbacks(new FlickGestureListener.GestureCallbacks() {
      @Override
      public void onFlickDismissEnd(long flickAnimationDuration) {
        if (resolvedMediaLink != null) {
          // Link has been resolved and image/video has started loading. Ignore this flick.
          return;
        }
        MediaAlbumViewerActivity.this.onFlickDismissEnd(flickAnimationDuration);
      }

      @Override
      public void onMoveMedia(float moveRatio) {}
    });
    flickListener.setOnTouchDownReturnValueProvider(motionEvent -> {
      // Hackkyyy hacckkk. Ugh.
      return !Views.touchLiesOn(resolveErrorView.getRetryButton(), motionEvent.getRawX(), motionEvent.getRawY());
    });
    flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
      @Override
      public int getContentHeightForDismissAnimation() {
        return flickDismissLayout.getHeight();
      }

      @Override
      public int getContentHeightForCalculatingThreshold() {
        return flickDismissLayout.getHeight();
      }
    });
    flickDismissLayout.setFlickGestureListener(flickListener);
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

  @Nullable
  @Override
  public Thumbnails getRedditSuppliedImages() {
    if (getIntent().hasExtra(KEY_REDDIT_SUPPLIED_IMAGE_JSON)) {
      String redditSuppliedImagesJson = getIntent().getStringExtra(KEY_REDDIT_SUPPLIED_IMAGE_JSON);
      return new Thumbnails(jacksonHelper.parseJsonNode(redditSuppliedImagesJson));
    } else {
      return null;
    }
  }

  @Override
  public Single<Integer> optionButtonsHeight() {
    return Single.create(emitter ->
        Views.executeOnMeasure(contentOptionButtonsContainerView,
            () -> emitter.onSuccess(contentOptionButtonsContainerView.getHeight())
        )
    );
  }

  @Override
  public Observable<Boolean> systemUiVisibilityStream() {
    return systemUiVisibilityStream;
  }

  @Override
  public LinkMovementMethod getMediaDescriptionLinkMovementMethod() {
    if (linkMovementMethod == null) {
      linkMovementMethod = DankLinkMovementMethod.newInstance();
      linkMovementMethod.setOnLinkClickListener((textView, url) -> {
        Point clickedUrlCoordinates = linkMovementMethod.getLastUrlClickCoordinates();
        Link parsedLink = UrlParser.parse(url);

        if (parsedLink instanceof RedditUserLink) {
          urlRouter.forLink(((RedditUserLink) parsedLink))
              .expandFrom(clickedUrlCoordinates)
              .open(textView);

        } else {
          urlRouter.forLink(parsedLink)
              .expandFrom(clickedUrlCoordinates)
              .open(this);
        }
        return true;
      });
    }
    return linkMovementMethod;
  }

  @Override
  public void onFlickDismissEnd(long flickAnimationDuration) {
    unsubscribeOnDestroy(
        Observable.timer(flickAnimationDuration, TimeUnit.MILLISECONDS)
            .doOnSubscribe(o -> animateMediaOptionsVisibility(false, Animations.INTERPOLATOR, 200, false))
            .subscribe(o -> finish())
    );
  }

  @Override
  public void onMoveMedia(@FloatRange(from = -1, to = 1) float moveRatio) {
    updateBackgroundDimmingAlpha(Math.abs(moveRatio));

    boolean isImageBeingMoved = moveRatio != 0f;
    optionButtonsContainerView.setVisibility(isImageBeingMoved ? View.GONE : View.VISIBLE);
  }

  /**
   * @param targetTransparencyFactor 1f for maximum transparency. 0f for none.
   */
  private void updateBackgroundDimmingAlpha(@FloatRange(from = 0, to = 1) float targetTransparencyFactor) {
    // Increase dimming exponentially so that the background is fully transparent while the image has been moved by half.
    float dimming = 1f - Math.min(1f, targetTransparencyFactor * 2);
    activityBackgroundDrawable.setAlpha((int) (dimming * 255));
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
          .download(albumItem.mediaLink().highQualityUrl())
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
      String optimizedQualityImageForDevice = mediaHostRepository.findOptimizedQualityImageForDisplay(
          getRedditSuppliedImages(), getDeviceDisplayWidth(), albumItem.mediaLink().lowQualityUrl()
      );

      FutureTarget<File> optimizedResolutionImageTarget = Glide.with(this)
          .download(optimizedQualityImageForDevice)
          .apply(new RequestOptions().onlyRetrieveFromCache(true))
          .submit();

      File optimizedResImageFile = optimizedResolutionImageTarget.get();

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
                    String imageNameWithExtension = Urls.parseFileNameWithExtension(activeMediaItem.mediaLink().highQualityUrl());
                    File imageFileWithExtension = new File(imageFile.getParent(), imageNameWithExtension);
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
              Single.just(activeMediaItem.mediaLink())
                  .map(mediaLink -> {
                    if (videoCacheServer.isCached(mediaLink.highQualityUrl())) {
                      String cachedVideoFileUrl = videoCacheServer.getProxyUrl(mediaLink.highQualityUrl());
                      return new File(Uri.parse(cachedVideoFileUrl).getPath());

                    } else if (videoCacheServer.isCached(mediaLink.lowQualityUrl())) {
                      String cachedVideoFileUrl = videoCacheServer.getProxyUrl(mediaLink.lowQualityUrl());
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

        case R.id.action_share_album_url:
          Intent shareAlbumUrlIntent = Intents.createForSharingUrl(null, resolvedMediaLink.unparsedUrl());
          startActivity(Intent.createChooser(shareAlbumUrlIntent, getString(R.string.webview_share_sheet_title)));
          break;

        case R.id.action_share_image_url:
        case R.id.action_share_video_url:
          Intent shareMediaUrlIntent = Intents.createForSharingUrl(null, activeMediaItem.mediaLink().highQualityUrl());
          startActivity(Intent.createChooser(shareMediaUrlIntent, getString(R.string.webview_share_sheet_title)));
          break;

        default:
          throw new AssertionError();
      }

      return true;
    });

    return sharePopupMenu;
  }

  private void updateShareMenuFor(MediaAlbumItem activeMediaItem) {
    sharePopupMenu.getMenu().clear();
    int menuRes = activeMediaItem.mediaLink().isVideo() ? R.menu.menu_share_video : R.menu.menu_share_image;
    getMenuInflater().inflate(menuRes, sharePopupMenu.getMenu());

    MenuItem shareAlbumMenuItem = sharePopupMenu.getMenu().findItem(R.id.action_share_album_url);
    shareAlbumMenuItem.setVisible(resolvedMediaLink.isMediaAlbum());
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
    // Open the entire album in the browser.
    if (resolvedMediaLink.isMediaAlbum()) {
      startActivity(Intents.createForOpeningUrl(resolvedMediaLink.unparsedUrl()));

    } else {
      MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
      startActivity(Intents.createForOpeningUrl(activeMediaItem.mediaLink().highQualityUrl()));
    }

    if (mediaAlbumAdapter.getCount() == 1) {
      // User prefers viewing this media in the browser.
      finish();
    }
  }

  @OnClick(R.id.mediaalbumviewer_reload_in_hd)
  void onClickReloadMediaInHighDefinition() {
    MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());
    if (hdEnabledMediaLinks.contains(activeMediaItem.mediaLink())) {
      hdEnabledMediaLinks.remove(activeMediaItem.mediaLink());
    } else {
      hdEnabledMediaLinks.add(activeMediaItem.mediaLink());
    }
    hdEnabledMediaLinksStream.accept(hdEnabledMediaLinks);
  }

  private void animateMediaOptionsVisibility(boolean showOptions, TimeInterpolator interpolator, long animationDuration, boolean setInitialValues) {
    if (setInitialValues) {
      contentInfoBackgroundGradientView.setAlpha(showOptions ? 0f : 1f);
      contentInfoBackgroundGradientView.setTranslationY(showOptions ? contentInfoBackgroundGradientView.getHeight() : 0f);
    }

    contentInfoBackgroundGradientView.animate().cancel();
    contentInfoBackgroundGradientView.animate()
        .translationY(showOptions ? 0f : contentInfoBackgroundGradientView.getHeight())
        .alpha(showOptions ? 1f : 0f)
        .setDuration((long) (animationDuration * (showOptions ? 1.5f : 1)))
        .setInterpolator(interpolator)
        .start();

    // Animating the child Views so that they get clipped by their parent container.
    for (int i = 0; i < optionButtonsContainerView.getChildCount(); i++) {
      View childView = optionButtonsContainerView.getChildAt(i);

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
}
