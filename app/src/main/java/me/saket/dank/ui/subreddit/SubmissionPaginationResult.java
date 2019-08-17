package me.saket.dank.ui.subreddit;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SubmissionPaginationResult {

  public enum Type {
    IN_FLIGHT,
    IDLE,
    FAILED
  }

  public abstract Type state();

  @Nullable
  public abstract Throwable error();

  public boolean isIdle() {
    return state() == Type.IDLE;
  }

  public boolean isInFlight() {
    return state() == Type.IN_FLIGHT;
  }

  public boolean isFailed() {
    return state() == Type.FAILED;
  }

  public static SubmissionPaginationResult inFlight() {
    return new AutoValue_SubmissionPaginationResult(Type.IN_FLIGHT, null);
  }

  public static SubmissionPaginationResult idle() {
    return new AutoValue_SubmissionPaginationResult(Type.IDLE, null);
  }

  public static SubmissionPaginationResult failed(Throwable error) {
    return new AutoValue_SubmissionPaginationResult(Type.FAILED, error);
  }


}
