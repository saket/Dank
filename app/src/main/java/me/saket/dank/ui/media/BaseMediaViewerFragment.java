package me.saket.dank.ui.media;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.MediaLink;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.MediaAlbumViewerTitleDescriptionView;
import me.saket.dank.widgets.binoculars.FlickGestureListener;

/**
 * Includes common logic for showing title & description and dimming the image when the description is scrolled.
 */
public abstract class BaseMediaViewerFragment extends DankFragment {

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
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Toggle description's background dimming when the description is scrolled or Activity goes immersive.
    ((ViewGroup) view).getLayoutTransition().setDuration(200);
    unsubscribeOnDestroy(
        titleDescriptionView.streamDimmingRequiredForTitleAndDescription()
            .distinctUntilChanged()
            .mergeWith(((MediaFragmentCallbacks) getActivity()).systemUiVisibilityStream()
                .map(systemUiVisible -> systemUiVisible && titleDescriptionView.streamDimmingRequiredForTitleAndDescription().getValue())
            )
            .subscribe(dimmingRequired -> imageDimmingView.setVisibility(dimmingRequired ? View.VISIBLE : View.GONE))
    );

    // Hide title & description when Activity goes immersive.
    unsubscribeOnDestroy(
        ((MediaFragmentCallbacks) getActivity()).systemUiVisibilityStream()
            .subscribe(systemUiVisible -> {
              titleDescriptionView.setVisibility(systemUiVisible ? View.VISIBLE : View.GONE);
            })
    );

    // Show title and description.
    unsubscribeOnDestroy(
        ((MediaFragmentCallbacks) getActivity()).optionButtonsHeight()
            .subscribe(optionsHeight -> {
              Views.setPaddingBottom(titleDescriptionView, titleDescriptionView.getPaddingBottom() + optionsHeight);

              if (mediaLinkToShow instanceof ImgurLink) {
                String title = ((ImgurLink) mediaLinkToShow).title();
                String description = ((ImgurLink) mediaLinkToShow).description();
                titleDescriptionView.setTitleAndDescription(title, description);

                if (description != null) {
                  LinkMovementMethod linkMovementMethod = ((MediaFragmentCallbacks) getActivity()).getMediaDescriptionLinkMovementMethod();
                  titleDescriptionView.descriptionView.setMovementMethod(linkMovementMethod);
                  Linkify.addLinks(titleDescriptionView.descriptionView, Linkify.ALL);
                }
              }
            })
    );
  }

  public FlickGestureListener createFlickGestureListener(FlickGestureListener.GestureCallbacks wrappedGestureCallbacks) {
    FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getContext()));
    flickListener.setFlickThresholdSlop(.5f);    // Dismiss once the image is swiped 50% away.
    flickListener.setGestureCallbacks(new FlickGestureListener.GestureCallbacks() {
      @Override
      public void onFlickDismissEnd(long flickAnimationDuration) {
        wrappedGestureCallbacks.onFlickDismissEnd(flickAnimationDuration);
      }

      @Override
      public void onMoveMedia(float moveRatio) {
        wrappedGestureCallbacks.onMoveMedia(moveRatio);

        boolean isImageBeingMoved = moveRatio != 0f;
        titleDescriptionView.setVisibility(!isImageBeingMoved ? View.VISIBLE : View.GONE);

        boolean showDimming = !isImageBeingMoved && titleDescriptionView.streamDimmingRequiredForTitleAndDescription().getValue();
        imageDimmingView.setVisibility(showDimming ? View.VISIBLE : View.GONE);
      }
    });
    return flickListener;
  }
}
