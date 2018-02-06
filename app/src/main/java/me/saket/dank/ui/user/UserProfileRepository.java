package me.saket.dank.ui.user;

import android.support.annotation.CheckResult;

import com.nytimes.android.external.store3.base.impl.MemoryPolicy;
import com.nytimes.android.external.store3.base.impl.Store;
import com.nytimes.android.external.store3.base.impl.StoreBuilder;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.links.RedditUserLink;

@Singleton
public class UserProfileRepository {

  private final Store<UserProfile, RedditUserLink> userProfileStore;

  @Inject
  public UserProfileRepository(DankRedditClient dankRedditClient) {
    userProfileStore = StoreBuilder.<RedditUserLink, UserProfile>key()
        .fetcher(userLink -> dankRedditClient.userProfile(userLink.name()))
        .memoryPolicy(MemoryPolicy.builder()
            // Warning: Memory-only store doesn't support expire-after-access policy.
            .setExpireAfterWrite(6)
            .setExpireAfterTimeUnit(TimeUnit.HOURS)
            .build())
        .open();
  }

  @CheckResult
  public Single<UserProfile> profile(RedditUserLink userLink) {
    return userProfileStore.get(userLink);
  }
}
