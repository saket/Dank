package me.saket.dank.data;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.annotation.CheckResult;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  /**
   * @param appContext Used for scheduling {@link VoteJobService}.
   */
  public VotingManager(Application appContext, DankRedditClient dankRedditClient, SharedPreferences sharedPrefs) {
    this.appContext = appContext;
    this.dankRedditClient = dankRedditClient;
    this.sharedPrefs = sharedPrefs;
  }

  private <T extends Thing & Votable> String keyFor(T thing) {return KEY_PENDING_VOTE_ + thing.getFullName();}

  @CheckResult
  public Completable vote(String thingFullName, VoteDirection voteDirection) {
    VotableThingFullNameWrapper thing = VotableThingFullNameWrapper.create(thingFullName);
    return vote(thing, voteDirection);
  }

  @CheckResult
  public <T extends Thing & Votable> Completable vote(T thingToVote, VoteDirection voteDirection) {
    // Mark the vote as pending in onSubscribe() instead of onError(),
    // so that getPendingVote() can be used immediately after calling vote().
    markVoteAsPending(thingToVote, voteDirection);

    return dankRedditClient.withAuth(Completable
        .fromAction(() -> dankRedditClient.userAccountManager().vote(thingToVote, voteDirection)))
        .doOnSubscribe(o -> Timber.i("Voting for %s…", thingToVote.getFullName()))
        .doOnComplete(() -> Timber.i("Voting done for %s", thingToVote.getFullName()));
  }

  /**
   * Used when we want to fire a vote and forget about the API call. This is important because we
   * don't want the retry logic to sit in the calling Activity/Fragment, which can get destroyed
   * before the API call returns and we're able to retry.
   */
  @CheckResult
  public <T extends Thing & Votable> Completable voteWithAutoRetry(T thingToVote, VoteDirection voteDirection) {
    Timber.i("Voting for %s with %s", thingToVote.getFullName(), voteDirection);

    return vote(thingToVote, voteDirection)
        .onErrorComplete(error -> {
          if (!Dank.errors().resolve(error).isUnknown()) {
            Timber.i("Voting failed for %s. Will retry again later.", thingToVote.getFullName());

            // For network/Reddit errors, swallow the error and attempt retries later.
            VoteJobService.scheduleRetry(appContext, thingToVote, voteDirection);
            return true;

          } else {
            return false;
          }
        });
  }

  /**
   * Assuming the server as the source of truth, remove pending vote for submissions that were fetched from remote.
   */
  @CheckResult
  public <T extends Thing & Votable> Completable removePendingVotesForFetchedSubmissions(List<T> thingsFromRemote) {
    return Completable.fromAction(() -> {
      SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
      for (T thing : thingsFromRemote) {
        if (isVotePending(thing)) {
          //Timber.i("Removing stale pending vote for %s", ((Submission) thing).getTitle());
          sharedPrefsEditor.remove(keyFor(thing));
        }
      }
      sharedPrefsEditor.apply();
    });
  }

  public <T extends Thing & Votable> VoteDirection getPendingOrDefaultVote(T thing, VoteDirection defaultValue) {
    return VoteDirection.valueOf(sharedPrefs.getString(keyFor(thing), defaultValue.name()));
  }

  public boolean isVotePending(String thingFullName) {
    VotableThingFullNameWrapper thing = VotableThingFullNameWrapper.create(thingFullName);
    return isVotePending(thing);
  }

  public <T extends Thing & Votable> boolean isVotePending(T thing) {
    return sharedPrefs.contains(keyFor(thing));
  }

  private <T extends Thing & Votable> void markVoteAsPending(T thingToVote, VoteDirection voteDirection) {
    sharedPrefs.edit().putString(keyFor(thingToVote), voteDirection.name()).apply();
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
  public <T extends Thing & Votable> int getScoreAfterAdjustingPendingVote(T thing) {
    int submissionScore = thing.getScore();

    if (isVotePending(thing)) {
      VoteDirection pendingOrDefaultVoteDirection = getPendingOrDefaultVote(thing, thing.getVote());
      switch (pendingOrDefaultVoteDirection) {
        case UPVOTE:
          submissionScore += 1;
          break;

        case DOWNVOTE:
          submissionScore -= 1;
          break;

        default:
        case NO_VOTE:
          switch (thing.getVote()) {
            case UPVOTE:
              submissionScore -= 1;
              break;

            case DOWNVOTE:
              submissionScore += 1;
              break;

            default:
            case NO_VOTE:
              // No-vote on a submission with already a no-vote direction happens when the user
              // makes multiple votes on it and eventually settles on the same no-vote direction.
              break;
          }
          break;
      }
    }

    return submissionScore;
  }
}
