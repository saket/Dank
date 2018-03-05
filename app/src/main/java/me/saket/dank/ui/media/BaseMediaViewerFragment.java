package me.saket.dank.ui.media;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import javax.inject.Inject;

import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.MediaAlbumViewerTitleDescriptionView;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

/**
 * Includes common logic for showing title & description and dimming the image when the description is scrolled.
 */
public abstract class BaseMediaViewerFragment extends DankFragment {

  @Inject DankLinkMovementMethod linkMovementMethod;

  private MediaAlbumViewerTitleDescriptionView titleDescriptionView;
  private View imageDimmingView;
  private MediaLink mediaLinkToShow;

  /**
   * Called when the data-set changes for this fragment. Currently happens when the HD button is toggled.
   */
  public abstract void handleMediaItemUpdate(MediaAlbumItem updatedMediaAlbumItem);

  public void setTitleDescriptionView(MediaAlbumViewerTitleDescriptionView titleDescriptionView) {
    this.titleDescriptionView = titleDescriptionView;
    Views.setMarginTop(titleDescriptionView, Views.statusBarHeight(getResources()));
  }

  public void setImageDimmingView(View imageDimmingView) {
    this.imageDimmingView = imageDimmingView;
  }

  public void setMediaLink(MediaLink mediaLink) {
    this.mediaLinkToShow = mediaLink;
  }

  @Override
  public void onAttach(Context context) {
    Dank.dependencyInjector().inject(this);
    super.onAttach(context);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Toggle description's background dimming when the description is scrolled or Activity goes immersive.
    ((ViewGroup) view).getLayoutTransition().setDuration(200);
    titleDescriptionView.streamDimmingRequiredForTitleAndDescription()
        .distinctUntilChanged()
        .mergeWith(((MediaFragmentCallbacks) getActivity()).systemUiVisibilityStream()
            .map(systemUiVisible -> systemUiVisible && titleDescriptionView.streamDimmingRequiredForTitleAndDescription().getValue())
        )
        .takeUntil(lifecycle().onDestroy())
        .subscribe(dimmingRequired -> imageDimmingView.setVisibility(dimmingRequired ? View.VISIBLE : View.GONE));

    // Hide title & description when Activity goes immersive.
    ((MediaFragmentCallbacks) getActivity()).systemUiVisibilityStream()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(systemUiVisible -> {
          titleDescriptionView.setVisibility(systemUiVisible ? View.VISIBLE : View.INVISIBLE);
        });

    // Show title and description.
    ((MediaFragmentCallbacks) getActivity()).optionButtonsHeight()
        .takeUntil(lifecycle().onDestroy().ignoreElements())
        .subscribe(optionsHeight -> {
          Views.setPaddingBottom(titleDescriptionView, titleDescriptionView.getPaddingBottom() + optionsHeight);

          if (mediaLinkToShow instanceof ImgurLink) {
            String title = ((ImgurLink) mediaLinkToShow).title();
            String description = ((ImgurLink) mediaLinkToShow).description();
            titleDescriptionView.setTitleAndDescription(title, description);

            if (description != null) {
              titleDescriptionView.descriptionView.setMovementMethod(linkMovementMethod);
              Linkify.addLinks(titleDescriptionView.descriptionView, Linkify.ALL);
            }
          }
        });
  }

  public FlickGestureListener createFlickGestureListener(FlickGestureListener.GestureCallbacks wrappedGestureCallbacks) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()));
    flickListener.setFlickThresholdSlop(FlickGestureListener.DEFAULT_FLICK_THRESHOLD);
    flickListener.setGestureCallbacks(new FlickGestureListener.GestureCallbacks() {
      @Override
      public void onFlickDismissEnd(long flickAnimationDuration) {
        wrappedGestureCallbacks.onFlickDismissEnd(flickAnimationDuration);
      }

      @Override
      public void onMoveMedia(float moveRatio) {
        wrappedGestureCallbacks.onMoveMedia(moveRatio);

        boolean isImageBeingMoved = moveRatio != 0f;
        titleDescriptionView.setVisibility(!isImageBeingMoved ? View.VISIBLE : View.INVISIBLE);

        boolean showDimming = !isImageBeingMoved && titleDescriptionView.streamDimmingRequiredForTitleAndDescription().getValue();
        imageDimmingView.setVisibility(showDimming ? View.VISIBLE : View.GONE);
      }
    });
    return flickListener;
  }
}
