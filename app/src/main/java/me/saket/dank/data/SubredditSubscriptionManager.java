package me.saket.dank.data;

import android.content.Context;
import android.support.annotation.NonNull;

import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Subreddit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import me.saket.dank.R;
import me.saket.dank.data.SubredditSubscription.PendingState;
import rx.Completable;
import rx.Observable;
import rx.functions.Action1;

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

    public SubredditSubscriptionManager(Context appContext, BriteDatabase database, DankRedditClient dankRedditClient) {
        this.appContext = appContext;
        this.database = database;
        this.dankRedditClient = dankRedditClient;
    }

    /**
     * Gets user's subscriptions from the database or from the network if they're not cached yet.
     */
    public Observable<List<SubredditSubscription>> getAll(boolean includeHidden) {
        String getQuery = includeHidden
                ? SubredditSubscription.QUERY_GET_ALL_SUBSCRIBED_INCLUDING_HIDDEN
                : SubredditSubscription.QUERY_GET_ALL_SUBSCRIBED_EXCLUDING_HIDDEN;

        return database
                .createQuery(SubredditSubscription.TABLE_NAME, getQuery)
                .mapToList(SubredditSubscription.MAPPER)
                .flatMap(subscriptions -> {
                    if (subscriptions.isEmpty()) {
                        // Check if the database is empty and fetch fresh subscriptions from remote if needed.
                        // TODO: 15/04/17 What happens if both local and remove subscriptions are empty?
                        return database
                                .createQuery(SubredditSubscription.TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
                                .mapToList(SubredditSubscription.MAPPER)
                                .flatMap(localSubscriptions -> {
                                    if (localSubscriptions.isEmpty()) {
                                        return fetchFreshSubscriptions(localSubscriptions)
                                                .doOnNext(saveSubscriptionsToDatabase());

                                    } else {
                                        return Observable.just(localSubscriptions);
                                    }
                                });
                    }

                    return Observable.just(subscriptions);
                });
    }

    public Completable subscribe(String subredditName) {
        return Completable.fromAction(() -> {
            SubredditSubscription subscription = SubredditSubscription.create(subredditName, PendingState.PENDING_SUBSCRIBE, false);
            database.update(SubredditSubscription.TABLE_NAME, subscription.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());

            dispatchSyncWithRedditJob();
        });
    }

    public Completable unsubscribe(SubredditSubscription subscription) {
        return Completable.fromAction(() -> {
            SubredditSubscription updated = SubredditSubscription.create(subscription.name(), PendingState.PENDING_UNSUBSCRIBE, subscription.isHidden());
            database.update(SubredditSubscription.TABLE_NAME, updated.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());

            dispatchSyncWithRedditJob();
        });
    }

    public Completable setHidden(SubredditSubscription subscription, boolean hidden) {
        return Completable.fromAction(() -> {
            if (subscription.pendingState() == PendingState.PENDING_UNSUBSCRIBE) {
                // When a subreddit gets marked for removal, the user should have not been able to toggle its hidden status.
                throw new IllegalStateException("Subreddit is marked for removal. Should have not reached here: " + subscription);
            }

            SubredditSubscription updated = SubredditSubscription.create(subscription.name(), subscription.pendingState(), hidden);
            database.update(SubredditSubscription.TABLE_NAME, updated.toContentValues(), SubredditSubscription.WHERE_NAME, subscription.name());
            dispatchSyncWithRedditJob();
        });
    }

    private void dispatchSyncWithRedditJob() {
        // TODO: 15/04/17 Sync with Reddit.
    }

// ======== REMOTE SUBREDDITS ======== //

    private Observable<List<SubredditSubscription>> fetchFreshSubscriptions(List<SubredditSubscription> localSubscriptions) {
        return (dankRedditClient.isUserLoggedIn() ? loggedInUserSubreddits() : Observable.just(loggedOutSubreddits()))
                .map(remoteSubNames -> {
                    // So we've received subreddits from the server. Before replacing our database table with these,
                    // we must insert pending-subscribe items and remove pending-unsubscribe items which haven't
                    // synced yet.
                    HashMap<String, SubredditSubscription> localSubsMap = new HashMap<>(localSubscriptions.size());
                    for (SubredditSubscription localSub : localSubscriptions) {
                        localSubsMap.put(localSub.name(), localSub);
                    }

                    // Construct a new list of subs based on the remote subreddits.
                    List<SubredditSubscription> finalSubreddits = new ArrayList<>(remoteSubNames.size());

                    for (String remoteSubName : remoteSubNames) {
                        if (localSubsMap.containsKey(remoteSubName)) {
                            // We already have this subreddit.
                            SubredditSubscription localCopy = localSubsMap.get(remoteSubName);

                            if (!localCopy.isUnsubscribePending()) {
                                SubredditSubscription stateClearedCopy = localCopy.toBuilder()
                                        .pendingState(PendingState.NONE)
                                        .build();
                                finalSubreddits.add(stateClearedCopy);
                            }

                        } else {
                            // New subreddit. User must have subscribed to this subreddit the website or another app (hopefully not).
                            finalSubreddits.add(SubredditSubscription.create(remoteSubName, PendingState.NONE, false));
                        }
                    }

                    return finalSubreddits;
                });
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
            remoteSubNames.add(0, "Frontpage");
            if (!remoteSubNames.contains("Popular") && !remoteSubNames.contains("popular")) {
                remoteSubNames.add(1, "Popular");
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
                database.delete(SubredditSubscription.TABLE_NAME, null);

                for (SubredditSubscription freshSubscription : newSubscriptions) {
                    database.insert(SubredditSubscription.TABLE_NAME, freshSubscription.toContentValues());
                }

                transaction.markSuccessful();
            }
        };
    }

}
