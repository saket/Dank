package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Subreddit;

import io.reactivex.Completable;
import me.saket.dank.data.DankRedditClient;

public interface Subscribeable {

  String displayName();

  Completable subscribe(DankRedditClient redditClient);

  Completable unsubscribe(DankRedditClient redditClient);

  static Subscribeable create(Subreddit subreddit) {
    return RemoteSubscribeable.create(subreddit);
  }

  static Subscribeable local(String subredditName) {
    return LocalSubscribeable.create(subredditName);
  }

  @AutoValue
  abstract class RemoteSubscribeable implements Subscribeable {
    public abstract Subreddit actual();

    @Override
    public String displayName() {
      return actual().getDisplayName();
    }

    @Override
    public Completable subscribe(DankRedditClient redditClient) {
      return redditClient.subscribeTo(actual());
    }

    @Override
    public Completable unsubscribe(DankRedditClient redditClient) {
      return redditClient.unsubscribeFrom(actual());
    }

    public static RemoteSubscribeable create(Subreddit subreddit) {
      return new AutoValue_Subscribeable_RemoteSubscribeable(subreddit);
    }
  }

  @AutoValue
  abstract class LocalSubscribeable implements Subscribeable {
    @Override
    public abstract String displayName();

    public Completable subscribe(DankRedditClient redditClient) {
      return Completable.complete();
    }

    @Override
    public Completable unsubscribe(DankRedditClient redditClient) {
      return Completable.complete();
    }

    public static LocalSubscribeable create(String subredditName) {
      return new AutoValue_Subscribeable_LocalSubscribeable(subredditName);
    }
  }
}
