package me.saket.dank.utils;

import android.widget.PopupMenu;
import android.widget.PopupWindow;

import androidx.annotation.CheckResult;

import com.jakewharton.rxbinding2.internal.Notification;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.MainThreadDisposable;

import static me.saket.dank.utils.Preconditions.checkMainThread;
import static me.saket.dank.utils.Preconditions.checkNotNull;

public final class RxPopupWindow {

  /**
   * Create an observable which emits on {@code view} dismiss events. The emitted value is
   * unspecified and should only be used as notification.
   * <p>
   * <em>Warning:</em> The created observable keeps a strong reference to {@code view}.
   * Unsubscribe to free this reference.
   * <p>
   * <em>Warning:</em> The created observable uses {@link PopupMenu#setOnDismissListener} to
   * observe dismiss change. Only one observable can be used for a view at a time.
   */
  @CheckResult
  public static Observable<Object> dismisses(PopupWindow view) {
    checkNotNull(view, "view == null");
    return new PopupWindowDismissObservable(view);
  }

  private RxPopupWindow() {}

  static final class PopupWindowDismissObservable extends Observable<Object> {
    private final PopupWindow view;

    PopupWindowDismissObservable(PopupWindow view) {
      this.view = view;
    }

    @Override
    protected void subscribeActual(Observer<? super Object> observer) {
      if (!checkMainThread(observer)) {
        return;
      }
      Listener listener = new Listener(view, observer);
      view.setOnDismissListener(listener);
      observer.onSubscribe(listener);
    }

    static final class Listener extends MainThreadDisposable implements PopupWindow.OnDismissListener {
      private final PopupWindow view;
      private final Observer<? super Object> observer;

      Listener(PopupWindow view, Observer<? super Object> observer) {
        this.view = view;
        this.observer = observer;
      }

      @Override
      public void onDismiss() {
        if (!isDisposed()) {
          observer.onNext(Notification.INSTANCE);
          observer.onComplete();
        }
      }

      @Override
      protected void onDispose() {
        view.setOnDismissListener(null);
      }
    }
  }
}
