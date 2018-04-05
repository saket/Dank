package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

public interface ZoomableImageView {

  interface OnPanChangeListener {
    void onPanChange(float scrollY);
  }

  interface OnZoomChangeListener {
    void onZoomChange(float zoom);
  }

  interface OnImageTooLargeExceptionListener {
    void onImageTooLargeException(Throwable e);
  }

  void setGravity(int gravity);

  float getVisibleZoomedImageHeight();

  float getZoomedImageHeight();

  float getZoom();

  boolean canPanFurtherVertically(boolean downwardPan);

  boolean canPanAnyFurtherHorizontally(int deltaX);

  void resetState();

  void addOnImagePanChangeListener(OnPanChangeListener listener);

  void removeOnImagePanChangeListener(OnPanChangeListener listener);

  void addOnImageZoomChangeListener(OnZoomChangeListener listener);

  void removeOnImageZoomChangeListener(OnZoomChangeListener listener);

  boolean hasImage();

  int getImageHeight();

  void setOnImageTooLargeExceptionListener(OnImageTooLargeExceptionListener listener);

// ======== IMAGEVIEW ======== //

  default ImageView view() {
    return (ImageView) this;
  }

  void setOnClickListener(View.OnClickListener listener);

  void setVisibility(int visibility);

  boolean isLaidOut();

  int getHeight();

  void setTranslationY(float y);

  void setRotation(float r);

  ViewPropertyAnimator animate();

  void setImageDrawable(Drawable drawable);

  Context getContext();
}
