package me.saket.dank.ui.user;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.utils.StoreFilePersister;
import okio.BufferedSource;

@Singleton
public class UserProfileRepository {

  private final Store<UserProfile, RedditUserLink> userProfileStore;

  @Inject
  public UserProfileRepository(DankRedditClient dankRedditClient, FileSystem cacheFileSystem, Moshi moshi) {
    PathResolver<RedditUserLink> pathResolver = key -> "reddit_user_" + key.name().toLowerCase(Locale.ENGLISH);
    StoreFilePersister.JsonParser<RedditUserLink, UserProfile> jsonParser = new UserProfileStoreJsonParser(moshi);

    userProfileStore = StoreBuilder.<RedditUserLink, UserProfile>key()
        .fetcher(userLink -> dankRedditClient.userProfile(userLink.name()))
        .persister(new StoreFilePersister<>(cacheFileSystem, pathResolver, jsonParser))
        .open();
  }

  @CheckResult
  public Single<UserProfile> profile(RedditUserLink userLink) {
    return userProfileStore.get(userLink);
  }

  private static class UserProfileStoreJsonParser implements StoreFilePersister.JsonParser<RedditUserLink, UserProfile> {
    private final JsonAdapter<UserProfile> jsonAdapter;

    public UserProfileStoreJsonParser(Moshi moshi) {
      this.jsonAdapter = moshi.adapter(UserProfile.class);
    }

    @Override
    public UserProfile fromJson(BufferedSource jsonBufferedSource) throws IOException {
      return jsonAdapter.fromJson(jsonBufferedSource);
    }

    @Override
    public String toJson(UserProfile value) {
      return jsonAdapter.toJson(value);
    }
  }
}
