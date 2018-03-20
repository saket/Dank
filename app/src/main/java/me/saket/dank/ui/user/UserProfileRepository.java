package me.saket.dank.ui.user;

import android.support.annotation.CheckResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store.util.Result;
import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Account;
import net.dean.jraw.models.LoggedInAccount;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.di.Dank;
import me.saket.dank.cache.DiskLruCachePathResolver;
import me.saket.dank.cache.MoshiStoreJsonParser;
import me.saket.dank.cache.StoreFilePersister;

@Singleton
public class UserProfileRepository {

  private final DankRedditClient dankRedditClient;
  private final Store<UserProfile, String> userProfileStore;
  private final Store<LoggedInAccount, String> loggedInUserAccountStore;
  private final UserSessionRepository userSessionRepository;

  @Inject
  public UserProfileRepository(DankRedditClient client, FileSystem fileSystem, Moshi moshi, UserSessionRepository userSessionRepository) {
    this.dankRedditClient = client;
    this.userSessionRepository = userSessionRepository;

    userProfileStore = StoreBuilder.<String, UserProfile>key()
        .fetcher(username -> userProfile(username))
        .memoryPolicy(MemoryPolicy.builder()
            // FIXME: Memory-only store doesn't support expire-after-access policy.
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .open();

    DiskLruCachePathResolver<String> pathResolver = new DiskLruCachePathResolver<String>() {
      @Override
      protected String resolveIn64Letters(String username) {
        return "logged_in_user_" + username;
      }
    };
    MoshiStoreJsonParser<String, LoggedInAccount> parser = new MoshiStoreJsonParser<>(moshi, LoggedInAccount.class);

    loggedInUserAccountStore = StoreBuilder.<String, LoggedInAccount>key()
        .fetcher(o -> dankRedditClient.loggedInUserAccount())
        .memoryPolicy(MemoryPolicy.builder()
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .persister(new StoreFilePersister<>(fileSystem, pathResolver, parser))
        .open();
  }

  public Single<UserProfile> userProfile(String username) {
    return dankRedditClient.withAuth(Single.fromCallable(() -> {
      Account account = dankRedditClient.userAccount(username);

      JsonNode subredditJsonNode = account.getDataNode().get("subreddit");
      if (subredditJsonNode.isNull()) {
        // Old Reddit profile.
        return UserProfile.create(account);

      } else {
        // New profile with cover image and profile image.
        JsonAdapter<UserSubreddit> userSubredditJsonAdapter = Dank.moshi().adapter(UserSubreddit.class);
        UserSubreddit userSubreddit = userSubredditJsonAdapter.fromJson(Dank.jackson().toJson(subredditJsonNode));
        return UserProfile.create(account, userSubreddit);
      }
    }));
  }

  @CheckResult
  public Single<UserProfile> profile(RedditUserLink userLink) {
    return userProfileStore.get(userLink.name());
  }

  @CheckResult
  public Observable<LoggedInAccount> loggedInUserAccounts() {
    String loggedInUserName = userSessionRepository.loggedInUserName();
    return loggedInUserAccountStore.stream(loggedInUserName);
  }

  @CheckResult
  public Completable refreshLoggedInUserAccount() {
    String loggedInUserName = userSessionRepository.loggedInUserName();

    return loggedInUserAccountStore
        .getWithResult(loggedInUserName)
        .filter(Result::isFromCache)
        .flatMapSingle(o -> loggedInUserAccountStore.fetch(loggedInUserName))
        .toCompletable();
  }
}
