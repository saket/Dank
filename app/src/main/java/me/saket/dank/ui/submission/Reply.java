package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.ui.user.UserSessionRepository;
import timber.log.Timber;

@AutoValue
public abstract class Reply {

  public abstract Contribution parentContribution();

  public abstract ParentThread parentThread();

  public abstract String body();

  public abstract long createdTimeMillis();

  public static Reply create(Contribution parentContribution, ParentThread parentThread, String body, long createdTimeMillis) {
    return new AutoValue_Reply(parentContribution, parentThread, body, createdTimeMillis);
  }

  public PendingSyncReply toPendingSync(UserSessionRepository userSessionRepository, long sentTimeMillis) {
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

  public Single<String> send(DankRedditClient dankRedditClient) {
    return dankRedditClient.withAuth(Single.fromCallable(() -> {
      String postedReplyId = dankRedditClient.userAccountManager().reply(parentContribution(), body());
      String postedFullName = parentThread().type().fullNamePrefix() + postedReplyId;
      Timber.i("Posted full-name: %s", postedFullName);
      return postedFullName;
    }));
  }
}
