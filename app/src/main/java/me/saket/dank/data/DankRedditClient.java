package me.saket.dank.data;

import net.dean.jraw.RedditClient;

/**
 * Wrapper around {@link RedditClient}.
 */
public class DankRedditClient {

  // ======== AUTHENTICATION ======== //

  /**
   * Get a new API token if we haven't already or refresh the existing API token if the last one has expired.
   */
//  private synchronized Completable authenticateIfNeeded() {
//    return Completable.fromCallable(() -> {
//      if (!authManagerInitialized) {
//        redditAuthManager.init(redditClient, tokenHandler);
//        authManagerInitialized = true;
//      }
//
//      AuthenticationState authState = redditAuthManager.checkAuthState();
//      if (authState != AuthenticationState.READY) {
//        switch (authState) {
//          case NONE:
//            Timber.d("Authenticating userless app");
//            redditClient.authenticate(redditClient.getOAuthHelper().easyAuth(userlessAppCredentials));
//            break;
//
//          case NEED_REFRESH:
//            Timber.d("Refreshing token");
//            redditAuthManager.refreshAccessToken(loggedInUserCredentials);
//            break;
//        }
//
//        if (onRedditClientAuthenticatedRelay.getValue() == null) {
//          onRedditClientAuthenticatedRelay.accept(true);
//        }
//      } //else {
//      //Timber.d("Already authenticated");
//      //}
//
//      // A dummy return value is required because Action0 doesn't handle Exceptions like Callable.
//      return true;
//    });
//  }

  /**
   * Although refreshing token is already handled by @link #authenticateIfNeeded(), it is possible that the
   * token expires right after it returns and a Reddit API call is made. Or it is also possible that the access
   * token got revoked somehow and the server is returning a 401. In both cases, this method attempts to
   * re-authenticate.
   */
//  private Function<Flowable<Throwable>, Publisher<Boolean>> refreshApiTokenIfExpiredAndRetry() {
//    return errors -> errors.flatMap(error -> {
//      if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 401) {
//        // Re-try authenticating.
//        Timber.w("Attempting to refresh token");
//
//        return Flowable.fromCallable(() -> {
//          redditAuthManager.refreshAccessToken(userSessionRepository.isUserLoggedIn() ? loggedInUserCredentials : userlessAppCredentials);
//          return true;
//        });
//
//      } else {
//        return Flowable.error(error);
//      }
//    });
//  }

  // TODO: Move to UserSession.java
//  public UserLoginHelper createUserLoginHelper() {
//    return new UserLoginHelper();
//  }

  // TODO: Move to UserSession.java
//  public Completable logout() {
//    return Completable.fromAction(() -> {
//      redditClient.getOAuthHelper().revokeAccessToken(loggedInUserCredentials);
//      userSessionRepository.removeLoggedInUsername();
//      Dank.subscriptions().resetDefaultSubreddit();
//    });
//  }

// ======== OTHER USERS ======== //

//  public Account userAccount(String username) {
//    return redditClient.getUser(username);
//  }

// ======== SUBREDDITS ======== //

//  @CheckResult
//  public Single<List<Subreddit>> userSubreddits() {
//    return withAuth(Single.fromCallable(() -> {
//      UserSubredditsPaginator subredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");
//      subredditsPaginator.setLimit(200);
//      return subredditsPaginator.accumulateMergedAllSorted();
//    }));
//  }
//
//  @Deprecated
//  @CheckResult
//  public Single<Subscribeable> findSubreddit(String name) {
//    if (name.equalsIgnoreCase("frontpage")) {
//      return Single.just(Subscribeable.local("Frontpage"));
//    }
//    if (name.equalsIgnoreCase("popular")) {
//      return Single.just(Subscribeable.local("Popular"));
//    }
//
//    return withAuth(Single.fromCallable(() -> redditClient.getSubreddit(name)))
//        .map(subreddit -> Subscribeable.create(subreddit));
//  }
//
//  @CheckResult
//  public Single<SubredditSearchResult> findSubreddit2(String name) {[
//    // TODO: Find a better way to keep these two methods in sync.
//    // IMPORTANT: Keep them in sync with needsRemoteSubscription().
//    if (name.equalsIgnoreCase("frontpage")) {
//      return Single.just(SubredditSearchResult.success(Subscribeable.local("Frontpage")));
//    }
//    if (name.equalsIgnoreCase("popular")) {
//      return Single.just(SubredditSearchResult.success(Subscribeable.local("Popular")));
//    }
//
//    return withAuth(Single.fromCallable(() -> redditClient.getSubreddit(name)))
//        .map(subreddit -> Subscribeable.create(subreddit))
//        .map(dankSubreddit -> SubredditSearchResult.success(dankSubreddit))
//        .onErrorReturn(error -> {
//          if (error instanceof IllegalArgumentException && error.getMessage().contains("is private")) {
//            return SubredditSearchResult.privateError();
//
//          } else if (error instanceof IllegalArgumentException && error.getMessage().contains("does not exist")
//              || (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 404))
//          {
//            return SubredditSearchResult.notFound();
//
//          } else {
//            return SubredditSearchResult.unknownError(error);
//          }
//        });
//  }

//  public boolean needsRemoteSubscription(String subredditName) {
//    return !subredditName.equalsIgnoreCase("frontpage") && !subredditName.equalsIgnoreCase("popular");
//  }

//  public Completable subscribeTo(Subreddit subreddit) {
//    return withAuth(Completable.fromAction(() -> userAccountManager().subscribe(subreddit)));
//  }
//
//  public Completable unsubscribeFrom(Subreddit subreddit) {
//    return Completable.fromAction(() -> userAccountManager().unsubscribe(subreddit));
//  }
}
