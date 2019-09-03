package me.saket.dank.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;

import androidx.annotation.CheckResult;

import io.reactivex.CompletableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Utility methods related to Rx.
 */
public class RxUtils {

  /**
   * A transformer that makes an Observable execute its computation (or emit items) inside an IO thread and the
   * operators that follow this method call (including the Subscriber) execute on the main thread. This should ideally
   * be called right before (or as close as possible) to the subscribe() call to ensure any other operator doesn't
   * accidentally get executed on the main thread.
   */
  public static <T> ObservableTransformer<T, T> applySchedulers() {
    return observable -> observable
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static <T> SingleTransformer<T, T> applySchedulersSingle() {
    return observable -> observable
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static CompletableTransformer applySchedulersCompletable() {
    return observable -> observable
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public static <T> Consumer<T> doNothing() {
    return t -> {};
  }

  public static Action doNothingCompletable() {
    return () -> {};
  }

  @SuppressLint("TimberExceptionLogging")
  public static Consumer<Throwable> logError(String errorMessage, Object... args) {
    return error -> Timber.e(error, errorMessage, args);
  }

  /**
   * Calls true on <var>consumer</var> when the stream is subscribed to and false
   * when the first event is emitted or the stream is terminated.
   */
  public static <T> ObservableTransformer<T, T> onStartAndFirstEvent(Consumer<Boolean> consumer) {
    return observable -> observable
        .doOnSubscribe(o -> consumer.accept(true))
        .doOnNext(o -> consumer.accept(false))
        .doOnTerminate(() -> consumer.accept(false));
  }

  /**
   * Calls true on <var>consumer</var> when the stream is subscribed to and false when it terminates (with success or failure).
   */
  public static <T> SingleTransformer<T, T> doOnSingleStartAndTerminate(Consumer<Boolean> consumer) {
    return observable -> observable
        .doOnSubscribe(o -> consumer.accept(true))
        .doAfterTerminate(() -> consumer.accept(false));
  }

  /**
   * Calls true on <var>consumer</var> when the stream is subscribed to and false when it finishes.
   */
  public static CompletableTransformer doOnCompletableStartAndTerminate(Consumer<Boolean> consumer) {
    return observable -> observable
        .doOnSubscribe(o -> consumer.accept(true))
        .doAfterTerminate(() -> consumer.accept(false));
  }

  /**
   * Run <var>oneShotConsumer</var> after the stream emits an item, exactly once.
   */
  public static <T> ObservableTransformer<T, T> doOnceAfterNext(Consumer<T> oneShotConsumer) {
    return observable -> observable.doAfterNext(new Consumer<T>() {
      boolean isFirstDoOnNext = true;

      @Override
      public void accept(T t) throws Exception {
        if (isFirstDoOnNext) {
          oneShotConsumer.accept(t);
        }
        isFirstDoOnNext = false;
      }
    });
  }

  /**
   * Run <var>oneShotConsumer</var> after the stream emits an item, exactly once.
   */
  public static <T> ObservableTransformer<T, T> doOnceOnNext(Consumer<T> oneShotConsumer) {
    return observable -> observable.doOnNext(new Consumer<T>() {
      boolean isFirstDoOnNext = true;

      @Override
      public void accept(T t) throws Exception {
        if (isFirstDoOnNext) {
          oneShotConsumer.accept(t);
        }
        isFirstDoOnNext = false;
      }
    });
  }

  /**
   * Replay last item in upstream when <var>stream</var> emits.
   */
  public static <T> ObservableTransformer<T, T> replayLastItemWhen(Observable<Object> stream) {
    return upstream -> upstream.flatMap(item -> stream.map(o -> item).startWith(item));
  }

  @CheckResult
  public static <T> Consumer<T> errorIfMainThread() {
    return o -> {
      boolean isMainThread = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
          ? Looper.getMainLooper().isCurrentThread()
          : Looper.getMainLooper() == Looper.myLooper();

      if (isMainThread) {
        //Timber.w("Is on main thread!");
        throw new AssertionError("Is on main thread!");
      }
    };
  }
}
