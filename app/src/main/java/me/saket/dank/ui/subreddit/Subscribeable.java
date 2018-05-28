package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Subreddit;

import io.reactivex.Completable;
import me.saket.dank.reddit.Reddit;

public interface Subscribeable {

  String displayName();

  Completable subscribe(Reddit.Subscriptions subscriptions);

  Completable unsubscribe(Reddit.Subscriptions subscriptions);

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
      return actual().getName();
    }

    @Override
    public Completable subscribe(Reddit.Subscriptions subscriptions) {
      return subscriptions.add(actual());
    }

    @Override
    public Completable unsubscribe(Reddit.Subscriptions subscriptions) {
      return subscriptions.remove(actual());
    }

    public static RemoteSubscribeable create(Subreddit subreddit) {
      return new AutoValue_Subscribeable_RemoteSubscribeable(subreddit);
    }
  }

  @AutoValue
  abstract class LocalSubscribeable implements Subscribeable {
    @Override
    public abstract String displayName();

    public Completable subscribe(Reddit.Subscriptions subscriptions) {
      return Completable.complete();
    }

    @Override
    public Completable unsubscribe(Reddit.Subscriptions subscriptions) {
      return Completable.complete();
    }

    public static LocalSubscribeable create(String subredditName) {
      return new AutoValue_Subscribeable_LocalSubscribeable(subredditName);
    }
  }
}
