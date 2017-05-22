package me.saket.dank.utils;

import android.graphics.Point;
import android.text.Spannable;
import android.view.MotionEvent;
import android.widget.TextView;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;

/**
 * Extension of BetterLinkMovementMethod that also tracks the coordinates of a clicked link.
 */
public class DankLinkMovementMethod extends BetterLinkMovementMethod {

  private Point clickedUrlCoordinates;

  public static DankLinkMovementMethod newInstance() {
    return new DankLinkMovementMethod();
  }

  @Override
  public boolean onTouchEvent(TextView view, Spannable text, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      // A link is potentially going to be clicked.
      clickedUrlCoordinates = new Point((int) event.getRawX(), (int) event.getRawY());
    }
    return super.onTouchEvent(view, text, event);
  }

  public Point getLastUrlClickCoordinates() {
    return clickedUrlCoordinates;
  }

}
