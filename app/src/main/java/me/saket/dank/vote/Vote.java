package me.saket.dank.vote;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import io.reactivex.Completable;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.walkthrough.SyntheticData;
import timber.log.Timber;

public interface Vote {

  Identifiable contributionToVote();

  VoteDirection direction();

  Completable saveAndSend(VotingManager votingManager);

  static Vote create(Identifiable contributionToVote, VoteDirection direction) {
    boolean isNoOpVote;

    if (contributionToVote instanceof Comment) {
      String submissionFullNme = ((Comment) contributionToVote).getSubmissionFullName();
      isNoOpVote = submissionFullNme.equalsIgnoreCase(SyntheticData.Companion.getSUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH());

    } else if (contributionToVote instanceof Submission) {
      String submissionId = contributionToVote.getId();
      isNoOpVote = submissionId.equalsIgnoreCase(SyntheticData.SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH);

    } else {
      isNoOpVote = true;
    }

    if (isNoOpVote) {
      return new AutoValue_Vote_NoOpVote(contributionToVote, direction);
    }
    return new AutoValue_Vote_RealVote(contributionToVote, direction);
  }

  Completable sendToRemote(Reddit reddit);

  @AutoValue
  abstract class RealVote implements Vote {

    @Override
    public Completable saveAndSend(VotingManager votingManager) {
      return votingManager.voteWithAutoRetry(this);
    }

    @Override
    public Completable sendToRemote(Reddit reddit) {
      return reddit.loggedInUser().vote(contributionToVote(), direction());
    }
  }

  @AutoValue
  abstract class NoOpVote implements Vote {

    @Override
    public Completable saveAndSend(VotingManager votingManager) {
      return votingManager.voteWithAutoRetry(this);
    }

    @Override
    public Completable sendToRemote(Reddit reddit) {
      Timber.i("Ignoring voting in synthetic-submission-for-gesture-walkthrough");
      return Completable.complete();
    }
  }
}
