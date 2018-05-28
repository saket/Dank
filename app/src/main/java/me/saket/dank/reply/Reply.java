package me.saket.dank.reply;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.walkthrough.SyntheticData;
import timber.log.Timber;

public interface Reply {

  Identifiable parent();

  ParentThread parentThread();

  String body();

  long createdTimeMillis();

  Completable saveAndSend(ReplyRepository replyRepository);

  /**
   * @return Full-name of the posted reply.
   */
  Single<Identifiable> sendToRemote(Reddit reddit);

  default PendingSyncReply toPendingSync(UserSessionRepository userSessionRepository, long sentTimeMillis) {
    return PendingSyncReply.create(
        body(),
        PendingSyncReply.State.POSTING,
        parentThread().fullName(),
        parent().getFullName(),
        userSessionRepository.loggedInUserName(),
        createdTimeMillis(),
        sentTimeMillis
    );
  }

  static Reply create(Identifiable parent, ParentThread parentThread, String body, long createdTimeMillis) {
    if (parentThread.fullName().equalsIgnoreCase(SyntheticData.Companion.getSUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH())) {
      return new AutoValue_Reply_NoOpReply(parent, parentThread, body, createdTimeMillis);
    }
    return new AutoValue_Reply_RealReply(parent, parentThread, body, createdTimeMillis);
  }

  @AutoValue
  abstract class RealReply implements Reply {

    @Override
    public Completable saveAndSend(ReplyRepository replyRepository) {
      return replyRepository.sendReply(this);
    }

    public Single<Identifiable> sendToRemote(Reddit reddit) {
      return reddit.loggedInUser()
          .reply(parent(), body())
          .map(postedReply -> {
            Timber.i("Posted full-name: %s", postedReply.getFullName());
            return postedReply;
          });
    }
  }

  @AutoValue
  abstract class NoOpReply implements Reply {

    @Override
    public Completable saveAndSend(ReplyRepository replyRepository) {
      Timber.i("Ignoring sending reply to synthetic-submission-for-gesture-walkthrough");
      return Completable.complete();
    }

    @Override
    public Single<Identifiable> sendToRemote(Reddit reddit) {
      return Single.error(new AssertionError("Shouldn't even reach here"));
    }
  }
}
