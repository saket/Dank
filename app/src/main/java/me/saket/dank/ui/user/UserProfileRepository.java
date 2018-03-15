package me.saket.dank.ui.user;

import android.support.annotation.CheckResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;
import com.squareup.moshi.JsonAdapter;

import net.dean.jraw.models.Account;
import net.dean.jraw.models.LoggedInAccount;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.di.Dank;

@Singleton
public class UserProfileRepository {

  private final Store<UserProfile, String> userProfileStore;
  private final DankRedditClient dankRedditClient;

  @Inject
  public UserProfileRepository(DankRedditClient client) {
    dankRedditClient = client;

    userProfileStore = StoreBuilder.<String, UserProfile>key()
        .fetcher(username -> userProfile(username))
        .memoryPolicy(MemoryPolicy.builder()
            // FIXME: Memory-only store doesn't support expire-after-access policy.
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
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
  public Single<LoggedInAccount> loggedInUserAccount() {
    return dankRedditClient.loggedInUserAccount();
  }
}
