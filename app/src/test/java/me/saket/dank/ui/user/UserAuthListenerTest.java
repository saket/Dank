package me.saket.dank.ui.user;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import android.support.annotation.NonNull;

import com.f2prateek.rx.preferences2.Preference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Predicate;
import me.saket.dank.ImmediateSchedulersRule;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.TimeInterval;

public class UserAuthListenerTest {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public ImmediateSchedulersRule schedulersRule = ImmediateSchedulersRule.create();

  @Mock SubredditSubscriptionManager subscriptionManager;
  @Mock UserSessionRepository userSessionRepository;
  @Mock Preference<TimeInterval> unreadMessagesPollInterval;

  private UserAuthListener userAuthListener;

  @Before
  public void setUp() throws Exception {
    userAuthListener = spy(new UserAuthListener(subscriptionManager, userSessionRepository, unreadMessagesPollInterval));
  }

  @Test
  public void startup() throws Exception {
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.of(UserSession.create("saketme"))));

    //noinspection ConstantConditions
    userAuthListener.doSomething(null)
        .test()
        .assertError(predicateForJobSchedulerErrors());

    verify(userAuthListener).handleActiveSessionOnAppStartup(any());
  }

  @Test
  public void login() {
    Optional<UserSession> user = Optional.of(UserSession.create("saketme"));
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.empty(), user));
    when(subscriptionManager.removeAll()).thenReturn(Completable.complete());
    when(subscriptionManager.refreshAndSaveSubscriptions()).thenReturn(Completable.complete());

    //noinspection ConstantConditions
    userAuthListener.doSomething(null)
        .test()
        .assertError(predicateForJobSchedulerErrors());

    verify(userAuthListener).handleLoggedIn(any());
    verify(subscriptionManager).removeAll();
    verify(subscriptionManager).refreshAndSaveSubscriptions();
  }

  @Test
  public void logout() {
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.empty(), Optional.empty()));
    when(subscriptionManager.removeAll()).thenReturn(Completable.complete());

    //noinspection ConstantConditions
    userAuthListener.doSomething(null)
        .test()
        .assertComplete()
        .assertNoErrors();

    verify(userAuthListener).handleLoggedOut();
    verify(subscriptionManager).removeAll();
    verify(subscriptionManager).resetDefaultSubreddit();
  }

  @NonNull
  private Predicate<Throwable> predicateForJobSchedulerErrors() {
    return e -> e.getMessage() != null && e.getMessage().contains("Method setRequiredNetworkType");
  }
}
