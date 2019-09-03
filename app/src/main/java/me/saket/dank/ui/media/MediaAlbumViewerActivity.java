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
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CheckResult;
import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.danikula.videocache.HttpProxyCacheServer;
import com.f2prateek.rx.preferences2.Preference;
import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;
import com.tbruyelle.rxpermissions2.RxPermissions;

import net.dean.jraw.models.SubmissionPreview;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.MediaDownloadService;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.urlparser.ImgurAlbumLink;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Files2;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.SystemUiHelper;
import me.saket.dank.utils.Urls;
import me.saket.dank.utils.VideoFormat;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.ProgressWithFileSizeView;
import me.saket.dank.widgets.ScrollInterceptibleViewPager;
import me.saket.dank.widgets.ZoomableImageView;
import me.saket.dank.widgets.binoculars.FlickDismissLayout;
import me.saket.dank.widgets.binoculars.FlickGestureListener;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

public class MediaAlbumViewerActivity extends DankActivity implements MediaFragmentCallbacks, FlickGestureListener.GestureCallbacks {

  private static final String KEY_MEDIA_LINK_TO_SHOW = "mediaLinkToShow";
  private static final String KEY_REDDIT_SUPPLIED_IMAGE = "redditSuppliedImage";

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
  @BindView(R.id.mediaalbumviewer_progress) ProgressWithFileSizeView resolveProgressView;
  @BindView(R.id.mediaalbumviewer_error_container) ViewGroup resolveErrorViewContainer;
  @BindView(R.id.mediaalbumviewer_error) ErrorStateView resolveErrorView;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject HttpProxyCacheServer videoCacheServer;
  @Inject ErrorResolver errorResolver;
  @Inject UserPreferences userPreferences;
  @Inject NetworkStateListener networkStateListener;

  @Inject @Named("hd_media_in_gallery")
  Lazy<Preference<NetworkStrategy>> highResolutionMediaNetworkStrategyPref;

  private SystemUiHelper systemUiHelper;
  private Drawable activityBackgroundDrawable;
  private MediaAlbumPagerAdapter mediaAlbumAdapter;
  private PopupMenu sharePopupMenu;
  private RxPermissions rxPermissions;
  private Relay<Boolean> systemUiVisibilityStream = BehaviorRelay.create();
  private MediaLink resolvedMediaLink;
  private Set<MediaLink> hdEnabledMediaLinks = new HashSet<>();
  private BehaviorRelay<Set<MediaLink>> hdEnabledMediaLinksStream = BehaviorRelay.create();
  private BehaviorRelay<MediaAlbumItem> viewpagerPageChangeStream = BehaviorRelay.create();

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

  public static Intent intent(Context context, MediaLink mediaLink, @Nullable SubmissionPreview redditPreview) {
    Intent intent = new Intent(context, MediaAlbumViewerActivity.class);
    intent.putExtra(KEY_MEDIA_LINK_TO_SHOW, mediaLink);
    if (redditPreview != null) {
      intent.putExtra(KEY_REDDIT_SUPPLIED_IMAGE, redditPreview);
    }
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);

    super.onCreate(savedInstanceState);
    overridePendingTransition(R.anim.fade_in_300, 0);

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

    rootLayout.setOnApplyWindowInsetsListener((v, insets) -> {
      positionMediaControlsToAvoidOverlappingWithNavBar(insets);
      return insets;
    });

    rxPermissions = new RxPermissions(this);
    systemUiHelper = new SystemUiHelper(this, SystemUiHelper.LEVEL_IMMERSIVE, 0, systemUiVisible ->
        systemUiVisibilityStream.accept(systemUiVisible)
    );
  }

  @Override
  public void finish() {
    super.finish();
    overridePendingTransition(0, R.anim.fade_out_300);
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

    resolveProgressView.setIndeterminate(true);
    resolveProgressView.setProgressBarBackgroundFillEnabled(false);

    // Since only the image/video is flick-dismissible and not the entire Activity, we
    // have another flick-dismiss container for the initial progress View.
    setupFlickDismissForProgressView();

    MediaLink mediaLinkToDisplay = getIntent().getParcelableExtra(KEY_MEDIA_LINK_TO_SHOW);
    resolveMediaLinkAndDisplayContent(mediaLinkToDisplay);

    // Hide all content when Activity goes immersive.
    systemUiVisibilityStream
        .takeUntil(lifecycle().onDestroy())
        .subscribe(systemUiVisible -> {
          TimeInterpolator interpolator = systemUiVisible ? new DecelerateInterpolator(2f) : new AccelerateInterpolator(2f);
          long animationDuration = 300;
          animateMediaOptionsVisibility(systemUiVisible, interpolator, animationDuration, false);
        });

    // Toggle HD button's visibility if a higher-res version can be shown and is not already visible.
    // TODO: Simplify this Rx chain. Concat-mapping with hdEnabledMediaLinksStream doesn't make sense.
    viewpagerPageChangeStream
        .concatMap(activeMediaItem -> hdEnabledMediaLinksStream.map(o -> activeMediaItem))
        .flatMapSingle(activeMediaItem -> getRedditSuppliedImages().map(redditImages -> Pair.create(activeMediaItem, redditImages)))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          MediaAlbumItem activeMediaItem = pair.first();
          Optional<SubmissionPreview> redditSuppliedImages = pair.second();

          String highQualityUrl = activeMediaItem.mediaLink().highQualityUrl();
          String optimizedQualityUrl;

          if (activeMediaItem.mediaLink().isGif()) {
            optimizedQualityUrl = activeMediaItem.mediaLink().lowQualityUrl();
          } else {
            ImageWithMultipleVariants imageVariants = ImageWithMultipleVariants.Companion.of(redditSuppliedImages);
            optimizedQualityUrl = imageVariants.findNearestFor(
                getResources().getDisplayMetrics().widthPixels,
                activeMediaItem.mediaLink().lowQualityUrl() /* defaultValue */
            );
          }

          //noinspection ConstantConditions
          boolean hasHighDefVersion = !optimizedQualityUrl.equals(highQualityUrl);
          boolean isAlreadyShowingHighDefVersion = hdEnabledMediaLinks.contains(activeMediaItem.mediaLink());
          reloadInHighDefButton.setEnabled(hasHighDefVersion && !isAlreadyShowingHighDefVersion);
        });

    mediaAlbumPager.setOnInterceptScrollListener((view, deltaX, touchX, touchY) -> {
      if (view instanceof ZoomableImageView) {
        return ((ZoomableImageView) view).canPanAnyFurtherHorizontally(deltaX);
      } else {
        // Avoid paging when a video's SeekBar is being used.
        return view.getId() == R.id.videocontrols_seek;
      }
    });
  }

  @Override
  protected void onDestroy() {
    try {
      super.onDestroy();
    } catch (Exception e) {
      if (e.getMessage() == null || !e.getMessage().contains("Cannot obtain size for recycled Bitmap")) {
        throw e;
      }
      // Else, swallow.
    }
  }

  private void positionMediaControlsToAvoidOverlappingWithNavBar(WindowInsets insets) {
    int bottomWindowInset = insets.getSystemWindowInsetBottom();
    int rightWindowInset = insets.getSystemWindowInsetRight();

    // FYI: The paddings are intentionally not relative.
    // The navigation bar is always on the right AFAIK.
    ViewGroup view = contentOptionButtonsContainerView;
    view.setPadding(
        view.getPaddingLeft(),
        view.getPaddingTop(),
        view.getPaddingRight() + rightWindowInset,
        view.getPaddingBottom() + bottomWindowInset);
  }

  private void resolveMediaLinkAndDisplayContent(MediaLink mediaLinkToDisplay) {
    mediaHostRepository.resolveActualLinkIfNeeded(mediaLinkToDisplay)
        // TODO: Handle Imgur rate limit reached.
        .doOnNext(resolvedMediaLink -> this.resolvedMediaLink = resolvedMediaLink)
        .map(resolvedMediaLink -> {
          // Find all child images under an album.
          if (resolvedMediaLink.isMediaAlbum()) {
            return ((ImgurAlbumLink) resolvedMediaLink).images();
          } else {
            return Collections.singletonList(resolvedMediaLink);
          }
        })
        // Toggle HD for all images with the default value.
        .flatMapSingle(mediaLinks -> highResolutionMediaNetworkStrategyPref.get().asObservable()
            .flatMap(strategy -> networkStateListener.streamNetworkInternetCapability(strategy, Optional.empty()))
            .firstOrError()
            .doOnSuccess(canLoadHighResolutionMedia -> {
              if (canLoadHighResolutionMedia) {
                hdEnabledMediaLinks.addAll(mediaLinks);
                hdEnabledMediaLinksStream.accept(hdEnabledMediaLinks);
              }
            })
            .map(o -> mediaLinks)
        )
        .flatMap(mediaLinks -> {
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
        .compose(RxUtils.applySchedulers())
        .takeUntil(lifecycle().onDestroy())
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

              if (resolvedError.isUnknown()) {
                Timber.e(error, "Error while loading media: %s", mediaLinkToDisplay.unparsedUrl());
              }
            }
        );
  }

  private void moveToScreenState(ScreenState screenState) {
    resolveProgressView.setVisibility(screenState == ScreenState.RESOLVING_LINK ? View.VISIBLE : View.GONE);
    resolveErrorViewContainer.setVisibility(screenState == ScreenState.FAILED ? View.VISIBLE : View.GONE);
    mediaAlbumPager.setVisibility(screenState == ScreenState.LINK_RESOLVED ? View.VISIBLE : View.GONE);
  }

  private void startListeningToViewPagerPageChanges() {
    RxViewPager.pageSelections(mediaAlbumPager)
        .map(currentItem -> mediaAlbumAdapter.getDataSet().get(currentItem))
        .doOnNext(activeMediaItem -> updateContentDescriptionOfOptionButtonsAccordingTo(activeMediaItem))
        .doOnNext(activeMediaItem -> updateShareMenuFor(activeMediaItem))
        .doOnNext(activeMediaItem -> updateMediaDisplayPosition())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(viewpagerPageChangeStream);
  }

  private void updateMediaDisplayPosition() {
    int totalMediaItems = mediaAlbumAdapter.getCount();
    mediaPositionTextView.setVisibility(totalMediaItems > 1 ? View.VISIBLE : View.INVISIBLE);
    // Always INVISIBLE and never GONE to maintain consistency and support muscle memory.

    if (totalMediaItems > 1) {
      // We're dealing with an album.
      mediaPositionTextView.setText(getString(R.string.mediaalbumviewer_media_position, mediaAlbumPager.getCurrentItem() + 1, totalMediaItems
      ));
    }
  }

  private void setupFlickDismissForProgressView() {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(this));
    flickListener.setFlickThresholdSlop(FlickGestureListener.DEFAULT_FLICK_THRESHOLD);
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
      public void onMoveMedia(float moveRatio) {
        // Intentionally ignored to avoid resetting dimming when images/videos eventually load.
      }
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
  public void toggleImmersiveMode() {
    systemUiHelper.toggle();
  }

  @Override
  public int getDeviceDisplayWidth() {
    return getResources().getDisplayMetrics().widthPixels;
  }

  @Override
  public Single<Optional<SubmissionPreview>> getRedditSuppliedImages() {
    Completable waitTillOnPostCreate;
    if (resolvedMediaLink == null || mediaAlbumAdapter == null) {
      // Bug workaround: Fragments are restored before onPostCreate() executes.
      waitTillOnPostCreate = lifecycle()
          .onResume()
          .take(1)
          .ignoreElements();
    } else {
      waitTillOnPostCreate = Completable.complete();
    }

    return waitTillOnPostCreate
        .observeOn(io())
        .andThen(Single.fromCallable(() -> {
          if (resolvedMediaLink instanceof ImgurAlbumLink || mediaAlbumAdapter.getCount() > 1) {
            // Child pages do not know if they're part of an album. Don't let
            // them replace imgur images with reddit-supplied album-cover image.
            return Optional.<SubmissionPreview>empty();
          }

          if (getIntent().hasExtra(KEY_REDDIT_SUPPLIED_IMAGE)) {
            SubmissionPreview redditPreview = (SubmissionPreview) getIntent().getSerializableExtra(KEY_REDDIT_SUPPLIED_IMAGE);
            return Optional.of(redditPreview);
          } else {
            return Optional.<SubmissionPreview>empty();
          }
        }))
        .observeOn(mainThread());
  }

  @Override
  public Single<Integer> optionButtonsHeight() {
    return Single.create(emitter ->
        Views.executeOnMeasure(
            contentOptionButtonsContainerView,
            () -> emitter.onSuccess(contentOptionButtonsContainerView.getHeight())
        )
    );
  }

  @Override
  public Observable<Boolean> systemUiVisibilityStream() {
    return systemUiVisibilityStream;
  }

  @Override
  public void onFlickDismissEnd(long flickAnimationDuration) {
    Observable.timer(flickAnimationDuration, TimeUnit.MILLISECONDS)
        .doOnSubscribe(o -> animateMediaOptionsVisibility(false, Animations.INTERPOLATOR, 200, false))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> finish());
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

// ======== SHARE ======== //

  /**
   * Menu items get inflated in {@link #onPostCreate(Bundle)} on page change depending upon the current media's type (image or video).
   */
  private PopupMenu createSharePopupMenu() {
    // Note: the style sets a negative top offset so that the popup appears on top of the share button.
    PopupMenu sharePopupMenu = new PopupMenu(this, shareButton, Gravity.TOP, 0, R.style.DankPopupMenu_AlbumViewerOption);
    sharePopupMenu.setOnMenuItemClickListener(item -> {
      MediaAlbumItem activeMediaItem = mediaAlbumAdapter.getDataSet().get(mediaAlbumPager.getCurrentItem());

      switch (item.getItemId()) {
        case R.id.action_share_image:
          findHighestResImageFileFromCache(activeMediaItem)
              .map(imageFile -> {
                // Glide uses random file names, without any extensions. Certain apps like Messaging
                // fail to parse images if there's no file format, so we'll have to create a copy.
                String imageNameWithExtension = Urls.parseFileNameWithExtension(activeMediaItem.mediaLink().highQualityUrl());
                File imageFileWithExtension = new File(imageFile.getParent(), imageNameWithExtension);
                Files2.INSTANCE.copy(imageFile, imageFileWithExtension);
                return imageFileWithExtension;
              })
              .compose(RxUtils.applySchedulersSingle())
              .takeUntil(lifecycle().onDestroyFlowable())
              .subscribe(
                  imageFile -> {
                    Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), imageFile);
                    startActivity(Intents.createForSharingMedia(this, contentUri));
                  },
                  error -> {
                    if (error instanceof NoSuchElementException) {
                      Toast.makeText(this, R.string.mediaalbumviewer_share_image_not_loaded_yet, Toast.LENGTH_SHORT).show();
                    } else {
                      ResolvedError resolvedError = Dank.errors().resolve(error);
                      Toast.makeText(this, resolvedError.errorMessageRes(), Toast.LENGTH_LONG).show();
                    }
                  }
              );
          break;

        case R.id.action_share_video:
          Single.just(activeMediaItem.mediaLink())
              .map(mediaLink -> {
                String highQualityUrl = mediaLink.highQualityUrl();
                VideoFormat videoFormat = VideoFormat.parse(highQualityUrl);

                if (videoFormat.canBeCached()) {
                  if (videoCacheServer.isCached(highQualityUrl)) {
                    String cachedVideoFileUrl = videoCacheServer.getProxyUrl(highQualityUrl);
                    return new File(Uri.parse(cachedVideoFileUrl).getPath());

                  } else if (videoCacheServer.isCached(mediaLink.lowQualityUrl())) {
                    String cachedVideoFileUrl = videoCacheServer.getProxyUrl(mediaLink.lowQualityUrl());
                    return new File(Uri.parse(cachedVideoFileUrl).getPath());

                  } else {
                    throw new VideoNotCachedYetException();
                  }
                } else {
                  throw new AssertionError("Share-video option shouldn't be visible for non-mp4 videos.");
                }
              })
              .takeUntil(lifecycle().onDestroyFlowable())
              .subscribe(
                  videoFile -> {
                    Timber.i("videoFile: %s", videoFile);
                    Uri contentUri = FileProvider.getUriForFile(this, getString(R.string.file_provider_authority), videoFile);
                    startActivity(Intents.createForSharingMedia(this, contentUri));
                  },
                  error -> {
                    if (error instanceof VideoNotCachedYetException) {
                      Toast.makeText(this, R.string.mediaalbumviewer_share_video_not_loaded_yet, Toast.LENGTH_SHORT).show();
                    } else {
                      ResolvedError resolvedError = Dank.errors().resolve(error);
                      Toast.makeText(this, resolvedError.errorMessageRes(), Toast.LENGTH_LONG).show();
                    }
                  }
              );
          break;

        case R.id.action_share_album_url:
          startActivity(Intents.createForSharingUrl(this, resolvedMediaLink.unparsedUrl()));
          break;

        case R.id.action_share_image_url:
        case R.id.action_share_video_url:
          startActivity(Intents.createForSharingUrl(this, activeMediaItem.mediaLink().highQualityUrl()));
          break;

        default:
          throw new AssertionError();
      }

      return true;
    });
    return sharePopupMenu;
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
      emitter.setCancellable(() -> Glide.with(this).clear(highResolutionImageTarget));
    });

    Observable<File> optimizedResImageFileStream = getRedditSuppliedImages()
        .flatMapObservable(redditImages -> Observable.create(emitter -> {
          ImageWithMultipleVariants imageVariants = ImageWithMultipleVariants.Companion.of(redditImages);
          String optimizedQualityImageForDevice = imageVariants.findNearestFor(getDeviceDisplayWidth(), albumItem.mediaLink().lowQualityUrl());

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
          emitter.setCancellable(() -> Glide.with(this).clear(optimizedResolutionImageTarget));
        }));

    return highResImageFileStream
        .onErrorResumeNext(Observable.empty())
        .concatWith(optimizedResImageFileStream.onErrorResumeNext(Observable.empty()))
        .firstOrError();
  }

  private void updateShareMenuFor(MediaAlbumItem activeMediaItem) {
    sharePopupMenu.getMenu().clear();
    int menuRes = activeMediaItem.mediaLink().isVideo() ? R.menu.menu_share_video : R.menu.menu_share_image;
    getMenuInflater().inflate(menuRes, sharePopupMenu.getMenu());

    MenuItem shareAlbumMenuItem = sharePopupMenu.getMenu().findItem(R.id.action_share_album_url);
    shareAlbumMenuItem.setVisible(resolvedMediaLink.isMediaAlbum());

    MenuItem shareVideoMenuItem = sharePopupMenu.getMenu().findItem(R.id.action_share_video);
    if (shareVideoMenuItem != null) {
      VideoFormat videoFormat = VideoFormat.parse(activeMediaItem.mediaLink().highQualityUrl());
      shareVideoMenuItem.setEnabled(videoFormat.canBeCached());
    }
  }

  @OnClick(R.id.mediaalbumviewer_share)
  void onClickShareMedia() {
    sharePopupMenu.show();
  }

// ======== END SHARE ======== //

  @OnClick(R.id.mediaalbumviewer_download)
  void onClickDownloadMedia() {
    rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
