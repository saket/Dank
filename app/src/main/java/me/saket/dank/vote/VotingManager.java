package me.saket.dank.vote;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Looper;

import androidx.annotation.CheckResult;

import com.squareup.moshi.Moshi;

import net.dean.jraw.ApiException;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Votable;
import net.dean.jraw.models.VoteDirection;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.BuildConfig;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import timber.log.Timber;

/**
 * Handles voting on {@link Submission Submissions} & {@link Comment Comments} and storing their values
 * locally, until they're refreshed from remote again.
 * <p>
 * TODO: Clear individual pending votes when they're received from remote.
 * TODO: Do we need to worry about recycling old pending votes because their jobs timed out and expired?
 */
public class VotingManager {

  private static final int HTTP_CODE_CONTRIBUTION_DELETED = 404;
  public static final int HTTP_CODE_TOO_MANY_REQUESTS = 429;
  private static final String KEY_PENDING_VOTE_ = "pendingVote_";

  private final Application appContext;
  private final Lazy<Reddit> reddit;
  private final Lazy<SharedPreferences> sharedPrefs;
  private final Lazy<Moshi> moshi;

  /**
   * @param appContext Used for scheduling {@link VoteJobService}.
   */
  @Inject
  public VotingManager(
      Application appContext,
      Lazy<Reddit> reddit,
      @Named("votes") Lazy<SharedPreferences> sharedPrefs,
      Lazy<Moshi> moshi)
  {
    this.appContext = appContext;
    this.reddit = reddit;
    this.sharedPrefs = sharedPrefs;
    this.moshi = moshi;
  }

  @CheckResult
  public Observable<Object> streamChanges() {
    return Observable.create(emitter -> {
      SharedPreferences.OnSharedPreferenceChangeListener changeListener = (sharedPreferences, key) -> emitter.onNext(key);
      sharedPrefs.get().registerOnSharedPreferenceChangeListener(changeListener);
      emitter.setCancellable(() -> sharedPrefs.get().unregisterOnSharedPreferenceChangeListener(changeListener));
      changeListener.onSharedPreferenceChanged(sharedPrefs.get(), "");    // Initial value.
    });
  }

  @CheckResult
  public Completable saveAndSend(Vote vote) {
    // Mark the vote as pending immediately so that getPendingVote() can be used immediately after calling vote().
    markVoteAsPending(vote.contributionToVote(), vote.direction());

    return vote.sendToRemote(reddit.get())
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
  public Completable voteWithAutoRetry(Vote vote) {
    return saveAndSend(vote)
        .onErrorComplete(error -> {
          boolean shouldComplete;
          // TODO: Reddit replies with 400 bad request for archived submissions.

          if (isTooManyRequestsError(error) || !Dank.errors().resolve(error).isUnknown()) {
            // If unknown, this will most probably be network/Reddit errors. Swallow the error and attempt retries later.
            Timber.i("Voting failed for %s. Will retry again later. Error: %s", vote.contributionToVote().getFullName(), error.getMessage());
            VoteJobService.scheduleRetry(appContext, vote.contributionToVote(), vote.direction(), moshi.get());
            shouldComplete = true;

          } else {
            shouldComplete = error instanceof NetworkException
                && ((NetworkException) error).getRes().getCode() == HTTP_CODE_CONTRIBUTION_DELETED;
          }

          return shouldComplete;
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

      SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.get().edit();
      for (Submission submission : submissionsFromRemote) {
        if (isVotePending(submission)) {
          //Timber.i("Removing stale pending vote for %s", ((Submission) submission).getTitle());
          sharedPrefsEditor.remove(keyFor(submission));
        }
      }
      sharedPrefsEditor.apply();
    });
  }

  public <T extends Votable & Identifiable> VoteDirection getPendingOrDefaultVote(T votableContribution, VoteDirection defaultValue) {
    String voteEnumString = sharedPrefs.get().getString(keyFor(votableContribution), defaultValue.name());
    return VoteDirectionX.valueOfWithMigration(voteEnumString);
  }

//  public boolean isVotePending(Identifiable votableContribution) {
//    return sharedPrefs.get().contains(keyFor(votableContribution));
//  }

  public boolean isVotePending(Identifiable identifiable) {
    return sharedPrefs.get().contains(keyFor(identifiable));
  }

  private void markVoteAsPending(Identifiable votableContribution, VoteDirection voteDirection) {
    sharedPrefs.get().edit().putString(keyFor(votableContribution), voteDirection.name()).apply();
  }

  @CheckResult
  public Completable removeAll() {
    if (!BuildConfig.DEBUG) {
      throw new IllegalStateException();
    }

    return Completable.fromAction(() ->
        // VotingManager uses a dedicated shared prefs file so we can safely clear everything.
        sharedPrefs.get().edit().clear().apply()
    );
  }

  /**
   * Get <var>thing</var>'s score assuming that any pending vote has been synced with remote.
   */
  public <T extends Votable & Identifiable> int getScoreAfterAdjustingPendingVote(T votableContribution) {
    if (!isVotePending(votableContribution)) {
      return votableContribution.getScore();
    }

    VoteDirection actualVoteDirection = votableContribution.getVote();
    VoteDirection pendingVoteDirection = getPendingOrDefaultVote(votableContribution, actualVoteDirection);

    if (actualVoteDirection == pendingVoteDirection) {
      return votableContribution.getScore();
    }

    int resultingScore = votableContribution.getScore();
    switch (pendingVoteDirection) {
      case UP:
        resultingScore += 1;
        break;

      case DOWN:
        resultingScore -= 1;
        break;

      default:
      case NONE:
        switch (votableContribution.getVote()) {
          case UP:
            resultingScore -= 1;
            break;

          case DOWN:
            resultingScore += 1;
            break;

          default:
          case NONE:
            throw new AssertionError();
        }
        break;
    }

    return resultingScore;
  }

  private String keyFor(String contributionFullName) {
    return KEY_PENDING_VOTE_ + contributionFullName;
  }

  private String keyFor(Identifiable contribution) {
    return keyFor(contribution.getFullName());
  }

  public static boolean isTooManyRequestsError(Throwable error) {
    return isHttpCode(error, HTTP_CODE_TOO_MANY_REQUESTS);
  }

  private static boolean isHttpCode(Throwable error, int httpCode) {
    //noinspection SimplifiableIfStatement
    if (error instanceof NetworkException && ((NetworkException) error).getRes().getCode() == httpCode) {
      return true;
    }
    return error instanceof ApiException && ((ApiException) error).getCode().equalsIgnoreCase(String.valueOf(httpCode));
  }
}
