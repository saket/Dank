package me.saket.dank.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * WROTE THIS CLASS MANY MONTHS AGO. HAS TO BE SHITTY.
 *
 * Sends a chain of onClick() events on a View.
 */
public class ChainOnClickSender implements View.OnTouchListener {

  private static final int LONG_PRESS_DELAY_MS = ViewConfiguration.getLongPressTimeout();
  private static final int GAP_BETWEEN_CLICKS = 150;

  private WeakReference<View> viewRef;
  private final ChainEventHandler chainEventHandler;
  private final LongPressHandler longPressHandler;
  private boolean longPressRegistered;

  private float downX;
  private float downY;
  private final int touchSlop;

  public ChainOnClickSender(Context context, View view) {
    viewRef = new WeakReference<>(view);
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    // For scheduling onClick events.
    chainEventHandler = new ChainEventHandler(this);
    longPressHandler = new LongPressHandler(this);
  }

  @Override
  @SuppressLint("ClickableViewAccessibility")
  public boolean onTouch(View view, MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        downX = event.getRawX();
        downY = event.getRawY();

        // Schedule a long-press if the finger isn't lifted soon.
        longPressRegistered = false;
        longPressHandler.schedule();
        return true;

      case MotionEvent.ACTION_UP:
        // If lifted before a long-press could happen.
        if (!longPressRegistered) {
          final float moveXAbs = Math.abs(downX - event.getRawX());
          final float moveYAbs = Math.abs(downY - event.getRawY());

          // Send an on-click event if this was not a swipe.
          if (moveXAbs <= touchSlop && moveYAbs <= touchSlop) {
            sendOnClick();
          }
        }

      case MotionEvent.ACTION_OUTSIDE:
      case MotionEvent.ACTION_CANCEL:
        // STAHPP ZE LONG PREZZ.
        if (longPressRegistered) {
          stopSendingEvents();
        }

        // And cancel any scheduled long-press event.
        longPressHandler.cancel();
        return true;
    }

    return false;
  }

  private void sendOnClick() {
    final View view = viewRef.get();
    if (view != null) {
      Timber.i("Performing click");
      view.performClick();
    } else
      stopSendingEvents();
  }

  /**
   * Decides whether or not the View's onClick listener can be called. Returning false
   * will immediately stop the chain.
   */
  protected boolean canPerformOnClick() {
    return viewRef.get() != null;
  }

  /**
   * Schedule sending of on-click events with a delay of {@link #CHAIN_TRIGGER_DELAY_NORMAL}
   */
  private void scheduleSendingEvents() {
    scheduleNextOnClick();
  }

  /**
   * Start sending on-click events right away.
   */
  private void startSendingEvents() {
    Timber.i("startSendingEvents()");

    // Since a long-press event has just been triggered, we'll perform
    // an on-click right away and schedule the rest with some delay.
    sendOnClick();
    scheduleSendingEvents();
  }

  /**
   * Schedule sending of on-click events with {@param delay}
   */
  private void scheduleNextOnClick() {
    Timber.i("scheduleNextOnClickIn()");
    // Remove any previously scheduled call and schedule a new one.
    stopSendingEvents();
    chainEventHandler.schedule(GAP_BETWEEN_CLICKS);
  }

  private void stopSendingEvents() {
    Timber.i("stopSendingEvents()");
    chainEventHandler.stop();
  }

  /**
   * Send on-click events on regular intervals.
   */
  private static class ChainEventHandler extends Handler {
    private final ChainOnClickSender sender;

    public void schedule(int delay) {
      Message message = Message.obtain(this, 0, delay);
      sendMessageDelayed(message, delay);
    }

    public void stop() {
      removeMessages(0);
    }

    public ChainEventHandler(ChainOnClickSender sender) {
      this.sender = sender;
    }

    @Override
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      sender.sendOnClick();
      sender.scheduleNextOnClick();
    }
  }

  /**
   * Trigger when the user has made a long-press.
   */
  private static class LongPressHandler extends Handler {

    private final ChainOnClickSender sender;

    public void schedule() {
      cancel();
      sendEmptyMessageDelayed(0, LONG_PRESS_DELAY_MS);
    }

    public void cancel() {
      removeMessages(0);
    }

    public LongPressHandler(ChainOnClickSender sender) {
      this.sender = sender;
    }

    @Override
    public void handleMessage(Message message) {
      super.handleMessage(message);

      Timber.i("Long pressing started");
      sender.longPressRegistered = true;
      sender.startSendingEvents();
    }
  }
}
