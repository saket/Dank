package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import me.saket.dank.R;

/**
 * Better than including the toolbar layout everywhere, which is limited because adding attrs like layout_weight
 * also requires the width and height of an include tag to be set.
 * <p>
 * Also, not sure why, but padding only works if there's a navigation icon set.
 */
public class DankToolbar extends ToolbarWithCustomTypeface {

  public DankToolbar(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.DankToolbar);

    Drawable navigationIcon = attributes.getDrawable(R.styleable.DankToolbar_navigationIcon);

    if (attributes.hasValue(R.styleable.DankToolbar_navigationIconTint)) {
      //noinspection ConstantConditions
      navigationIcon = navigationIcon.mutate();
      navigationIcon.setTint(attributes.getColor(R.styleable.DankToolbar_navigationIconTint, Color.WHITE));
    }
    if (navigationIcon != null) {
      setNavigationIcon(navigationIcon);
    }
    attributes.recycle();
  }

}
