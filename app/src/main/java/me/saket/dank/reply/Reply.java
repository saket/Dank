package me.saket.dank.reply;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.walkthrough.SyntheticData;
import timber.log.Timber;

public interface Reply {

  Contribution parentContribution();

  ParentThread parentThread();

  String body();

  long createdTimeMillis();

  /**
   * @return Fullname of the posted reply.
   */
  Single<String> sendToRemote(DankRedditClient dankRedditClient);

  Completable saveAndSend(ReplyRepository replyRepository);

  default PendingSyncReply toPendingSync(UserSessionRepository userSessionRepository, long sentTimeMillis) {
    return PendingSyncReply.create(
        body(),
        PendingSyncReply.State.POSTING,
        parentThread().fullName(),
        parentContribution().getFullName(),
        userSessionRepository.loggedInUserName(),
        createdTimeMillis(),
        sentTimeMillis
    );
  }

  static Reply create(Contribution parentContribution, ParentThread parentThread, String body, long createdTimeMillis) {
    if (parentThread.fullName().equalsIgnoreCase(SyntheticData.SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH)) {
      return new AutoValue_Reply_NoOpReply(parentContribution, parentThread, body, createdTimeMillis);
    }
    return new AutoValue_Reply_RealReply(parentContribution, parentThread, body, createdTimeMillis);
  }

  @AutoValue
  abstract class RealReply implements Reply {
    @Override
    public Completable saveAndSend(ReplyRepository replyRepository) {
      return replyRepository.sendReply(this);
    }

    public Single<String> sendToRemote(DankRedditClient dankRedditClient) {
      return dankRedditClient.withAuth(Single.fromCallable(() -> {
        String postedReplyId = dankRedditClient.userAccountManager().reply(parentContribution(), body());
        String postedFullName = parentThread().type().fullNamePrefix() + postedReplyId;
        Timber.i("Posted full-name: %s", postedFullName);
        return postedFullName;
      }));
    }
  }

  @AutoValue
  abstract class NoOpReply implements Reply {
    @Override
    public Completable saveAndSend(ReplyRepository replyRepository) {
      Timber.i("Ignoring reply sent to synthetic-submission-for-gesture-walkthrough");
      return Completable.complete();
    }

    @Override
    public Single<String> sendToRemote(DankRedditClient dankRedditClient) {
      return Single.error(new AssertionError("Shouldn't even reach here"));
    }
  }
}
