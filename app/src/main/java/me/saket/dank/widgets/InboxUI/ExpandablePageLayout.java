package me.saket.dank.widgets.InboxUI;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.saket.dank.utils.Views;

/**
 * An expandable / collapsible ViewGroup to be used with a {@link InboxRecyclerView}.
 */
public class ExpandablePageLayout extends BaseExpandablePageLayout implements PullToCollapseListener.OnPullListener {

    private static final float MAX_ALPHA = 1f;
    private static final float MIN_ALPHA = 0f;

    private InboxRecyclerView inboxList;
    private View activityToolbar;                            // Toolbar inside the parent page
    private ValueAnimator toolbarAnimator;
    private State currentState;
    private PullToCollapseListener pullToCollapseListener;
    private OnPullToCollapseIntercepter onPullToCollapseIntercepter;
    private List<Callbacks> callbacks;
    private Map<String, InternalPageCallbacks> internalStateCallbacks = new HashMap<>(2);

    private boolean isFullyCoveredByNestedPage;

    public enum State {
        COLLAPSING,
        COLLAPSED,
        EXPANDING,
        EXPANDED
    }

    public interface OnPullToCollapseIntercepter {
        /**
         * Called when the user makes a vertical swipe gesture, which is registered as a pull-to-collapse gesture.
         * This is called once per gesture, when the user's finger touches this page. <code>ExpandablePage</code>
         * starts accepting this gesture if this method returns false.
         * <p>
         * So, if you have other vertically scrollable Views in your layout (RecyclerView, ListView, ScrollView, etc.),
         * you can return true to consume a gesture after verifying that the touch event lies on one of those Views.
         * This will block <code>ExpandablePage</code> from processing the gesture until the finger is lifted.
         *
         * @param downX          X-location from where the gesture started.
         * @param downY          Y-location from where the gesture started.
         * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward swipe == downward
         *                       scroll and vice versa.
         * @return True to consume this touch event. False otherwise.
         */
        boolean onInterceptPullToCollapseGesture(MotionEvent event, float downX, float downY, boolean upwardPagePull);
    }

    /**
     * Implement this to receive callbacks about an <code>ExpandingPage</code>. Use with
     * {@link ExpandablePageLayout#addCallbacks(Callbacks...)}. If you use a Fragment
     * inside your <code>ExpandablePage</code>, it's best to make your Fragment implement this interface.
     */
    public interface Callbacks {

        /**
         * Called when the user has selected an item and the <code>ExpandablePage</code> is going to be expand.
         */
        void onPageAboutToExpand(long expandAnimDuration);

        /**
         * Called when either the <code>ExpandablePage</code>'s expand animation is complete or if the
         * <code>ExpandablePage</code> was expanded immediately. At this time, the page is fully covering the list.
         */
        void onPageExpanded();

        /**
         * Called when the user has chosen to close the expanded item and the <code>ExpandablePage</code> is going to be collapse.
         */
        void onPageAboutToCollapse(long collapseAnimDuration);

        /**
         * Called when the page's collapse animation is complete. At this time, it's totally invisible to the user.
         */
        void onPageCollapsed();

    }

    // Used internally, by InboxRecyclerView.
    interface InternalPageCallbacks {
        /**
         * Called when this page has fully covered the list.
         */
        void onPageFullyCovered();

        /**
         * Called when this page is going to be collapsed.
         */
        void onPageAboutToCollapse();

        /**
         * Called when this page has fully collapsed and is no longer visible.
         */
        void onPageFullyCollapsed();

        /**
         * Page is being pulled. Sync the scroll with the list.
         */
        void onPagePull(float deltaY);

        /**
         * User released the page.
         *
         * @param collapseEligible Whether the page was pulled enough for collapsing it.
         */
        void onPageRelease(boolean collapseEligible);
    }

    public ExpandablePageLayout(Context context) {
        super(context);
        init();
    }

    public ExpandablePageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpandablePageLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ExpandablePageLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        // Hidden on start
        setAlpha(ExpandablePageLayout.MAX_ALPHA);
        setVisibility(View.INVISIBLE);
        changeState(State.COLLAPSED);

        // Handles pull-to-collapse-this-page gestures
        pullToCollapseListener = new PullToCollapseListener(getContext(), this, this);

        // Make this ViewGroup clickable so that it receives all touch events. Consume everything.
        setClickable(true);
    }

    void setup(InboxRecyclerView inboxList, View toolbar) {
        this.inboxList = inboxList;
        activityToolbar = toolbar;
        pullToCollapseListener.setToolbar(toolbar);
    }

    protected void changeState(State newState) {
        currentState = newState;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Ignore touch events until the page is fully expanded for avoiding accidental taps.
        return isExpanded() && super.dispatchTouchEvent(ev);
    }

// ======== PULL TO COLLAPSE ======== //

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return (getVisibility() == VISIBLE && pullToCollapseListener.onTouch(this, event)) || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return pullToCollapseListener.onTouch(this, event) || super.onTouchEvent(event);
    }

    @Override
    public void onPull(float deltaY, float currentTranslationY, boolean upwardPull, boolean deltaUpwardPull) {
        // In case the user pulled the page before it could fully open (and while the toolbar was still hiding).
        stopToolbarAnimation();

        // Reveal the toolbar if this page is being pulled down or hide it back if it's being released.
        updateToolbarTranslationY(currentTranslationY > 0f, currentTranslationY);

        // Sync the positions of the list items with this page.
        dispatchOnPagePullCallbacks(deltaY);
    }

    @Override
    public void onRelease(boolean collapseEligible) {
        dispatchOnPageReleasedCallback(collapseEligible);

        // The list should either collapse or animate back its items out of the list.
        if (collapseEligible) {
            dispatchOnPageAboutToCollapseCallback();

        } else {
            if (isCollapsedOrCollapsing()) {
                // Let the page collapse in peace.
                return;
            }

            changeState(State.EXPANDED);
            stopAnyOngoingPageAnimation();

            // Restore everything to their expanded position
            // 1. Hide Toolbar again
            animateToolbar(false, 0f);

            // 2. Expand page again
            if (getTranslationY() != 0f) {
                animate().translationY(0f)
                        .alpha(MAX_ALPHA)
                        .setDuration(InboxRecyclerView.ANIM_DURATION_COLLAPSE)
                        .setInterpolator(InboxRecyclerView.ANIM_INTERPOLATOR_COLLAPSE)
                        .setListener(new AnimatorListenerAdapter() {
                            boolean mCanceled;

                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                mCanceled = false;
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                super.onAnimationCancel(animation);
                                mCanceled = true;
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                if (mCanceled) {
                                    return;
                                }
                                dispatchOnPageFullyCoveredCallback();
                            }

                        })
                        .start();
            }
        }
    }

// ======== EXPAND / COLLAPSE ANIMATION ======== //

    /**
     * Expands this page (with animation) so that it fills the whole screen.
     */
    void expand(InboxRecyclerView.ExpandInfo expandInfo) {
        // Skip animations if Android hasn't measured Views yet.
        if (!isLaidOut() && getVisibility() != GONE) {
            throw new IllegalAccessError("Width / Height not available to expand");
        }

        // Ignore if already expanded.
        if (isExpandedOrExpanding()) {
            return;
        }

        // Place the expandable page on top of the expanding item.
        alignPageWithExpandingItem(expandInfo);

        // Animate!
        animatePageExpandCollapse(true /* expand */, getWidth(), getHeight(), expandInfo);

        // Callbacks, just before the animation starts.
        dispatchOnAboutToExpandCallback();
    }

    /**
     * Expands this page instantly, without any animation. Use this when the user wants to directly
     * navigate to this page, by-passing the list.
     */
    public void expandImmediately() {
        // Ignore if already expanded
        if (currentState == State.EXPANDING || currentState == State.EXPANDED) {
            return;
        }

        // Cover the whole screen right away. Don't need any animations.
        alignPageToCoverScreen();

        setVisibility(VISIBLE);
        setAlpha(MAX_ALPHA);

        // Hide the toolbar as soon as we have its height (as expandImmediately() could have been
        // called before the Views were drawn).
        // TODO: 29/01/17 Try using activityToolbar.post() instead of using executeOnMeasure().
        Views.executeOnMeasure(activityToolbar, () -> updateToolbarTranslationY(false, 0));

        // Let the list know about this so that it dims itself
        // right away and does not draw any child Views
        dispatchOnAboutToExpandCallback();
        dispatchOnFullyExpandedCallback();
    }

    /**
     * Collapses this page, back to its original state.
     */
    void collapse(InboxRecyclerView.ExpandInfo expandInfo) {
        // Ignore if already collapsed.
        if (currentState == State.COLLAPSED || currentState == State.COLLAPSING) {
            return;
        }

        // Fire!
        int targetWidth = expandInfo.expandedItemLocationRect.width();
        int targetHeight = expandInfo.expandedItemLocationRect.height();
        if (targetWidth == 0) {
            // Page must expanded immediately after a state restoration.
            targetWidth = getWidth();
        }
        animatePageExpandCollapse(false /* Collapse */, targetWidth, targetHeight, expandInfo);

        // Send callbacks that the city is going to collapse.
        dispatchOnPageAboutToCollapseCallback();
    }

    /**
     * Places the expandable page exactly on top of the expanding list item
     * (including matching its height with the list item)
     */
    protected void alignPageWithExpandingItem(InboxRecyclerView.ExpandInfo expandInfo) {
        // Match height and location.
        setClippedDimensions(
                expandInfo.expandedItemLocationRect.width(),
                expandInfo.expandedItemLocationRect.height()
        );
        setTranslationY(expandInfo.expandedItemLocationRect.top);
    }

    protected void alignPageToCoverScreen() {
        resetClipping();
        setTranslationY(0f);
    }

    protected void animatePageExpandCollapse(boolean expand, int targetWidth, int targetHeight, InboxRecyclerView.ExpandInfo expandInfo) {
        float targetPageTranslationY = expand ? 0f : expandInfo.expandedItemLocationRect.top;
        final float targetPageTranslationX = expand ? 0f : expandInfo.expandedItemLocationRect.left;

        // If there's no record about the expanded list item (from whose place this page was expanded),
        // collapse just below the toolbar and not the window top to avoid closing the toolbar upon hiding.
        if (!expand && expandInfo.expandedItemLocationRect.height() == 0) {
            targetPageTranslationY = Math.max(targetPageTranslationY, activityToolbar.getBottom());
        }

        if (expand) {
            setVisibility(View.VISIBLE);
        }

        // Alpha and translation.
        setAlpha(expand ? MIN_ALPHA : MAX_ALPHA);
        stopAnyOngoingPageAnimation();
        animate()
                .alpha(expand ? MAX_ALPHA : MIN_ALPHA)
                .translationY(targetPageTranslationY)
                .translationX(targetPageTranslationX)
                .setDuration(expand ? InboxRecyclerView.ANIM_DURATION_EXPAND : InboxRecyclerView.ANIM_DURATION_COLLAPSE)
                .setInterpolator(expand ? InboxRecyclerView.ANIM_INTERPOLATOR_EXPAND : InboxRecyclerView.ANIM_INTERPOLATOR_COLLAPSE)
                .setListener(new AnimatorListenerAdapter() {
                    private boolean mCanceled;

                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        mCanceled = false;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        super.onAnimationCancel(animation);
                        mCanceled = true;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (mCanceled) {
                            return;
                        }
                        if (!expand) {
                            setVisibility(View.INVISIBLE);
                            dispatchOnPageCollapsedCallback();
                        } else {
                            dispatchOnFullyExpandedCallback();
                        }
                    }
                })
                .setStartDelay(!expand ? 0 : InboxRecyclerView.ANIM_START_DELAY)
                .start();

        // Show the toolbar fully even if the page is going to collapse behind it
        float targetPageTranslationYForToolbar = targetPageTranslationY;
        if (!expand && targetPageTranslationYForToolbar < activityToolbar.getHeight()) {
            targetPageTranslationYForToolbar = activityToolbar.getHeight();
        }

        // Hide / show the toolbar by pushing it up during expand and pulling it down during collapse.
        animateToolbar(
                !expand,    // When expand = false -> !expand = Show toolbar
                targetPageTranslationYForToolbar
        );

        // Width & Height.
        animateDimensions(targetWidth, targetHeight);
    }

    private void animateToolbar(final boolean show, float targetPageTranslationY) {
        if (getTranslationY() == targetPageTranslationY) {
            return;
        }

        final float toolbarCurrentBottom = activityToolbar.getHeight() + activityToolbar.getTranslationY();
        final float fromTy = Math.max(toolbarCurrentBottom, getTranslationY());

        // The hide animation happens a bit too quickly if the page has to travel a large
        // distance (when using the current interpolator: EASE). Let's try slowing it down.
        long speedFactor = 1L;
        if (show && Math.abs(targetPageTranslationY - fromTy) > getClippedHeight() * 2 / 5) {
            speedFactor *= 2L;
        }

        stopToolbarAnimation();

        // If the page lies behind the toolbar, use toolbar's current bottom position instead
        toolbarAnimator = ObjectAnimator.ofFloat(fromTy, targetPageTranslationY);
        toolbarAnimator.addUpdateListener(animation -> updateToolbarTranslationY(show, (float) animation.getAnimatedValue()));
        toolbarAnimator.setDuration(show ? InboxRecyclerView.ANIM_DURATION_COLLAPSE : InboxRecyclerView.ANIM_DURATION_EXPAND * speedFactor);
        toolbarAnimator.setInterpolator(show ? InboxRecyclerView.ANIM_INTERPOLATOR_COLLAPSE : InboxRecyclerView.ANIM_INTERPOLATOR_EXPAND);
        toolbarAnimator.setStartDelay(InboxRecyclerView.ANIM_START_DELAY);
        toolbarAnimator.start();
    }

    /**
     * Helper method for showing / hiding the toolbar depending upon this page's current translationY.
     */
    private void updateToolbarTranslationY(boolean show, float pageTranslationY) {
        final int toolbarHeight = activityToolbar.getHeight();
        float targetTranslationY = pageTranslationY - toolbarHeight;

        if (show) {
            if (targetTranslationY > toolbarHeight) {
                targetTranslationY = toolbarHeight;
            }
            if (targetTranslationY > 0) {
                targetTranslationY = 0;
            }

        } else if (pageTranslationY >= toolbarHeight || activityToolbar.getTranslationY() <= -toolbarHeight) {
            // Hide
            return;
        }

        activityToolbar.setTranslationY(targetTranslationY);
    }

    void stopAnyOngoingPageAnimation() {
        animate().cancel();
        stopToolbarAnimation();
    }

    private void stopToolbarAnimation() {
        if (toolbarAnimator != null) {
            toolbarAnimator.cancel();
        }
    }

// ======== OPTIMIZATIONS ======== //

    /**
     * To be used when another ExpandablePageLayout is shown inside this page.
     * This page will avoid all draw calls while the nested page is open to
     * minimize overdraw.
     */
    public void setNestedExpandablePage(ExpandablePageLayout nestedPage) {
        nestedPage.setInternalCallbacksNestedPage(new InternalPageCallbacks() {
            @Override
            public void onPageAboutToCollapse() {
                onPageBackgroundVisible();
            }

            @Override
            public void onPageFullyCollapsed() {
            }

            @Override
            public void onPagePull(float deltaY) {
                onPageBackgroundVisible();
            }

            @Override
            public void onPageRelease(boolean collapseEligible) {
                if (collapseEligible) {
                    onPageBackgroundVisible();
                }
            }

            @Override
            public void onPageFullyCovered() {
                final boolean invalidate = !isFullyCoveredByNestedPage;
                isFullyCoveredByNestedPage = true;   // Skips draw() until visible again to the
                if (invalidate) {
                    postInvalidate();
                }
            }

            public void onPageBackgroundVisible() {
                final boolean invalidate = isFullyCoveredByNestedPage;
                isFullyCoveredByNestedPage = false;
                if (invalidate) {
                    postInvalidate();
                }
            }
        });
    }

    @Override
    public void draw(Canvas canvas) {
        // Minimize overdraw by not drawing anything while this page is totally covered by another
        // nested page.
        if (isFullyCoveredByNestedPage) {
            return;
        }

        // Or if the page is collapsed.
        if (currentState == State.COLLAPSED) {
            return;
        }
        super.draw(canvas);
    }

// ======== CALLBACKS ======== //

    private void dispatchOnPagePullCallbacks(float deltaY) {
        for (final InternalPageCallbacks internalCallback : internalStateCallbacks.values()) {
            if (internalCallback == null) {
                continue;
            }
            internalCallback.onPagePull(deltaY);
        }
    }

    private void dispatchOnPageReleasedCallback(boolean collapseEligible) {
        for (final InternalPageCallbacks internalCallback : internalStateCallbacks.values()) {
            if (internalCallback == null) {
                continue;
            }
            internalCallback.onPageRelease(collapseEligible);
        }
    }

    private void dispatchOnAboutToExpandCallback() {
        // The state change must happen after the subscribers have been
        // notified that the page is going to expand
        changeState(State.EXPANDING);

        if (callbacks != null) {
            for (int i = 0; i < callbacks.size(); i++) {    // Note: DO NOT convert to for-each loop which generates an iterator object on each call.
                callbacks.get(i).onPageAboutToExpand(InboxRecyclerView.ANIM_DURATION_EXPAND);
            }
        }
    }

    @SuppressWarnings("Convert2streamapi")
    private void dispatchOnFullyExpandedCallback() {
        changeState(State.EXPANDED);
        dispatchOnPageFullyCoveredCallback();

        if (callbacks != null) {
            for (final Callbacks callback : callbacks) {
                callback.onPageExpanded();
            }
        }
    }

    /**
     * There's a difference between the page fully expanding and fully covering the list.
     * When the page is fully expanded, it may or may not be covering the list. This is
     * usually when the user is pulling the page.
     */
    private void dispatchOnPageFullyCoveredCallback() {
        for (final InternalPageCallbacks internalCallback : internalStateCallbacks.values()) {
            if (internalCallback == null) {
                continue;
            }
            internalCallback.onPageFullyCovered();
        }
    }

    private void dispatchOnPageAboutToCollapseCallback() {
        // The state change must happen after the subscribers have been notified that the page is going to collapse.
        changeState(State.COLLAPSING);

        for (final InternalPageCallbacks internalCallback : internalStateCallbacks.values()) {
            if (internalCallback == null) {
                continue;
            }
            internalCallback.onPageAboutToCollapse();
        }

        if (callbacks != null) {
            for (final Callbacks callback : callbacks) {
                callback.onPageAboutToCollapse(InboxRecyclerView.ANIM_DURATION_COLLAPSE);
            }
        }
    }

    private void dispatchOnPageCollapsedCallback() {
        changeState(State.COLLAPSED);

        for (final InternalPageCallbacks internalCallback : internalStateCallbacks.values()) {
            if (internalCallback == null) {
                continue;
            }
            internalCallback.onPageFullyCollapsed();
        }

        if (callbacks != null) {
            for (final Callbacks callback : callbacks) {
                callback.onPageCollapsed();
            }
        }
    }

    /**
     * Calls for the associated InboxList.
     */
    void setInternalCallbacksList(InternalPageCallbacks listCallbacks) {
        internalStateCallbacks.put("List", listCallbacks);
    }

    void setInternalCallbacksNestedPage(InternalPageCallbacks pageCallbacks) {
        internalStateCallbacks.put("NestedPage", pageCallbacks);
    }

    public boolean isExpanded() {
        return currentState == State.EXPANDED;
    }

    public boolean isExpandingOrCollapsing() {
        return currentState == State.EXPANDING || currentState == State.COLLAPSING;
    }

    public boolean isCollapsing() {
        return currentState == State.COLLAPSING;
    }

    public boolean isCollapsed() {
        return currentState == State.COLLAPSED;
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean isExpanding() {
        return currentState == State.EXPANDING;
    }

    public boolean isExpandedOrExpanding() {
        return currentState == State.EXPANDED || currentState == State.EXPANDING;
    }

    public boolean isCollapsedOrCollapsing() {
        return currentState == State.COLLAPSING || currentState == State.COLLAPSED;
    }

    /**
     * The {@link InboxRecyclerView} this page is attached to.
     */
    public InboxRecyclerView getAttachedList() {
        return inboxList;
    }

    public void addCallbacks(Callbacks... callbacks) {
        if (this.callbacks == null) {
            this.callbacks = new ArrayList<>(4);
        }
        Collections.addAll(this.callbacks, callbacks);
    }

    public void removeCallbacks(Callbacks pageCallbacks) {
        callbacks.remove(pageCallbacks);
    }

    public PullToCollapseListener getPullToCollapseListener() {
        return pullToCollapseListener;
    }

    public void setPullToCollapseIntercepter(OnPullToCollapseIntercepter intercepter) {
        onPullToCollapseIntercepter = intercepter;
    }

    OnPullToCollapseIntercepter getPullToCollapseIntercepter() {
        return onPullToCollapseIntercepter;
    }

}
