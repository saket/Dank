package me.saket.dank;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * For debugging:
 * RuntimeException: Canvas: trying to use a recycled bitmap android.graphics.Bitmap@54a85ba.
 */
public class ImageViewWithStackTraceName extends AppCompatImageView {

  public ImageViewWithStackTraceName(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    try {
      super.onDraw(canvas);
    } catch (RuntimeException e) {
      if (e.getMessage().contains("trying to use a recycled bitmap")) {
        String viewId = getResources().getResourceName(getId());
        throw new RuntimeException("CAUSED HERE: " + viewId, e);
      } else {
        throw e;
      }
    }
  }
}
