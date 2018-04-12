package me.saket.dank.utils;

import android.graphics.Point;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.utils.markdown.markwon.SpoilerRevealClickListenerSpan;

/**
 * Extension of BetterLinkMovementMethod that also tracks the coordinates of a clicked link.
 */
public class DankLinkMovementMethod extends BetterLinkMovementMethod {

  private Point clickedUrlCoordinates;

  public static DankLinkMovementMethod newInstance() {
    return new DankLinkMovementMethod();
  }

  public Point getLastUrlClickCoordinates() {
    return clickedUrlCoordinates;
  }

  @Override
  public boolean onTouchEvent(TextView view, Spannable text, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_UP) {
      // A link is potentially going to be clicked.
      clickedUrlCoordinates = new Point((int) event.getRawX(), (int) event.getRawY());
    }
    return super.onTouchEvent(view, text, event);
  }

  @Override
  protected void highlightUrl(TextView textView, ClickableSpan clickableSpan, Spannable text) {
    if (clickableSpan instanceof SpoilerRevealClickListenerSpan) {
      return;
    }
    super.highlightUrl(textView, clickableSpan, text);
  }

  @Override
  protected void dispatchUrlClick(TextView textView, ClickableSpan clickableSpan) {
    if (clickableSpan instanceof SpoilerRevealClickListenerSpan) {
      clickableSpan.onClick(textView);
      return;
    }
    super.dispatchUrlClick(textView, clickableSpan);
  }

  @Override
  protected void dispatchUrlLongClick(TextView textView, ClickableSpan clickableSpan) {
    if (!(clickableSpan instanceof SpoilerRevealClickListenerSpan)) {
      super.dispatchUrlLongClick(textView, clickableSpan);
    }
  }
}
