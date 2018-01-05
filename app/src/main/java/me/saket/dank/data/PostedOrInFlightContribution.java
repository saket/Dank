package me.saket.dank.data;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

import me.saket.dank.ui.submission.CommentTreeConstructor;
import me.saket.dank.ui.submission.PendingSyncReply;

/**
 * A contribution that is either present on remote or is still being posted.
 * Could be a submission, a comment or a message.
 */
public interface PostedOrInFlightContribution extends Parcelable {

  enum State {
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

  /** Null only when {@link #isPosted()} returns false. */
  @Nullable
  String fullName();

  Integer score();

  VoteDirection voteDirection();

  /**
   * When posted, gestures can be registered, replies can be made, etc.
   */
  boolean isPosted();

  /**
   * Used by {@link CommentTreeConstructor} as this object's ID.
   */
  String idForTogglingCollapse();

  static <T extends Thing & Votable> PostedOrInFlightContribution from(T votableThing) {
    return ContributionFetchedFromRemote.create(votableThing.getFullName(), votableThing.getScore(), votableThing.getVote());
  }

  static PostedOrInFlightContribution from(Message message) {
    return MessageFetchedFromRemote.create(message.getFullName());
  }

  static PostedOrInFlightContribution createLocal(PendingSyncReply pendingSyncReply) {
    return LocallyPostedContribution.create(pendingSyncReply, 1, VoteDirection.UPVOTE);
  }

  @AutoValue
  abstract class ContributionFetchedFromRemote implements PostedOrInFlightContribution {
    @Override
    public boolean isPosted() {
      return true;
    }

    @Override
    public String idForTogglingCollapse() {
      return fullName();
    }

    public static ContributionFetchedFromRemote create(String fullName, Integer score, VoteDirection voteDirection) {
      return new AutoValue_PostedOrInFlightContribution_ContributionFetchedFromRemote(fullName, score, voteDirection);
    }
  }

  @AutoValue
  abstract class LocallyPostedContribution implements PostedOrInFlightContribution {

    public abstract State state();

    public abstract String idForTogglingCollapse();

    public boolean isPosted() {
      return state() == State.POSTED;
    }

    public static LocallyPostedContribution create(PendingSyncReply pendingSyncReply, Integer score, VoteDirection voteDirection) {
      State state = pendingSyncReply.state() == PendingSyncReply.State.POSTED
          ? State.POSTED
          : State.IN_FLIGHT;

      String idForTogglingCollapse = pendingSyncReply.parentContributionFullName() + "_reply_" + pendingSyncReply.createdTimeMillis();

      return new AutoValue_PostedOrInFlightContribution_LocallyPostedContribution(
          pendingSyncReply.postedFullName(),
          score,
          voteDirection,
          state,
          idForTogglingCollapse
      );
    }
  }

  @AutoValue
  abstract class MessageFetchedFromRemote implements PostedOrInFlightContribution {
    @Override
    public VoteDirection voteDirection() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Integer score() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String idForTogglingCollapse() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isPosted() {
      return true;
    }

    public static MessageFetchedFromRemote create(String fullName) {
      return new AutoValue_PostedOrInFlightContribution_MessageFetchedFromRemote(fullName);
    }
  }
}
