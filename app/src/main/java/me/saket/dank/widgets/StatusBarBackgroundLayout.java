package me.saket.dank.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.widget.FrameLayout;

import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.Views;

/**
 * Draws a solid color behind the status bar. This is used in {@link DankPullCollapsibleActivity} because
 * Android draws a translucent black background on status bar. As a result, when a new expandable activity is
 * opening, the translucent black color of the new and the old activity combine to form a darker black color
 * until the activity's expanding content has reached the status bar. So it feels like the status bar dims for
 * a short moment before turning normal.
 */
public class StatusBarBackgroundLayout extends FrameLayout {

    private Rect statusBarRect;
    private Paint statusBarPaint;

    public StatusBarBackgroundLayout(@NonNull Context context) {
        super(context);
        init(android.R.color.transparent);
    }

    public StatusBarBackgroundLayout(@NonNull Context context, @ColorRes int statusBarBackgroundColor) {
        super(context);
        init(statusBarBackgroundColor);
    }

    private void init(int statusBarBackgroundColor) {
        setWillNotDraw(false);

        statusBarPaint = new Paint();
        statusBarPaint.setColor(ContextCompat.getColor(getContext(), statusBarBackgroundColor));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (statusBarRect == null) {
            statusBarRect = new Rect(0, 0, getRight(), Views.statusBarHeight(getResources()));
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(statusBarRect, statusBarPaint);
        super.draw(canvas);
    }

}
