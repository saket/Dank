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

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.BuildConfig;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.VoteJobService;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import timber.log.Timber;

/**
 * Handles voting on {@link Submission Submissions} & {@link Comment Comments} and storing their values
 * locally, until they're refreshed from remote again.
 * <p>
 * TODO: Clear individual pending votes when they're received from remote.
 * TODO: Do we need to worry about recycling old pending votes because their jobs timed out and expired?
 */
public class VotingManager {

  public static final String SHARED_PREFS_NAME = "sharedpreferences_votingManager";
  private static final Object NOTHING = LifecycleStreams.NOTHING;
  private static final String KEY_PENDING_VOTE_ = "pendingVote_";

  private final Application appContext;
  private final DankRedditClient dankRedditClient;
  private final SharedPreferences sharedPrefs;
  private final Moshi moshi;

  /**
   * @param appContext Used for scheduling {@link VoteJobService}.
   */
  @Inject
  public VotingManager(
      Application appContext,
      DankRedditClient dankRedditClient,
      @Named(SHARED_PREFS_NAME) SharedPreferences sharedPrefs,
      Moshi moshi)
  {
    this.appContext = appContext;
    this.dankRedditClient = dankRedditClient;
    this.sharedPrefs = sharedPrefs;
    this.moshi = moshi;
  }

  @CheckResult
  public Observable<Object> changes() {
    return Observable.create(emitter -> {
      SharedPreferences.OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> emitter.onNext(NOTHING);
      sharedPrefs.registerOnSharedPreferenceChangeListener(changeListener);
      emitter.setCancellable(() -> sharedPrefs.unregisterOnSharedPreferenceChangeListener(changeListener));
      changeListener.onSharedPreferenceChanged(sharedPrefs, "");    // Initial value.
    });
  }

  @CheckResult
  public Completable vote(PostedOrInFlightContribution contributionToVote, VoteDirection voteDirection) {
    if (!contributionToVote.isPosted()) {
      throw new AssertionError();
    }

    // Mark the vote as pending immediately so that getPendingVote() can be used immediately after calling vote().
    markVoteAsPending(contributionToVote, voteDirection);

    //noinspection ConstantConditions
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

    return Completable.fromAction(() ->
        // VotingManager uses a dedicated shared prefs file so we can safely clear everything.
        sharedPrefs.edit().clear().apply()
    );
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

  private String keyFor(String contributionFullName) {
    return KEY_PENDING_VOTE_ + contributionFullName;
  }

  private String keyFor(PostedOrInFlightContribution contribution) {
    return keyFor(contribution.fullName());
  }
}
