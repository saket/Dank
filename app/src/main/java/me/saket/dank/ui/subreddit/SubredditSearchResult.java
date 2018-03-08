package me.saket.dank.ui.subreddit;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Subreddit;

public interface SubredditSearchResult {

  enum Type {
    SUCCESS,
    ERROR_PRIVATE,
    ERROR_NOT_FOUND,
    ERROR_UNKNOWN
  }

  Type type();

  static Success success(Subreddit subreddit) {
    return new AutoValue_SubredditSearchResult_Success(subreddit);
  }

  static Private privateError() {
    return new AutoValue_SubredditSearchResult_Private();
  }

  static NotFound notFound() {
    return new AutoValue_SubredditSearchResult_NotFound();
  }

  static UnknownError unknownError() {
    return new AutoValue_SubredditSearchResult_UnknownError();
  }

  @AutoValue
  abstract class Success implements SubredditSearchResult {
    abstract Subreddit subreddit();

    @Override
    public Type type() {
      return Type.SUCCESS;
    }
  }

  @AutoValue
  abstract class Private implements SubredditSearchResult {

    @Override
    public Type type() {
      return Type.ERROR_PRIVATE;
    }
  }

  @AutoValue
  abstract class NotFound implements SubredditSearchResult {

    @Override
    public Type type() {
      return Type.ERROR_NOT_FOUND;
    }
  }

  @AutoValue
  abstract class UnknownError implements SubredditSearchResult {

    @Override
    public Type type() {
      return Type.ERROR_UNKNOWN;
    }
  }
}
