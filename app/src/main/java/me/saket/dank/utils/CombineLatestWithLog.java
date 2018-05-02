package me.saket.dank.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;

import com.google.auto.value.AutoValue;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function4;
import io.reactivex.functions.Function5;
import io.reactivex.functions.Function6;
import me.saket.dank.BuildConfig;
import timber.log.Timber;

@SuppressLint("UseSparseArrays")
@TargetApi(Build.VERSION_CODES.N)
public class CombineLatestWithLog {

  @AutoValue
  public static abstract class O<T> {
    public abstract String name();

    public abstract Observable<T> observable();

    public static <T> O<T> of(String name, Observable<T> observable) {
      return new AutoValue_CombineLatestWithLog_O<>(name, observable);
    }
  }

  private interface Profiler {

    <T> ObservableTransformer<T, T> log(String name);

    <T> ObservableTransformer<T, T> print();
  }

  private static class DebugProfiler implements Profiler {
    private long subscribeTime;
    private Map<String, Object> items = new HashMap<>();
    private Map<String, Long> delays = new HashMap<>();
    private Map<String, Long> subscribeTimes = new HashMap<>();
    private Map<String, Long> endTimes = new HashMap<>();

    public DebugProfiler() {
      subscribeTime = System.currentTimeMillis();
    }

    @Override
    public <T> ObservableTransformer<T, T> print() {
      return upstream -> upstream.compose(RxUtils.doOnceOnNext(o -> {
        boolean needsLog = false;
        int threshold = 500;

        for (Long delay : delays.values()) {
          if (delay > threshold) {
            needsLog = true;
            break;
          }
        }

        if (needsLog) {
          Timber.w("------------------------------------------------");
          items.forEach((name, item) -> {
            if (delays.get(name) > threshold) {
              Timber.w("%s = %sms [SLOW]", name, delays.get(name));
              Timber.w("Time since subscribe: %s", (endTimes.get(name) - subscribeTimes.get(name)));
            }
          });
          Timber.i("------------------------------------------------");
        }
      }));
    }

    @Override
    public <T> ObservableTransformer<T, T> log(String name) {
      return upstream -> upstream
          .doOnSubscribe(o -> subscribeTimes.put(name, System.currentTimeMillis()))
          .compose(RxUtils.doOnceOnNext(item -> {
            items.put(name, item);
            endTimes.put(name, System.currentTimeMillis());
            delays.put(name, (System.currentTimeMillis() - subscribeTime));
          }));
    }
  }

  private static class NoOpProfiler implements Profiler {

    @Override
    public <T> ObservableTransformer<T, T> log(String name) {
      return upstream -> upstream;
    }

    @Override
    public <T> ObservableTransformer<T, T> print() {
      return upstream -> upstream;
    }
  }

  private static Profiler createVariantBasedProfiler() {
    return BuildConfig.DEBUG ? new DebugProfiler() : new NoOpProfiler();
  }

  public static <T1, T2, T3, T4, R> Observable<R> from(
      O<? extends T1> source1, O<? extends T2> source2,
      O<? extends T3> source3, O<? extends T4> source4,
      Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combiner)
  {
    Profiler profiler = createVariantBasedProfiler();

    return Observable
        .combineLatest(
            source1.observable().compose(profiler.log(source1.name())),
            source2.observable().compose(profiler.log(source2.name())),
            source3.observable().compose(profiler.log(source3.name())),
            source4.observable().compose(profiler.log(source4.name())),
            combiner)
        .compose(profiler.print());
  }

  public static <T1, T2, T3, T4, T5, R> Observable<R> from(
      O<? extends T1> source1, O<? extends T2> source2,
      O<? extends T3> source3, O<? extends T4> source4,
      O<? extends T5> source5,
      Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combiner)
  {
    Profiler profiler = createVariantBasedProfiler();

    return Observable
        .combineLatest(
            source1.observable().compose(profiler.log(source1.name())),
            source2.observable().compose(profiler.log(source2.name())),
            source3.observable().compose(profiler.log(source3.name())),
            source4.observable().compose(profiler.log(source4.name())),
            source5.observable().compose(profiler.log(source5.name())),
            combiner)
        .compose(profiler.print());
  }

  public static <T1, T2, T3, T4, T5, T6, R> Observable<R> from(
      O<? extends T1> source1, O<? extends T2> source2,
      O<? extends T3> source3, O<? extends T4> source4,
      O<? extends T5> source5, O<? extends T6> source6,
      Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combiner)
  {
    Profiler profiler = createVariantBasedProfiler();

    return Observable
        .combineLatest(
            source1.observable().compose(profiler.log(source1.name())),
            source2.observable().compose(profiler.log(source2.name())),
            source3.observable().compose(profiler.log(source3.name())),
            source4.observable().compose(profiler.log(source4.name())),
            source5.observable().compose(profiler.log(source5.name())),
            source6.observable().compose(profiler.log(source6.name())),
            combiner)
        .compose(profiler.print());
  }
}
