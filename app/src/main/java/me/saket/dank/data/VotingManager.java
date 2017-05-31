package me.saket.dank.data;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.annotation.CheckResult;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thing;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

import io.reactivex.Completable;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.submission.VoteJobService;
import timber.log.Timber;

/**
 * Handles voting on {@link Submission Submissions} & {@link Comment Comments} and storing their values
 * locally, until they're refreshed from remote again.
 *
 * TODO: Clear individual pending votes when they're received from remote.
 * TODO: Do we need to worry about recycling old pending votes because their jobs timed out and expired?
 */
public class VotingManager {

  private static final String KEY_PENDING_VOTE_ = "pendingVote_";

  private Application appContext;
  private DankRedditClient dankRedditClient;
  private SharedPreferences sharedPreferences;

  /**
   * @param appContext Used for scheduling {@link VoteJobService}.
   */
  public VotingManager(Application appContext, DankRedditClient dankRedditClient, SharedPreferences sharedPreferences) {
    this.appContext = appContext;
    this.dankRedditClient = dankRedditClient;
    this.sharedPreferences = sharedPreferences;
  }

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

  public <T extends Thing & Votable> VoteDirection getPendingVote(T thing, VoteDirection defaultValue) {
    return VoteDirection.valueOf(sharedPreferences.getString(keyFor(thing), defaultValue.name()));
  }

  public boolean isVotePending(String thingFullName) {
    VotableThingFullNameWrapper thing = VotableThingFullNameWrapper.create(thingFullName);
    return sharedPreferences.contains(keyFor(thing));
  }

  private <T extends Thing & Votable> void markVoteAsPending(T thingToVote, VoteDirection voteDirection) {
    sharedPreferences.edit().putString(keyFor(thingToVote), voteDirection.name()).apply();
  }

  private <T extends Thing & Votable> void removePendingVote(T thingToVote) {
    sharedPreferences.edit().remove(keyFor(thingToVote)).apply();
  }

  private <T extends Thing & Votable> String keyFor(T thing) {return KEY_PENDING_VOTE_ + thing.getFullName();}
}
