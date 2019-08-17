package me.saket.dank;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import timber.log.Timber;

/**
 * For debugging:
 * RuntimeException: Canvas: trying to use a recycled bitmap android.graphics.Bitmap@54a85ba.
 *
 * @deprecated The issue is now solved, but keeping it around for a while just in case it shows up again.
 */
public class ImageViewWithStackTraceName extends AppCompatImageView {

  public ImageViewWithStackTraceName(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  @SuppressLint("DrawAllocation")
  protected void onDraw(Canvas canvas) {
    try {
      super.onDraw(canvas);
    } catch (RuntimeException e) {
      if (e.getMessage().contains("trying to use a recycled bitmap")) {
        String viewId = getResources().getResourceName(getId());
        Timber.e(new RuntimeException("CAUSED HERE: " + viewId, e));
      } else {
        throw e;
      }
    }
  }
}
