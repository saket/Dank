package me.saket.dank.ui.media;

import android.content.Context;
import android.support.annotation.Px;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Stupid GestureViews always disallows touch interception on the parent ViewGroups and manually passes
 * touch events to the ViewPager. This ViewPager works around the problem by manually keeping a track of
 * the flag.
 */
public class ViewPagerWithManualScrollBlock extends ViewPager {

  private boolean scrollingBlocked;

  public ViewPagerWithManualScrollBlock(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setScrollingBlocked(boolean blocked) {
    scrollingBlocked = blocked;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    if (scrollingBlocked) {
      return false;
    }
    return super.onInterceptTouchEvent(ev);
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    if (scrollingBlocked) {
      return false;
    }
    return super.onTouchEvent(ev);
  }

  @Override
  public void scrollBy(@Px int x, @Px int y) {
    throw new AssertionError("Did not expect this. Should block scroll if needed");
  }
}
