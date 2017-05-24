package me.saket.dank.widgets.swipe;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.widget.Space;
import android.util.AttributeSet;

/**
 * Marks the release area for a swipe action.
 */
public class SwipeReleaseTarget extends Space {

  private final float layoutWeight;

  public SwipeReleaseTarget(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (!(getBackground() instanceof ColorDrawable)) {
      throw new UnsupportedOperationException("Only solid color backgrounds can be used");
    }

    int[] attrsToObtain = {
        android.R.attr.layout_weight,
    };

    TypedArray attributes = context.obtainStyledAttributes(attrs, attrsToObtain);
    layoutWeight = attributes.getFloat(0, 1f);
    attributes.recycle();
  }

  public float layoutWeight() {
    return layoutWeight;
  }

  public int backgroundColor() {
    return ((ColorDrawable) getBackground()).getColor();
  }
}
