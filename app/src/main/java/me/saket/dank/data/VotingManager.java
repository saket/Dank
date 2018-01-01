package me.saket.dank.data;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.annotation.CheckResult;

import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;

import io.reactivex.Completable;
import me.saket.dank.BuildConfig;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.VoteJobService;
import timber.log.Timber;

/**
 * Handles voting on {@link Submission Submissions} & {@link Comment Comments} and storing their values
 * locally, until they're refreshed from remote again.
 * <p>
 * TODO: Clear individual pending votes when they're received from remote.
 * TODO: Do we need to worry about recycling old pending votes because their jobs timed out and expired?
 */
public class VotingManager {

  private static final String KEY_PENDING_VOTE_ = "pendingVote_";

  private Application appContext;
  private DankRedditClient dankRedditClient;
  private SharedPreferences sharedPrefs;
  private final Moshi moshi;

  /**
   * @param appContext Used for scheduling {@link VoteJobService}.
   */
  @Inject
  public VotingManager(Application appContext, DankRedditClient dankRedditClient, SharedPreferences sharedPrefs, Moshi moshi) {
    this.appContext = appContext;
    this.dankRedditClient = dankRedditClient;
    this.sharedPrefs = sharedPrefs;
    this.moshi = moshi;
  }

  private String keyFor(String contributionFullName) {
    return KEY_PENDING_VOTE_ + contributionFullName;
  }

  private String keyFor(PostedOrInFlightContribution contribution) {
    return keyFor(contribution.fullName());
  }

  @CheckResult
  public Completable vote(PostedOrInFlightContribution contributionToVote, VoteDirection voteDirection) {
    // Mark the vote as pending in onSubscribe() instead of onError(),
    // so that getPendingVote() can be used immediately after calling vote().
    markVoteAsPending(contributionToVote, voteDirection);

    VotableThingFullNameWrapper votableThing = VotableThingFullNameWrapper.create(contributionToVote.fullName());
    return dankRedditClient.withAuth(Completable.fromAction(() -> dankRedditClient.userAccountManager().vote(votableThing, voteDirection)))
//        .doOnSubscribe(o -> Timber.i("Voting for %s…", contributionToVote.fullName()()))
//        .doOnComplete(() -> Timber.i("Voting done for %s", contributionToVote.fullName()()))
        ;
  }

  /**
   * Used when we want to fire a vote and forget about the API call. This is important because we
   * don't want the retry logic to sit in the calling Activity/Fragment, which can get destroyed
   * before the API call returns and we're able to retry.
   */
  @CheckResult
  public Completable voteWithAutoRetry(PostedOrInFlightContribution contributionToVote, VoteDirection voteDirection) {
    Timber.i("Voting for %s with %s", contributionToVote.fullName(), voteDirection);

    return vote(contributionToVote, voteDirection)
        .onErrorComplete(error -> {
          if (!Dank.errors().resolve(error).isUnknown()) {
            Timber.i("Voting failed for %s. Will retry again later.", contributionToVote.fullName());

            // For network/Reddit errors, swallow the error and attempt retries later.
            VoteJobService.scheduleRetry(appContext, contributionToVote, voteDirection, moshi);
            return true;

          } else {
            return false;
          }
        });
  }

  /**
   * TODO: Remove pending votes for comments.
   * Assuming the server as the source of truth, remove pending vote for submissions that were fetched from remote.
   */
  @CheckResult
  public Completable removePendingVotesForFetchedSubmissions(List<Submission> submissionsFromRemote) {
    return Completable.fromAction(() -> {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        throw new IllegalStateException(
            "Expected to be called on a background thread but was " + Thread.currentThread().getName());
      }

      SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
      for (Submission submission : submissionsFromRemote) {
        PostedOrInFlightContribution votableSubmission = PostedOrInFlightContribution.from(submission);
        if (isVotePending(votableSubmission)) {
          //Timber.i("Removing stale pending vote for %s", ((Submission) submission).getTitle());
          sharedPrefsEditor.remove(keyFor(votableSubmission));
        }
      }
      sharedPrefsEditor.apply();
    });
  }

  public <T extends Thing & Votable> VoteDirection getPendingOrDefaultVote(T thing, VoteDirection defaultValue) {
    return VoteDirection.valueOf(sharedPrefs.getString(keyFor(thing.getFullName()), defaultValue.name()));
  }

  public VoteDirection getPendingOrDefaultVote(PostedOrInFlightContribution votableContribution, VoteDirection defaultValue) {
    return VoteDirection.valueOf(sharedPrefs.getString(keyFor(votableContribution), defaultValue.name()));
  }

  public boolean isVotePending(PostedOrInFlightContribution votableContribution) {
    return sharedPrefs.contains(keyFor(votableContribution));
  }

  private void markVoteAsPending(PostedOrInFlightContribution votableContribution, VoteDirection voteDirection) {
    sharedPrefs.edit().putString(keyFor(votableContribution), voteDirection.name()).apply();
  }

  @CheckResult
  public Completable removeAll() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }

    return Completable.fromAction(() -> {
      Set<String> keysToRemove = new HashSet<>();

      Map<String, ?> allValues = sharedPrefs.getAll();
      for (Map.Entry<String, ?> entry : allValues.entrySet()) {
        if (entry.getKey().startsWith(KEY_PENDING_VOTE_)) {
          keysToRemove.add(entry.getKey());
        }
      }

      SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
      for (String keyToRemove : keysToRemove) {
        sharedPrefsEditor.remove(keyToRemove);
      }
      sharedPrefsEditor.apply();
    });
  }

  /**
   * Get <var>thing</var>'s score assuming that any pending vote has been synced with remote.
   */
  public <T extends Thing & Votable> int getScoreAfterAdjustingPendingVote(T votableContribution) {
    return getScoreAfterAdjustingPendingVote(PostedOrInFlightContribution.from(votableContribution));
  }

  /**
   * Get <var>thing</var>'s score assuming that any pending vote has been synced with remote.
   */
  public int getScoreAfterAdjustingPendingVote(PostedOrInFlightContribution votableContribution) {
    if (!isVotePending(votableContribution)) {
      return votableContribution.score();
    }

    VoteDirection actualVoteDirection = votableContribution.voteDirection();
    VoteDirection pendingVoteDirection = getPendingOrDefaultVote(votableContribution, actualVoteDirection);

    if (actualVoteDirection == pendingVoteDirection) {
      return votableContribution.score();
    }

    int resultingScore = votableContribution.score();
    switch (pendingVoteDirection) {
      case UPVOTE:
        resultingScore += 1;
        break;

      case DOWNVOTE:
        resultingScore -= 1;
        break;

      default:
      case NO_VOTE:
        switch (votableContribution.voteDirection()) {
          case UPVOTE:
            resultingScore -= 1;
            break;

          case DOWNVOTE:
            resultingScore += 1;
            break;

          default:
          case NO_VOTE:
            throw new AssertionError();
        }
        break;
    }

    return resultingScore;
  }
}
