package me.saket.dank.ui.media.gfycat;

import androidx.annotation.CheckResult;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.Single;

import static java.lang.System.currentTimeMillis;

public class GfycatRepositoryData {

  private static final String ACCESS_TOKEN_EMPTY_VALUE = "AUTH_TOKEN_ABSENT";

  private final Preference<Boolean> needsAccessTokenHeaderStore;
  private final Preference<Long> accessTokenExpiryTimeMillisStore;
  private final Preference<String> accessTokenStore;

  @Inject
  public GfycatRepositoryData(@Named("gfycat_repository") RxSharedPreferences kvStore) {
    needsAccessTokenHeaderStore = kvStore.getBoolean("gfycat_needs_access_token", false);
    accessTokenExpiryTimeMillisStore = kvStore.getLong("gfycat_access_token_expiry_time_millis", currentTimeMillis());
    accessTokenStore = kvStore.getString("gfycat_access_token", ACCESS_TOKEN_EMPTY_VALUE);
  }

  @CheckResult
  public boolean isAccessTokenRequired() {
    return needsAccessTokenHeaderStore.get();
  }

  public void setAccessTokenRequired(boolean needsToken) {
    needsAccessTokenHeaderStore.set(needsToken);
  }

  @CheckResult
  public Single<Long> tokenExpiryTimeMillis() {
    return Single.just(accessTokenExpiryTimeMillisStore.get());
  }

  @CheckResult
  public Completable saveOAuthResponse(GfycatOauthResponse response) {
    return Completable.fromAction(() -> {
      accessTokenExpiryTimeMillisStore.set(currentTimeMillis() + response.expiresInMillis());
      accessTokenStore.set(response.accessToken());
    });
  }

  @CheckResult
  public Single<String> accessToken() {
    return Single.just(accessTokenStore.get());
  }
}
