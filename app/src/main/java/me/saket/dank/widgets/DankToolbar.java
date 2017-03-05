package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import me.saket.dank.R;

/**
 *
 */
public class DankToolbar extends Toolbar {

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
