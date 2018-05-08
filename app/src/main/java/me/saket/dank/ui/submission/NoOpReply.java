package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;

@AutoValue
public abstract class NoOpReply implements Reply {

  @Override
  public Single<String> send(DankRedditClient dankRedditClient) {
    return Single.just("test_reply_for_" + parentContribution().getFullName() + "_created_at_" + createdTimeMillis());
  }
}
