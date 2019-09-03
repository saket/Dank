package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ViewFlipper;

import me.saket.dank.R;

public class ViewFlipperWithDebugPreview extends ViewFlipper {

  private int displayedChild = 0;

  public ViewFlipperWithDebugPreview(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (isInEditMode()) {
      TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ViewFlipperWithDebugPreview);
      displayedChild = attributes.getInt(R.styleable.ViewFlipperWithDebugPreview_debug_displayedChild, 0);
      attributes.recycle();
    }
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    if (isInEditMode()) {
      if (displayedChild > getChildCount()) {
        throw new IllegalStateException("displayedChild is greater than child count");
      }
      setDisplayedChild(displayedChild);
    }
  }
}
