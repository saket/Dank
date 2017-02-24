package me.saket.dank.widgets;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MotionEvent;

import butterknife.BindDimen;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.BaseExpandablePageLayout;

public class ToolbarExpandableSheet extends BaseExpandablePageLayout {

    @BindDimen(R.dimen.subreddit_toolbar_sheet_elevation) int elevationOnExpand;

    private Toolbar toolbar;

    public ToolbarExpandableSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        ButterKnife.bind(this, this);

        // Hide on start.
        Views.executeOnMeasure(this, () -> setClippedDimensions(getWidth(), 0));
    }

    public void setToolbar(Toolbar toolbar) {
        this.toolbar = toolbar;
    }

    public void toggleVisibility() {
        boolean showSheet = isClipped();
        animateDimensions(getWidth(), showSheet ? getHeight() : 0);

        toolbar.animate()
                .translationZ(showSheet ? elevationOnExpand : 0)
                .start();

        animate()
                .translationZ(showSheet ? elevationOnExpand : 0)
                .start();
    }

// ======== PUBLIC APIs END ======== //

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean touchLiesInsideVisibleRect = getClippedRect().contains(ev.getX(), ev.getY());
        return touchLiesInsideVisibleRect && super.dispatchTouchEvent(ev);
    }

}
