package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Outline;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;

import butterknife.BindDimen;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.BaseExpandablePageLayout;

public class ToolbarExpandableSheet extends BaseExpandablePageLayout {

    @BindDimen(R.dimen.subreddit_toolbar_sheet_elevation) int elevationOnExpand;

    public ToolbarExpandableSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        ButterKnife.bind(this, this);

        // Hide on start.
        Views.executeOnMeasure(this, () -> setClippedDimensions(getWidth(), 0));

        // Avoid the shadows from showing up above the sheet. This is done by
        // passing in the center location of this sheet as the top-location
        // for the shadow, essentially hiding it behind this sheet.
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRect(0, (int) (getClippedRect().height() / 2), ((int) getClippedRect().width()), (int) getClippedRect().height());
            }
        });
    }

    public void toggleVisibility() {
        boolean showSheet = isClipped();
        animateDimensions(getWidth(), showSheet ? getHeight() : 0);

        animate()
                .translationZ(showSheet ? elevationOnExpand : 0)
                .start();
    }

    public boolean isVisible() {
        return !isClipped();
    }

// ======== PUBLIC APIs END ======== //

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean touchLiesInsideVisibleRect = getClippedRect().contains(ev.getX(), ev.getY());
        return touchLiesInsideVisibleRect && super.dispatchTouchEvent(ev);
    }

}
