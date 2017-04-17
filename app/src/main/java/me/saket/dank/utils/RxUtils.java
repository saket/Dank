package me.saket.dank.utils;

import rx.Completable;
import rx.Notification;
import rx.Observable;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
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
    public static <T> Observable.Transformer<T, T> applySchedulers() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> Single.Transformer<T, T> applySchedulersSingle() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static Completable.Transformer applySchedulersCompletable() {
        return observable -> observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> Action1<T> doNothing() {
        return t -> {
        };
    }

    public static Action0 doNothingCompletable() {
        return () -> {

        };
    }

    /**
     * Convenience method to run something when either of onNext or onError is called. This is equivalent of manually
     * calling both {@link Observable#doOnNext(Action1)} and {@link Observable#doOnError(Action1)}.
     */
    public static Completable.Transformer doOnCompleteOrError(Action0 action) {
        return completable -> completable.doOnEach(notif -> {
            if (notif.getKind() != Notification.Kind.OnCompleted) {
                try {
                    action.call();
                } catch (Exception e) {
                    throw OnErrorThrowable.from(e);
                }
            }
        });
    }

    public static Action1<Throwable> logError(String errorMessage, Object... args) {
        return error -> Timber.e(error, errorMessage, args);
    }

    public static <T> Observable.Transformer<T, T> doOnStartAndNext(Action1<Boolean> action) {
        return observable -> observable
                .doOnSubscribe(() -> action.call(true))
                .doOnNext(o -> action.call(false))
                .doOnError(o -> action.call(false));
    }

    public static <T> Observable.Transformer<T, T> doOnStartAndEnd(Action1<Boolean> action) {
        return observable -> observable
                .doOnSubscribe(() -> action.call(true))
                .doOnUnsubscribe(() -> action.call(false));
    }

    public static <T> Single.Transformer<T, T> doOnStartAndEndSingle(Action1<Boolean> isOngoingAction) {
        return observable -> observable
                .doOnSubscribe(() -> isOngoingAction.call(true))
                .doOnUnsubscribe(() -> isOngoingAction.call(false));
    }

    public static Completable.Transformer doOnStartAndComplete(Action1<Boolean> action) {
        return observable -> observable
                .doOnSubscribe(o -> action.call(true))
                .doOnUnsubscribe(() -> action.call(false));
    }

}
