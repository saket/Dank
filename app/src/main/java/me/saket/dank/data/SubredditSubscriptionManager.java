package me.saket.dank.data;

import static me.saket.dank.data.SubredditSubscription.TABLE_NAME;
import static me.saket.dank.utils.CommonUtils.toImmutable;
import static rx.Observable.just;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

import com.google.common.collect.ImmutableList;
import com.squareup.sqlbrite.BriteDatabase;

import net.dean.jraw.models.Subreddit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import me.saket.dank.R;
import me.saket.dank.data.SubredditSubscription.PendingState;
import rx.Completable;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

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
                        // TODO: 15/04/17 What happens if both local and remote subscriptions are empty?
                        return database
                                .createQuery(TABLE_NAME, SubredditSubscription.QUERY_GET_ALL)
                                .mapToList(SubredditSubscription.MAPPER)
                                .first()
                                .flatMap(localSubs -> {
                                    if (localSubs.isEmpty()) {
                                        return fetchRemoteSubscriptions(localSubs)
                                                .doOnNext(saveSubscriptionsToDatabase())
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
                        if (frontpageSub == null && subscription.name().equalsIgnoreCase(frontpageSubName)) {
                            reOrderedFilteredSubs.remove(i);
                            frontpageSub = subscription;
                            reOrderedFilteredSubs.add(0, frontpageSub);

                        } else if (popularSub == null && subscription.name().equalsIgnoreCase(popularSubName)) {
                            // Found frontpage!
                            reOrderedFilteredSubs.remove(i);
                            popularSub = subscription;
                            reOrderedFilteredSubs.add(0, popularSub);
                        }
                    }
                    
                    return ImmutableList.copyOf(reOrderedFilteredSubs);
                });
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

    private Observable<List<SubredditSubscription>> fetchRemoteSubscriptions(List<SubredditSubscription> localSubscriptions) {
        return (dankRedditClient.isUserLoggedIn() ? loggedInUserSubreddits() : just(loggedOutSubreddits()))
                .map(remoteSubNames -> {
                    Timber.i("Getting remote subs");

                    // So we've received subreddits from the server. Before overriding our database table with these,
                    // retain pending-subscribe items and pending-unsubscribe items.
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

                    // TODO: Retain pending-subscribe and pending-unsubscribe items.

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
            Timber.i("Saving to DB");

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
