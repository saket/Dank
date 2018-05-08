package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import timber.log.Timber;

@AutoValue
public abstract class RealReply implements Reply {

  @Override
  public abstract Contribution parentContribution();

  @Override
  public abstract ParentThread parentThread();

  @Override
  public abstract String body();

  @Override
  public abstract long createdTimeMillis();

  public Single<String> send(DankRedditClient dankRedditClient) {
    return dankRedditClient.withAuth(Single.fromCallable(() -> {
      String postedReplyId = dankRedditClient.userAccountManager().reply(parentContribution(), body());
      String postedFullName = parentThread().type().fullNamePrefix() + postedReplyId;
      Timber.i("Posted full-name: %s", postedFullName);
      return postedFullName;
    }));
  }
}
