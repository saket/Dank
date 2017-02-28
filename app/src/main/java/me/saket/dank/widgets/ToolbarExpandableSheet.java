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

    private StateChangeListener stateChangeListener;
    private State currentState;

    public enum State {
        COLLAPSING,
        COLLAPSED,
        EXPANDING,
        EXPANDED
    }

    public interface StateChangeListener {
        void onStateChange(State newState);
    }

    public ToolbarExpandableSheet(Context context, AttributeSet attrs) {
        super(context, attrs);
        ButterKnife.bind(this, this);

        // Hide on start.
        currentState = State.COLLAPSED;
        Views.executeOnMeasure(this, () -> {
            setClippedDimensions(getWidth(), 0);
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

    public boolean isExpandedOrExpanding() {
        return currentState == State.EXPANDED || currentState == State.EXPANDING;
    }

    public void toggleVisibility() {
        if (isExpandedOrExpanding()) {
            collapse();
        } else {
            expand();
        }
    }

    public void expand() {
        if (currentState == State.EXPANDED || currentState == State.EXPANDING) {
            return;
        }

        animateDimensions(getWidth(), getHeight());
        animate()
                .translationZ(elevationOnExpand)
                .setDuration(getAnimationDuration())
                .setInterpolator(getAnimationInterpolator())
                .withStartAction(() -> dispatchStateChangeCallback(State.EXPANDING))
                .withEndAction(() -> dispatchStateChangeCallback(State.EXPANDED))
                .start();
    }

    public void collapse() {
        if (currentState == State.COLLAPSED || currentState == State.COLLAPSING) {
            return;
        }

        animateDimensions(getWidth(), 0);
        animate()
                .translationZ(0)
                .setDuration(getAnimationDuration())
                .setInterpolator(getAnimationInterpolator())
                .withStartAction(() -> dispatchStateChangeCallback(State.COLLAPSING))
                .withEndAction(() -> dispatchStateChangeCallback(State.COLLAPSED))
                .start();
    }

    public void hideOnOutsideTouch(RecyclerView recyclerView) {
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (isExpandedOrExpanding()) {
                    collapse();
                }
                return super.onInterceptTouchEvent(rv, e);
            }
        });
    }

    public void setStateChangeListener(StateChangeListener listener) {
        stateChangeListener = listener;
    }

// ======== PUBLIC APIs END ======== //

    private void dispatchStateChangeCallback(State state) {
        currentState = state;
        stateChangeListener.onStateChange(state);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean touchLiesInsideVisibleRect = getClippedRect().contains(ev.getX(), ev.getY());
        return (ev.getAction() != MotionEvent.ACTION_DOWN || touchLiesInsideVisibleRect) && super.dispatchTouchEvent(ev);
    }

}
