package me.saket.dank.utils;

import android.content.Context;
import android.content.SharedPreferences;

import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.auth.TokenStore;

/**
 * Simple implementation of TokenStore that uses SharedPreferences
 */
public class AndroidTokenStore implements TokenStore {

  private static final String TOKEN_ACQUIRE_TIME = "token_acquire_time_";

  private final Context context;

  public AndroidTokenStore(Context context) {
    this.context = context;
  }

  @Override
  public boolean isStored(String key) {
    return getSharedPreferences().contains(key);
  }

  @Override
  public boolean isAcquiredTimeStored(String key) {
    return getSharedPreferences().contains(TOKEN_ACQUIRE_TIME + key);
  }

  @Override
  public String readToken(String key) throws NoSuchTokenException {
    String token = getSharedPreferences().getString(key, null);
    if (token == null) {
      throw new NoSuchTokenException("Token for key '" + key + "' does not exist");
    }
    return token;
  }

  @Override
  public void writeToken(String key, String token) {
    getSharedPreferences().edit().putString(key, token).apply();
  }

  @Override
  public void removeToken(String key) {
    getSharedPreferences().edit().remove(key).apply();
  }

  @Override
  public long readAcquireTimeMillis(String key) {
    return getSharedPreferences().getLong(TOKEN_ACQUIRE_TIME + key, 0);
  }

  @Override
  public void writeAcquireTimeMillis(String key, long acquireTimeMs) {
    getSharedPreferences().edit().putLong(TOKEN_ACQUIRE_TIME + key, acquireTimeMs).apply();
  }

  @Override
  public void removeAcquireTimeMillis(String key) {
    getSharedPreferences().edit().remove(TOKEN_ACQUIRE_TIME + key).apply();
  }

  private SharedPreferences getSharedPreferences() {
    return context.getSharedPreferences(context.getPackageName() + ".reddit_token_store", Context.MODE_PRIVATE);
  }

}
