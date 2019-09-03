package me.saket.dank.ui.user;

import androidx.annotation.NonNull;

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
import me.saket.dank.analytics.CrashReporter;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.TimeInterval;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

public class UserAuthListenerTest {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public ImmediateSchedulersRule schedulersRule = ImmediateSchedulersRule.create();

  @Mock SubscriptionRepository subscriptionRepository;
  @Mock UserSessionRepository userSessionRepository;
  @Mock CrashReporter crashReporter;
  @Mock Preference<Boolean> unreadMessagesPollEnabled;
  @Mock Preference<TimeInterval> unreadMessagesPollInterval;
  @Mock Preference<NetworkStrategy> unreadMessagesPollNetworkStrategy;

  private UserAuthListener userAuthListener;

  @Before
  public void setUp() {
    userAuthListener = spy(new UserAuthListener(
        () -> subscriptionRepository,
        () -> userSessionRepository,
        () -> crashReporter,
        () -> unreadMessagesPollEnabled,
        () -> unreadMessagesPollInterval,
        () -> unreadMessagesPollNetworkStrategy));
  }

  @Test
  public void startup() {
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.of(UserSession.create("saketme"))));

    //noinspection ConstantConditions
    userAuthListener.startListening(null)
        .test()
        .assertError(predicateForJobSchedulerErrors());

    verify(userAuthListener).handleActiveSessionOnAppStartup(any());
  }

  @Test
  public void login() {
    Optional<UserSession> user = Optional.of(UserSession.create("saketme"));
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.empty(), user));
    when(subscriptionRepository.removeAll()).thenReturn(Completable.complete());
    when(subscriptionRepository.refreshAndSaveSubscriptions()).thenReturn(Completable.complete());

    //noinspection ConstantConditions
    userAuthListener.startListening(null)
        .test()
        .assertError(predicateForJobSchedulerErrors());

    verify(userAuthListener).handleLoggedIn(any(), any());
    verify(subscriptionRepository).removeAll();
    verify(subscriptionRepository).refreshAndSaveSubscriptions();
  }

  @Test
  public void on_logout_should_remove_all_user_subscriptions() {
    when(userSessionRepository.streamSessions()).thenReturn(Observable.just(Optional.empty(), Optional.empty()));
    when(subscriptionRepository.removeAll()).thenReturn(Completable.complete());

    //noinspection ConstantConditions
    userAuthListener.startListening(null)
        .test()
        .assertComplete()
        .assertNoErrors();

    verify(userAuthListener).handleLoggedOut();
    verify(subscriptionRepository).removeAll();
  }

  @NonNull
  private Predicate<Throwable> predicateForJobSchedulerErrors() {
    return e -> e.getMessage() != null && e.getMessage().contains("Method setRequiredNetworkType");
  }
}
