package me.saket.dank.ui.user;

import android.support.annotation.CheckResult;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Preconditions;

/**
 * TODO: Merge with {@link UserProfileRepository}.
 */
public class UserSessionRepository {

  private static final String KEY_LOGGED_IN_USERNAME = "logged_in_username";
  private static final String EMPTY = "";

  private final Preference<String> loggedInUsername;

  @Inject
  public UserSessionRepository(@Named("user_session") RxSharedPreferences rxSharedPreferences) {
    loggedInUsername = rxSharedPreferences.getString(KEY_LOGGED_IN_USERNAME, EMPTY);
  }

  public void setLoggedInUsername(String username) {
    Preconditions.checkNotNull(username, "username == null");
    loggedInUsername.set(username);
  }

  public void removeLoggedInUsername() {
    loggedInUsername.set(EMPTY);
  }

  public boolean isUserLoggedIn() {
    return loggedInUserName() != null && !loggedInUserName().equals(EMPTY);
  }

  public String loggedInUserName() {
    return loggedInUsername.get();
  }

  /** Note: emits the current value immediately. */
  @CheckResult
  public Observable<Optional<UserSession>> streamSessions() {
    return loggedInUsername.asObservable()
        .map(username -> username.equals(EMPTY)
            ? Optional.empty()
            : Optional.of(UserSession.create(username))
        );
  }
}
