package me.saket.dank.ui.submission;

import net.dean.jraw.models.Contribution;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.walkthrough.SyntheticData;

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
      return new AutoValue_NoOpReply(parentContribution, parentThread, body, createdTimeMillis);
    }
    return new AutoValue_RealReply(parentContribution, parentThread, body, createdTimeMillis);
  }
}
