package me.saket.dank.widgets.InboxUI;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import me.saket.dank.widgets.ScrollingRecyclerViewSheet;

/**
 * Moves its child View in parallax with respect to a {@link ScrollingRecyclerViewSheet}.
 */
public class ParallaxImageContainer extends FrameLayout {

    private int parallaxLowerBounds;
    private View childView;

    public ParallaxImageContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (getChildCount() > 1) {
            throw new IllegalStateException("Can only have one child");
        }
        childView = child;
        super.addView(child, index, params);
    }

    /**
     * Set the minimum value, beyond which no parallax will be performed.
     * In our case, this will be the visible height of the child ImageView.
     */
    public void setParallaxLowerBound(int bound) {
        parallaxLowerBounds = bound;
        parallaxOffsetChild(bound);
    }

    public void syncParallaxWith(ScrollingRecyclerViewSheet sheet) {
        sheet.addOnSheetScrollChangeListener(scrollY -> {
            float sheetScrollYFromParentTop = scrollY + sheet.getTop();
            if (sheetScrollYFromParentTop > parallaxLowerBounds) {
                parallaxOffsetChild(sheetScrollYFromParentTop);
            }
        });
    }

    private void parallaxOffsetChild(float sheetScrollYFromParentTop) {
        float parallaxAmount = sheetScrollYFromParentTop - parallaxLowerBounds;
        childView.setTranslationY(-(childView.getHeight() / 2 - parallaxLowerBounds / 2) + parallaxAmount / 2);
    }

}
