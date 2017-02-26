package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Outline;
import android.support.v7.widget.RecyclerView;
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

    private boolean isVisible;
    private StateChangeListener stateChangeListener;

    public interface StateChangeListener {
        void onExpandOrCollapse(boolean visible);
    }

    public ToolbarExpandableSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        ButterKnife.bind(this, this);

        // Hide on start.
        Views.executeOnMeasure(this, () -> {
            setClippedDimensions(getWidth(), 0);
            isVisible = false;
        });

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
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    public void show() {
        if (isVisible) {
            return;
        }
        isVisible = true;

        animateDimensions(getWidth(), getHeight());
        animate()
                .translationZ(elevationOnExpand)
                .setDuration(getAnimationDuration())
                .setInterpolator(getAnimationInterpolator())
                .withStartAction(() -> stateChangeListener.onExpandOrCollapse(true))
                .start();
    }

    public void hide() {
        if (!isVisible) {
            return;
        }
        isVisible = false;

        animateDimensions(getWidth(), 0);
        animate()
                .translationZ(0)
                .setDuration(getAnimationDuration())
                .setInterpolator(getAnimationInterpolator())
                .withEndAction(() -> stateChangeListener.onExpandOrCollapse(false))
                .start();
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void hideOnOutsideTouch(RecyclerView recyclerView) {
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (isVisible()) {
                    hide();
                }
                return super.onInterceptTouchEvent(rv, e);
            }
        });
    }

    public void setStateChangeListener(StateChangeListener listener) {
        stateChangeListener = listener;
    }

// ======== PUBLIC APIs END ======== //

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean touchLiesInsideVisibleRect = getClippedRect().contains(ev.getX(), ev.getY());
        return touchLiesInsideVisibleRect && super.dispatchTouchEvent(ev);
    }

}
