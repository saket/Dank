package me.saket.dank.ui.subreddit;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.ui.UiChange;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.subreddit.events.SubredditScreenCreateEvent;
import me.saket.dank.ui.user.UserProfileRepository;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.ui.user.messages.InboxFolder;

public class SubredditController implements ObservableTransformer<UiEvent, UiChange<SubredditActivity>> {

  private final Lazy<InboxRepository> inboxRepository;
  private final Lazy<UserProfileRepository> userProfileRepository;
  private final Lazy<UserSessionRepository> userSessionRepository;

  @Inject
  public SubredditController(
      Lazy<InboxRepository> inboxRepository,
      Lazy<UserProfileRepository> userProfileRepository,
      Lazy<UserSessionRepository> userSessionRepository)
  {
    this.inboxRepository = inboxRepository;
    this.userProfileRepository = userProfileRepository;
    this.userSessionRepository = userSessionRepository;
  }

  @Override
  public ObservableSource<UiChange<SubredditActivity>> apply(Observable<UiEvent> upstream) {
    Observable<UiEvent> replayedEvents = upstream.replay().refCount();
    //noinspection unchecked
    return Observable.mergeArray(unreadMessageIconChanges(replayedEvents));
  }

  private Observable<UiChange<SubredditActivity>> unreadMessageIconChanges(Observable<UiEvent> events) {
    return events
        .ofType(SubredditScreenCreateEvent.class)
        .switchMap(o -> userSessionRepository.get().streamSessions())
        .filter(session -> session.isPresent())
        .switchMap(session -> {
          Observable<Integer> unreadCountsFromAccount = userProfileRepository.get()
              .loggedInUserAccounts()
              .map(account -> account.getInboxCount());

          Observable<Integer> unreadCountsFromInbox = inboxRepository.get()
              .messages(InboxFolder.UNREAD)
              .map(unreads -> unreads.size());

          return unreadCountsFromInbox.mergeWith(unreadCountsFromAccount)
              .subscribeOn(io())
              .observeOn(mainThread())
              .map(unreadCount -> unreadCount > 0)
              .map(hasUnreads -> hasUnreads
                  ? SubredditUserProfileIconType.USER_PROFILE_WITH_UNREAD_MESSAGES
                  : SubredditUserProfileIconType.USER_PROFILE)
              .map(iconType -> (UiChange<SubredditActivity>) ui -> ui.setToolbarUserProfileIcon(iconType));
        });
  }
}
