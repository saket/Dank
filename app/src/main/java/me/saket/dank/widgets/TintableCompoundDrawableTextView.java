package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A TextView which tints its compound drawable with the same color as its text.
 */
public class TintableCompoundDrawableTextView extends AppCompatTextView {

    public TintableCompoundDrawableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TintableCompoundDrawableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        applyColorTintToCompoundDrawables(this, getCurrentTextColor());
    }

    /**
     * Applies color tint to all compound drawables set on Views extending TextView (eg., Button(s)).
     */
    public static void applyColorTintToCompoundDrawables(TextView textView, @ColorInt int tintColor) {
        Drawable[] drawables;
        drawables = textView.getCompoundDrawablesRelative();

        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                // Wrap the drawable so that future tinting calls work on pre-v21 devices. Always use the returned drawable.
                drawables[i] = drawables[i].mutate();
                drawables[i].setTint(tintColor);
            }
        }

        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        applyColorTintToCompoundDrawables(this, color);
    }

}
