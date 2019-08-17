package me.saket.dank.ui.subscriptions;

import static me.saket.dank.utils.Arrays2.immutable;
import static me.saket.dank.utils.Arrays2.toImmutable;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;

import android.app.Application;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.CheckResult;
import androidx.annotation.VisibleForTesting;

import com.squareup.sqlbrite2.BriteDatabase;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import net.dean.jraw.models.Subreddit;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

import me.saket.dank.R;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.subreddit.SubredditSearchResult;
import me.saket.dank.ui.subreddit.SubredditSearchResult.Success;
import me.saket.dank.ui.subreddit.Subscribeable;
import me.saket.dank.ui.user.UserSessionRepository;

/**
 * Manages:
 * <p>
 * - Fetching user's subscription (cached or fresh or both)
 * - Subscribing
 * - Un-subscribing.
 * - Hiding subscriptions.
 */
@Singleton
public class SubscriptionRepository {

  private Lazy<Reddit> reddit;
  private Lazy<Application> appContext;
  private Lazy<BriteDatabase> database;
  private Lazy<UserPreferences> userPreferences;
  private Lazy<UserSessionRepository> userSessionRepository;

  @Inject
  public SubscriptionRepository(
      Lazy<Reddit> reddit,
      Lazy<Application> appContext,
      Lazy<BriteDatabase> database,
      Lazy<UserPreferences> userPreferences,
      Lazy<UserSessionRepository> userSessionRepository)
  {
    this.reddit = reddit;
    this.appContext = appContext;
    this.database = database;
    this.userPreferences = userPreferences;
    this.userSessionRepository = userSessionRepository;
  }

  public boolean isFrontpage(String subredditName) {
    return appContext.get().getString(R.string.frontpage_subreddit_name).equals(subredditName);
  }

  /**
   * Searches user's subscriptions from the database.
   *
   * @param filterTerm Can be empty, but not null.
   */
  @CheckResult
  public Observable<List<SubredditSubscription>> getAll(String filterTerm, boolean includeHidden) {
    String getQuery = includeHidden
        ? SubredditSubscription.QUERY_SEARCH_ALL_SUBSCRIBED_INCLUDING_HIDDEN
        : SubredditSubscription.QUERY_SEARCH_ALL_SUBSCRIBED_EXCLUDING_HIDDEN;

    return database.get().createQuery(SubredditSubscription.TABLE_NAME, getQuery, "%" + filterTerm + "%")
        .mapToList(SubredditSubscription.MAPPER)
        .as(immutable())
        .flatMap(filteredSubs -> {
          if (filteredSubs.isEmpty()) {
            // Fetch fresh subscriptions from remote if DB is empty.
            return database.get().createQuery(SubredditSubscription.TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
                .mapToList(SubredditSubscription.MAPPER)
                .firstOrError()
                .flatMapObservable(localSubs -> {
                  if (localSubs.isEmpty()) {
                    return refreshAndSaveSubscriptions(localSubs)
                        // Don't let this stream emit anything. A change in the database will anyway trigger that.
                        .flatMapObservable(o -> Observable.never());

                  } else {
                    return Observable.just(filteredSubs);
                  }
                });
          } else {
            return Observable.just(filteredSubs);
          }
        })
        .map(filteredSubs -> {
          // Move Frontpage and Popular to the top.
          String frontpageSubName = appContext.get().getString(R.string.frontpage_subreddit_name);
          String popularSubName = appContext.get().getString(R.string.popular_subreddit_name);

          SubredditSubscription frontpageSub = null;
          SubredditSubscription popularSub = null;

          List<SubredditSubscription> reOrderedFilteredSubs = new ArrayList<>(filteredSubs);

          for (int i = reOrderedFilteredSubs.size() - 1; i >= 0; i--) {
            SubredditSubscription subscription = reOrderedFilteredSubs.get(i);

            // Since we're going backwards, we'll find popular before frontpage.
            if (popularSub == null && subscription.name().equalsIgnoreCase(popularSubName)) {
              reOrderedFilteredSubs.remove(i);
              popularSub = subscription;
              reOrderedFilteredSubs.add(0, popularSub);

            } else if (frontpageSub == null && subscription.name().equalsIgnoreCase(frontpageSubName)) {
              reOrderedFilteredSubs.remove(i);
              frontpageSub = subscription;
              reOrderedFilteredSubs.add(0, frontpageSub);
            }
          }

          return Collections.unmodifiableList(reOrderedFilteredSubs);
        });
  }

  @CheckResult
  public Observable<List<SubredditSubscription>> getAllIncludingHidden() {
    return getAll("", true);
  }

  /**
   * Get updated subscriptions from remote and save to DB.
   */
  @CheckResult
  public Completable refreshAndSaveSubscriptions() {
    return database.get().createQuery(SubredditSubscription.TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
        .mapToList(SubredditSubscription.MAPPER)
        .firstOrError()
        .flatMap(localSubscriptions -> refreshAndSaveSubscriptions(localSubscriptions))
        .toCompletable();
  }

  @CheckResult
  private Single<List<SubredditSubscription>> refreshAndSaveSubscriptions(List<SubredditSubscription> localSubs) {
    return fetchRemoteSubscriptions(localSubs).doOnSuccess(saveSubscriptionsToDatabase());
  }

  @CheckResult
  public Completable subscribe(Subscribeable subscribeable) {
    return subscribeable.subscribe(reddit.get().subscriptions())
        .andThen(Single.just(SubredditSubscription.PendingState.NONE))
        .onErrorResumeNext(e -> {
          Timber.e(e, "Couldn't subscribe to %s. Will try again later.", subscribeable);
          return Single.just(SubredditSubscription.PendingState.PENDING_SUBSCRIBE);
        })
        .flatMapCompletable(pendingState -> Completable.fromAction(() -> {
          SubredditSubscription subscription = SubredditSubscription.create(subscribeable.displayName(), pendingState, false);
          database.get().insert(SubredditSubscription.TABLE_NAME, subscription.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
        }));
  }

  @CheckResult
  public Completable unsubscribe(SubredditSubscription subscription) {
    Completable deleteCompletable = Completable.fromAction(() ->
        database.get().delete(SubredditSubscription.TABLE_NAME, SubredditSubscription.WHERE_NAME, subscription.name()));

    if (reddit.get().subscriptions().needsRemoteSubscription(subscription.name())) {
      return deleteCompletable
          .andThen(reddit.get().subreddits().find(subscription.name()))
          .flatMapCompletable(findResult -> {
            switch (findResult.type()) {
              case SUCCESS:
                return ((Success) findResult).subscribeable().unsubscribe(reddit.get().subscriptions());

              case ERROR_PRIVATE:
                return Completable.error(new AssertionError("Couldn't unsubscribe from a private subreddit :O"));

              case ERROR_UNKNOWN:
                Throwable error = ((SubredditSearchResult.UnknownError) findResult).error();
                Timber.e(error, "Couldn't unsubscribe from %s. Will try again later.", subscription);
                SubredditSubscription updated = subscription.toBuilder().pendingState(SubredditSubscription.PendingState.PENDING_UNSUBSCRIBE).build();
                database.get().insert(SubredditSubscription.TABLE_NAME, updated.toContentValues());
                return Completable.complete();

              default:
              case ERROR_NOT_FOUND:
                // 404 == subreddit isn't present on the server anymore.
                return Completable.complete();
            }
          });

    } else {
      return deleteCompletable;
    }
  }

  @CheckResult
  public Completable setHidden(SubredditSubscription subscription, boolean hidden) {
    return Completable.fromAction(() -> {
      if (subscription.pendingState() == SubredditSubscription.PendingState.PENDING_UNSUBSCRIBE) {
        // When a subreddit gets marked for removal, the user should have not been able to toggle its hidden status.
        throw new IllegalStateException("Subreddit is marked for removal. Should have not reached here: " + subscription);
      }

      SubredditSubscription updated = subscription.toBuilder()
          .isHidden(hidden)
          .build();
      database.get().update(SubredditSubscription.TABLE_NAME, updated.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());
    });
  }

  @CheckResult
  public Completable removeAll() {
    return Completable.fromAction(() -> database.get().delete(SubredditSubscription.TABLE_NAME, null));
  }

  /**
   * Execute pending-subscribe and pending-unsubscribe actions that failed earlier because of some error.
   */
  @CheckResult
  public Completable executePendingSubscribesAndUnsubscribes() {
    return database.get().createQuery(SubredditSubscription.TABLE_NAME, SubredditSubscription.QUERY_GET_ALL_PENDING)
        .mapToList(SubredditSubscription.MAPPER)
        .take(1)
        .flatMapIterable(subscriptions -> subscriptions)
        .flatMapCompletable(pendingSubscription -> {
          if (pendingSubscription.isSubscribePending()) {
            Timber.i("Subscribing to %s", pendingSubscription.name());
            return reddit.get().subreddits()
                .findOld(pendingSubscription.name())
                .flatMapCompletable(subreddit -> subscribe(subreddit));
          } else {
            Timber.i("Unsubscribing from %s", pendingSubscription.name());
            return unsubscribe(pendingSubscription);
          }
        });
  }

  @CheckResult
  public Observable<Boolean> isSubscribed(String subredditName) {
    // This internally calls getAll(), which fetches new subscriptions in case the DB is empty.
    return getAllIncludingHidden()
        .map(subscriptions -> {
          for (SubredditSubscription subscription : subscriptions) {
            if (subscription.name().equalsIgnoreCase(subredditName)) {
              return true;
            }
          }
          return false;
        });
  }

// ======== DEFAULT SUBREDDIT ======== //

  public String defaultSubreddit() {
    return userPreferences.get().defaultSubreddit(appContext.get().getString(R.string.frontpage_subreddit_name));
  }

  public void setAsDefault(SubredditSubscription subscription) {
    userPreferences.get().setDefaultSubreddit(subscription.name());
  }

  public void resetDefaultSubreddit() {
    userPreferences.get().setDefaultSubreddit(appContext.get().getString(R.string.frontpage_subreddit_name));
  }

  public boolean isDefault(SubredditSubscription subscription) {
    return subscription.name().equalsIgnoreCase(defaultSubreddit());
  }

// ======== REMOTE SUBREDDITS ======== //

  @CheckResult
  private Single<List<SubredditSubscription>> fetchRemoteSubscriptions(List<SubredditSubscription> localSubs) {
    //Timber.w("Fetching subscriptions");
    Single<List<String>> subredditsStream = userSessionRepository.get().isUserLoggedIn()
        ? loggedInUserSubscriptions()
        : Single.just(loggedOutSubreddits());
    return subredditsStream
        .compose(applySchedulersSingle())
        .map(mergeRemoteSubscriptionsWithLocal(localSubs));
  }

  /**
   * Kind of similar to how git merge works. The local subscription are considered as the source of truth for
   * pending subscribe and unsubscribe subscription.
   */
  @VisibleForTesting()
  Function<List<String>, List<SubredditSubscription>> mergeRemoteSubscriptionsWithLocal(List<SubredditSubscription> localSubs) {
    return remoteSubNames -> {
      // So we've received subreddits from the server. Before overriding our database table with these,
      // retain pending-subscribe items and pending-unsubscribe items.
      Set<String> remoteSubsNamesSet = new HashSet<>(remoteSubNames.size());
      remoteSubsNamesSet.addAll(remoteSubNames);

      List<SubredditSubscription> syncedListBuilder = new ArrayList<>();

      // Go through the local dataset first to find items that we and/or the remote already have.
      for (SubredditSubscription localSub : localSubs) {
        if (remoteSubsNamesSet.contains(localSub.name())) {
          // Remote still has this sub.
          if (localSub.isUnsubscribePending()) {
            //noinspection ResultOfMethodCallIgnored
            syncedListBuilder.add(localSub);

          } else if (localSub.isSubscribePending()) {
            // A pending subscribed sub has already been subscribed on remote. Great.
            //noinspection ResultOfMethodCallIgnored
            syncedListBuilder.add(localSub.toBuilder().pendingState(SubredditSubscription.PendingState.NONE).build());

          } else {
            // Both local and remote have the same sub. All cool.
            //noinspection ResultOfMethodCallIgnored
            syncedListBuilder.add(localSub);
          }

        } else {
          // Remote doesn't have this sub.
          if (localSub.isSubscribePending()) {
            // Oh okay, we haven't been able to make the subscribe API call yet.
            //noinspection ResultOfMethodCallIgnored
            syncedListBuilder.add(localSub);

          } else if (!reddit.get().subscriptions().needsRemoteSubscription(localSub.name())) {
            // This subscription doesn't need to be on remote. Let's keep it.
            syncedListBuilder.add(localSub);
          }
          // Else, sub has been removed on remote! Will not add this sub.
        }
      }

      // Do a second pass to find items that the remote has but we don't.
      Map<String, SubredditSubscription> localSubsMap = new HashMap<>(localSubs.size());
      for (SubredditSubscription localSub : localSubs) {
        localSubsMap.put(localSub.name(), localSub);
      }
      for (String remoteSubName : remoteSubNames) {
        if (!localSubsMap.containsKey(remoteSubName)) {
          // New sub found.
          //noinspection ResultOfMethodCallIgnored
          syncedListBuilder.add(SubredditSubscription.create(remoteSubName, SubredditSubscription.PendingState.NONE, false));
        }
      }

      return Collections.unmodifiableList(syncedListBuilder);
    };
  }

  // TODO: Retry on network errors.
  private Single<List<String>> loggedInUserSubscriptions() {
    return reddit.get().subscriptions().userSubscriptions()
        .map(remoteSubs -> {
          List<String> remoteSubNames = new ArrayList<>(remoteSubs.size());
          for (Subreddit subreddit : remoteSubs) {
            remoteSubNames.add(subreddit.getName());
          }

          // Add frontpage and /r/popular.
          String frontpageSub = appContext.get().getString(R.string.frontpage_subreddit_name);
          remoteSubNames.add(0, frontpageSub);

          String popularSub = appContext.get().getString(R.string.popular_subreddit_name);
          if (!remoteSubNames.contains(popularSub) && !remoteSubNames.contains(popularSub.toLowerCase(Locale.ENGLISH))) {
            remoteSubNames.add(1, popularSub);
          }

          return remoteSubNames;
        })
        .map(toImmutable());
  }

  private List<String> loggedOutSubreddits() {
    return Arrays.asList(appContext.get().getResources().getStringArray(R.array.default_subreddits));
  }

  /**
   * Replace all items in the database with a new list of subscriptions.
   */
  private Consumer<List<SubredditSubscription>> saveSubscriptionsToDatabase() {
    return newSubscriptions -> {
      Timber.i("Saved %s subscriptions", newSubscriptions.size());

      List<ContentValues> newSubscriptionValuesList = new ArrayList<>(newSubscriptions.size());
      for (SubredditSubscription newSubscription : newSubscriptions) {
        newSubscriptionValuesList.add(newSubscription.toContentValues());
      }

      try (BriteDatabase.Transaction transaction = database.get().newTransaction()) {
        database.get().delete(SubredditSubscription.TABLE_NAME, null);
        for (ContentValues newSubscriptionValues : newSubscriptionValuesList) {
          database.get().insert(SubredditSubscription.TABLE_NAME, newSubscriptionValues);
        }
        transaction.markSuccessful();
      }
    };
  }

// ======== VISIT FREQUENCY ======== //

  @CheckResult
  public Completable incrementVisitCount(SubredditSubscription subscription) {
    return Completable.fromAction(() -> {
      SubredditSubscription updatedSubscription = subscription.toBuilder()
          .visitCount(subscription.visitCount() + 1)
          .build();

      database.get().update(
          SubredditSubscription.TABLE_NAME,
          updatedSubscription.toContentValues(),
          SubredditSubscription.WHERE_NAME,
          subscription.name());
    });
  }

  @CheckResult
  public Observable<List<SubredditSubscription>> frequents() {
    // TODO.
    return Observable.empty();
  }
}
