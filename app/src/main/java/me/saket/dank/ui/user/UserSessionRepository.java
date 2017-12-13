package me.saket.dank.ui.user;

import android.content.SharedPreferences;
import android.support.annotation.CheckResult;

import com.f2prateek.rx.preferences2.RxSharedPreferences;

import java.util.Objects;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.utils.Optional;

public class UserSessionRepository {

  private static final String KEY_LOGGED_IN_USERNAME = "loggedInUsername";
  private final RxSharedPreferences rxSharedPreferences;
  private SharedPreferences sharedPrefs;

  @Inject
  public UserSessionRepository(SharedPreferences sharedPrefs) {
    this.sharedPrefs = sharedPrefs;
    this.rxSharedPreferences = RxSharedPreferences.create(sharedPrefs);
  }

  public void setLoggedInUsername(String username) {
    sharedPrefs.edit().putString(KEY_LOGGED_IN_USERNAME, Objects.requireNonNull(username, "username == null")).apply();
  }

  public void removeLoggedInUsername() {
    sharedPrefs.edit().remove(KEY_LOGGED_IN_USERNAME).apply();
  }

  public boolean isUserLoggedIn() {
    return loggedInUserName() != null;
  }

  public String loggedInUserName() {
    return sharedPrefs.getString(KEY_LOGGED_IN_USERNAME, null);
  }

  /** Note: emits the current value immediately. */
  @CheckResult
  public Observable<Optional<UserSession>> streamUserSession() {
    return rxSharedPreferences.getString(KEY_LOGGED_IN_USERNAME, "")
        .asObservable()
        .map(username -> username.equals("")
            ? Optional.empty()
            : Optional.of(UserSession.create(username))
        );
  }

  @CheckResult
  public Observable<UserSession> streamFutureLogInEvents() {
    return streamUserSession()
        .skip(1) // Not interested in the current value.
        .filter(optionalSession -> optionalSession.isPresent())
        .map(optional -> optional.get());
  }
}
