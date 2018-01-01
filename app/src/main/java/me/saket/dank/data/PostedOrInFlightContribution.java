package me.saket.dank.data;

import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

/**
 * A contribution that is either present on remote or is still being posted.
 */
@AutoValue
public abstract class PostedOrInFlightContribution {

  /** Null only when {@link #isPosted()} returns false. */
  @Nullable
  public abstract String fullName();

  public abstract Integer score();

  public abstract VoteDirection voteDirection();

  public abstract State state();

  public enum State {
    /**
     * Either received from the server or posted locally and synced with remote.
     * The full-name is available in this state.
     */
    POSTED,

    /**
     * Yet to be sent to remote. Full-name isn't available yet
     */
    IN_FLIGHT
  }

  public static <T extends Thing & Votable> PostedOrInFlightContribution from(T votableThing) {
    return create(votableThing.getFullName(), votableThing.getScore(), votableThing.getVote(), State.POSTED);
  }

  public static PostedOrInFlightContribution create(String fullName, Integer score, VoteDirection voteDirection, State state) {
    return new AutoValue_PostedOrInFlightContribution(fullName, score, voteDirection, state);
  }

  public boolean isPosted() {
    return state() == State.POSTED;
  }
}
