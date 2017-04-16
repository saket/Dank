package me.saket.dank.data;

import static me.saket.dank.data.SubredditSubscription.TABLE_NAME;
import static me.saket.dank.utils.CommonUtils.toImmutable;
import static rx.Observable.just;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableList;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Subreddit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import me.saket.dank.R;
import me.saket.dank.data.SubredditSubscription.PendingState;
import rx.Completable;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Manages:
 * <p>
 * - Fetching user's subscription (cached or fresh or both)
 * - Subscribing
 * - Un-subscribing.
 * - Hiding subscriptions.
 */
public class SubredditSubscriptionManager {

    private Context appContext;
    private BriteDatabase database;
    private DankRedditClient dankRedditClient;
    private UserPrefsManager userPrefsManager;

    public SubredditSubscriptionManager(Context context, BriteDatabase database, DankRedditClient dankRedditClient,
            UserPrefsManager userPrefsManager)
    {
        this.appContext = context;
        this.database = database;
        this.dankRedditClient = dankRedditClient;
        this.userPrefsManager = userPrefsManager;
    }

    public boolean isFrontpage(String subredditName) {
        return appContext.getString(R.string.frontpage_subreddit_name).equals(subredditName);
    }

    /**
     * Gets user's subscriptions from the database.
     *
     * @param searchQuery Can be empty, but not null.
     */
    public Observable<List<SubredditSubscription>> search(String searchQuery, boolean includeHidden) {
        String getQuery = includeHidden
                ? SubredditSubscription.QUERY_SEARCH_ALL_SUBSCRIBED_INCLUDING_HIDDEN
                : SubredditSubscription.QUERY_SEARCH_ALL_SUBSCRIBED_EXCLUDING_HIDDEN;

        return database
                .createQuery(TABLE_NAME, getQuery, "%" + searchQuery + "%")
                .mapToList(SubredditSubscription.MAPPER)
                .map(toImmutable())
                .flatMap(filteredSubs -> {
                    if (filteredSubs.isEmpty()) {
                        // Check if the database is empty and fetch fresh subscriptions from remote if needed.
                        return database
                                .createQuery(TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
                                .mapToList(SubredditSubscription.MAPPER)
                                .first()
                                .flatMap(localSubs -> {
                                    if (localSubs.isEmpty()) {
                                        return refreshSubscriptions(localSubs)
                                                // Don't let this stream emit anything. A change in the database will anyway trigger that.
                                                .flatMap(o -> Observable.never());

                                    } else {
                                        return just(filteredSubs);
                                    }
                                });
                    } else {
                        return just(filteredSubs);
                    }
                })
                .map(filteredSubs -> {
                    // Move Frontpage and Popular to the top.
                    String frontpageSubName = appContext.getString(R.string.frontpage_subreddit_name);
                    String popularSubName = appContext.getString(R.string.popular_subreddit_name);

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

                    return ImmutableList.copyOf(reOrderedFilteredSubs);
                });
    }

    @CheckResult
    public Completable refreshSubscriptions() {
        return database
                .createQuery(TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
                .mapToList(SubredditSubscription.MAPPER)
                .first()
                .flatMap(localSubscriptions -> refreshSubscriptions(localSubscriptions))
                .toCompletable();
    }

    @NonNull
    private Observable<List<SubredditSubscription>> refreshSubscriptions(List<SubredditSubscription> localSubs) {
        return fetchRemoteSubscriptions(localSubs)
                .filter(subs -> !subs.isEmpty())
                .doOnNext(saveSubscriptionsToDatabase());
    }

    @CheckResult
    public Completable subscribe(String subredditName) {
        return Completable.fromAction(() -> {
            SubredditSubscription subscription = SubredditSubscription.create(subredditName, PendingState.PENDING_SUBSCRIBE, false);
            database.update(TABLE_NAME, subscription.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());

            dispatchSyncWithRedditJob();
        });
    }

    @CheckResult
    public Completable unsubscribe(SubredditSubscription subscription) {
        return Completable.fromAction(() -> {
            SubredditSubscription updated = SubredditSubscription.create(subscription.name(), PendingState.PENDING_UNSUBSCRIBE, subscription.isHidden());
            database.update(TABLE_NAME, updated.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());

            dispatchSyncWithRedditJob();
        });
    }

    @CheckResult
    public Completable setHidden(SubredditSubscription subscription, boolean hidden) {
        return Completable.fromAction(() -> {
            if (subscription.pendingState() == PendingState.PENDING_UNSUBSCRIBE) {
                // When a subreddit gets marked for removal, the user should have not been able to toggle its hidden status.
                throw new IllegalStateException("Subreddit is marked for removal. Should have not reached here: " + subscription);
            }

            SubredditSubscription updated = SubredditSubscription.create(subscription.name(), subscription.pendingState(), hidden);
            database.update(TABLE_NAME, updated.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());

            dispatchSyncWithRedditJob();
        });
    }

    /**
     * Removes all subreddit subscriptions.
     */
    @CheckResult
    public Completable removeAll() {
        return Completable.fromAction(() -> database.delete(TABLE_NAME, null));
    }

    private void dispatchSyncWithRedditJob() {
        // TODO: 15/04/17 Sync with Reddit.
    }

// ======== DEFAULT ======== //

    public String defaultSubreddit() {
        return userPrefsManager.defaultSubreddit(appContext.getString(R.string.frontpage_subreddit_name));
    }

    public void setAsDefault(SubredditSubscription subscription) {
        userPrefsManager.setDefaultSubreddit(subscription.name());
    }

    public void resetDefaultSubreddit() {
        userPrefsManager.setDefaultSubreddit(appContext.getString(R.string.frontpage_subreddit_name));
    }

    public boolean isDefault(SubredditSubscription subscription) {
        return subscription.name().equalsIgnoreCase(defaultSubreddit());
    }

// ======== REMOTE SUBREDDITS ======== //

    private Observable<List<SubredditSubscription>> fetchRemoteSubscriptions(List<SubredditSubscription> localSubs) {
        return (dankRedditClient.isUserLoggedIn() ? loggedInUserSubreddits() : just(loggedOutSubreddits()))
                .map(mergeRemoteSubscriptionsWithLocal(localSubs));
    }

    /**
     * Kind of similar to how git merge works. The local subscription are considered as the source of truth for
     * pending subscribe and unsubscribe subscription.
     */
    @NonNull
    @VisibleForTesting()
    Func1<List<String>, List<SubredditSubscription>> mergeRemoteSubscriptionsWithLocal(List<SubredditSubscription> localSubs) {
        return remoteSubNames -> {
            // So we've received subreddits from the server. Before overriding our database table with these,
            // retain pending-subscribe items and pending-unsubscribe items.
            Set<String> remoteSubsNamesSet = new HashSet<>(remoteSubNames.size());
            for (String remoteSubName : remoteSubNames) {
                remoteSubsNamesSet.add(remoteSubName);
            }

            ImmutableList.Builder<SubredditSubscription> syncedListBuilder = ImmutableList.builder();

            // Go through the local dataset first to find items that we and/or the remote already have.
            for (SubredditSubscription localSub : localSubs) {
                if (remoteSubsNamesSet.contains(localSub.name())) {
                    // Remote still has this sub.
                    if (localSub.isUnsubscribePending()) {
                        syncedListBuilder.add(localSub);

                    } else if (localSub.isSubscribePending()) {
                        // A pending subscribed sub has already been subscribed on remote. Great.
                        syncedListBuilder.add(localSub.toBuilder().pendingState(PendingState.NONE).build());

                    } else {
                        // Both local and remote have the same sub. All cool.
                        syncedListBuilder.add(localSub);
                    }

                } else {
                    // Remote doesn't have this sub.
                    if (localSub.isSubscribePending()) {
                        // Oh okay, we haven't been able to make the subscribe API call yet.
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
                    syncedListBuilder.add(SubredditSubscription.create(remoteSubName, PendingState.NONE, false));
                }
            }

            return syncedListBuilder.build();
        };
    }

    @NonNull
    private Observable<List<String>> loggedInUserSubreddits() {
        return Observable.fromCallable(() -> {
            List<Subreddit> remoteSubs = dankRedditClient.userSubredditsPaginator().accumulateMergedAllSorted();
            List<String> remoteSubNames = new ArrayList<>(remoteSubs.size());
            for (Subreddit subreddit : remoteSubs) {
                remoteSubNames.add(subreddit.getDisplayName());
            }

            // Add frontpage and /r/popular.
            String frontpageSub = appContext.getString(R.string.frontpage_subreddit_name);
            remoteSubNames.add(0, frontpageSub);

            String popularSub = appContext.getString(R.string.popular_subreddit_name);
            if (!remoteSubNames.contains(popularSub) && !remoteSubNames.contains(popularSub.toLowerCase(Locale.ENGLISH))) {
                remoteSubNames.add(1, popularSub);
            }

            return remoteSubNames;
        });
    }

    private List<String> loggedOutSubreddits() {
        return Arrays.asList(appContext.getResources().getStringArray(R.array.default_subreddits));
    }

    /**
     * Replace all items in the database with a new list of subscriptions.
     */
    @NonNull
    private Action1<List<SubredditSubscription>> saveSubscriptionsToDatabase() {
        return newSubscriptions -> {
            try (BriteDatabase.Transaction transaction = database.newTransaction()) {
                database.delete(TABLE_NAME, null);

                for (SubredditSubscription freshSubscription : newSubscriptions) {
                    database.insert(TABLE_NAME, freshSubscription.toContentValues());
                }

                transaction.markSuccessful();
            }
        };
    }

}
