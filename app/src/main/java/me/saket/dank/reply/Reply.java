package me.saket.dank.reply;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

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
  Single<String> send(DankRedditClient dankRedditClient);

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
    if (parentThread.fullName().equalsIgnoreCase(SyntheticData.ID_SUBMISSION_FOR_GESTURE_WALKTHROUGH)) {
      return new AutoValue_Reply_NoOpReply(parentContribution, parentThread, body, createdTimeMillis);
    }
    return new AutoValue_Reply_RealReply(parentContribution, parentThread, body, createdTimeMillis);
  }

  @AutoValue
  abstract class RealReply implements Reply {

    public Single<String> send(DankRedditClient dankRedditClient) {
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
    public Single<String> send(DankRedditClient dankRedditClient) {
      return Single.just("test_reply_for_" + parentContribution().getFullName() + "_created_at_" + createdTimeMillis());
    }
  }
}
