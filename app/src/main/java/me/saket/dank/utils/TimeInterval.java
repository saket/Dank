package me.saket.dank.utils;

import androidx.annotation.NonNull;

import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.exceptions.Exceptions;

@AutoValue
public abstract class TimeInterval {

  public abstract long interval();

  public abstract TimeUnit timeUnit();

  public long intervalMillis() {
    return timeUnit().toMillis(interval());
  }

  public static TimeInterval create(long duration, TimeUnit timeUnit) {
    return new AutoValue_TimeInterval(duration, timeUnit);
  }

  public static JsonAdapter<TimeInterval> jsonAdapter(Moshi moshi) {
    return new AutoValue_TimeInterval.MoshiJsonAdapter(moshi);
  }

  public static class TimeUnitPrefConverter implements Preference.Converter<TimeInterval> {
    private final Moshi moshi;
    private JsonAdapter<TimeInterval> adapter;

    public TimeUnitPrefConverter(Moshi moshi) {
      this.moshi = moshi;
    }

    @NonNull
    @Override
    public TimeInterval deserialize(@NonNull String serialized) {
      JsonAdapter<TimeInterval> adapter = adapter();
      try {
        //noinspection ConstantConditions
        return adapter.fromJson(serialized);
      } catch (IOException e) {
        throw Exceptions.propagate(e);
      }
    }

    @NonNull
    @Override
    public String serialize(@NonNull TimeInterval value) {
      JsonAdapter<TimeInterval> adapter = adapter();
      //noinspection ConstantConditions
      return adapter.toJson(value);
    }

    private JsonAdapter<TimeInterval> adapter() {
      if (adapter == null) {
        adapter = moshi.adapter(TimeInterval.class);
      }
      return adapter;
    }
  }
}
