package me.saket.dank.ui.subreddits;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NetworkCallStatus {

  public enum State {
    IN_FLIGHT,
    IDLE,
    FAILED
  }

  public abstract State state();

  @Nullable
  public abstract Throwable error();

  public static NetworkCallStatus createInFlight() {
    return new AutoValue_NetworkCallStatus(State.IN_FLIGHT, null);
  }

  public static NetworkCallStatus createIdle() {
    return new AutoValue_NetworkCallStatus(State.IDLE, null);
  }

  public static NetworkCallStatus createFailed(Throwable error) {
    return new AutoValue_NetworkCallStatus(State.FAILED, error);
  }
}
