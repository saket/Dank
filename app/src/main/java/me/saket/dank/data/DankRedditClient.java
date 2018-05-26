package me.saket.dank.data;

import android.content.Context;
import android.support.annotation.CheckResult;

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.SubmissionRequest;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthData;
import net.dean.jraw.http.oauth.OAuthHelper;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Account;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;
import net.dean.jraw.models.LoggedInAccount;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Subreddit;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.UUID;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.subreddit.SubredditSearchResult;
import me.saket.dank.ui.subreddit.Subscribeable;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.AndroidTokenStore;
import me.saket.dank.utils.DankSubmissionRequest;
import timber.log.Timber;

/**
 * Wrapper around {@link RedditClient}.
 */
public class DankRedditClient {

  public static final String CONTEXT_QUERY_PARAM = "context";
  public static final int COMMENT_DEFAULT_CONTEXT_COUNT = 3;

  // "Confidence" is now "Best".
  public static final CommentSort DEFAULT_COMMENT_SORT = CommentSort.CONFIDENCE;

  private final RedditClient redditClient;
  private final AuthenticationManager redditAuthManager;
  private UserSessionRepository userSessionRepository;
  private final Credentials loggedInUserCredentials;
  private final Credentials userlessAppCredentials;
  private final RefreshTokenHandler tokenHandler;

  private boolean authManagerInitialized;
  private BehaviorRelay<Boolean> onRedditClientAuthenticatedRelay;

  public DankRedditClient(
      Context context,
      RedditClient redditClient,
      AuthenticationManager redditAuthManager,
      UserSessionRepository userSessionRepository,
      UUID deviceUuid)
  {
    this.redditClient = redditClient;
    this.redditAuthManager = redditAuthManager;
    this.userSessionRepository = userSessionRepository;

    redditClient.setLoggingMode(LoggingMode.ON_FAIL);

    String redditAppClientId = context.getString(R.string.reddit_app_client_id);
    loggedInUserCredentials = Credentials.installedApp(redditAppClientId, context.getString(R.string.reddit_app_redirect_url));
    userlessAppCredentials = Credentials.userlessApp(redditAppClientId, deviceUuid);
    tokenHandler = new RefreshTokenHandler(new AndroidTokenStore(context), redditClient);
    onRedditClientAuthenticatedRelay = BehaviorRelay.create();
  }

  /**
   * A relay that emits an event when the Reddit client has been authenticated.
   * This should happen only once app cold start.
   */
  public BehaviorRelay<Boolean> onRedditClientAuthenticated() {
    return onRedditClientAuthenticatedRelay;
  }

  public SubredditPaginator subredditPaginator(String subredditName, boolean isFrontpage) {
    if (isFrontpage) {
      return new SubredditPaginator(redditClient);
    } else {
      return new SubredditPaginator(redditClient, subredditName);
    }
  }

  @CheckResult
  public Single<Submission> submission(DankSubmissionRequest submissionRequest) {
    SubmissionRequest jrawSubmissionRequest = submissionRequest.toJraw();
    return withAuth(Single.fromCallable(() -> redditClient.getSubmission(jrawSubmissionRequest)));
  }

  /**
   * Load more replies of a comment node.
   *
   * @return List of new comments.
   */
  @CheckResult
  public Single<List<CommentNode>> loadMoreComments(CommentNode commentNode) {
    return Single.fromCallable(() -> commentNode.loadMoreComments(redditClient));
  }

// ======== AUTHENTICATION ======== //

  /**
   * Ensures that the app is authorized to make Reddit API calls and execute <var>wrappedObservable</var> to be specific.
   */
  // TODO: Move to UserSession.java
  public <T> Single<T> withAuth(Single<T> wrappedObservable) {
    return Dank.reddit()
        .authenticateIfNeeded()
        .andThen(wrappedObservable)
        .retryWhen(refreshApiTokenIfExpiredAndRetry());
  }

  // TODO: Move to UserSession.java
  public Completable withAuth(Completable completable) {
    return Dank.reddit()
        .authenticateIfNeeded()
        .andThen(completable)
        .retryWhen(refreshApiTokenIfExpiredAndRetry());
  }

  // TODO: Move to UserSession.java
  public <T> Observable<T> withAuth(Observable<T> completable) {
    return Dank.reddit()
        .authenticateIfNeeded()
        .andThen(completable)
        .toFlowable(BackpressureStrategy.LATEST)
        .retryWhen(refreshApiTokenIfExpiredAndRetry())
        .toObservable();
  }

  /**
   * Get a new API token if we haven't already or refresh the existing API token if the last one has expired.
   */
  // TODO: Move to UserSession.java
  private synchronized Completable authenticateIfNeeded() {
    return Completable.fromCallable(() -> {
      if (!authManagerInitialized) {
        redditAuthManager.init(redditClient, tokenHandler);
        authManagerInitialized = true;
      }

      AuthenticationState authState = redditAuthManager.checkAuthState();
      if (authState != AuthenticationState.READY) {
        switch (authState) {
          case NONE:
            Timber.d("Authenticating userless app");
            redditClient.authenticate(redditClient.getOAuthHelper().easyAuth(userlessAppCredentials));
            break;

          case NEED_REFRESH:
            Timber.d("Refreshing token");
            redditAuthManager.refreshAccessToken(loggedInUserCredentials);
            break;
        }

        if (onRedditClientAuthenticatedRelay.getValue() == null) {
          onRedditClientAuthenticatedRelay.accept(true);
        }
      } //else {
      //Timber.d("Already authenticated");
      //}

      // A dummy return value is required because Action0 doesn't handle Exceptions like Callable.
      return true;
    });
  }

  /**
   * Although refreshing token is already handled by {@link #authenticateIfNeeded()}, it is possible that the
   * token expires right after it returns and a Reddit API call is made. Or it is also possible that the access
   * token got revoked somehow and the server is returning a 401. In both cases, this method attempts to
   * re-authenticate.
   */
  // TODO: Move to UserSession.java
  private Function<Flowable<Throwable>, Publisher<Boolean>> refreshApiTokenIfExpiredAndRetry() {
    return errors -> errors.flatMap(error -> {
      if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 401) {
        // Re-try authenticating.
        Timber.w("Attempting to refresh token");

        return Flowable.fromCallable(() -> {
          redditAuthManager.refreshAccessToken(userSessionRepository.isUserLoggedIn() ? loggedInUserCredentials : userlessAppCredentials);
          return true;
        });

      } else {
        return Flowable.error(error);
      }
    });
  }

  // TODO: Move to UserSession.java
  public UserLoginHelper createUserLoginHelper() {
    return new UserLoginHelper();
  }

  // TODO: Move to UserSession.java
  public Completable logout() {
    return Completable.fromAction(() -> {
      redditClient.getOAuthHelper().revokeAccessToken(loggedInUserCredentials);
      userSessionRepository.removeLoggedInUsername();
      Dank.subscriptions().resetDefaultSubreddit();
    });
  }

  // TODO: Move to UserSession.java
  public class UserLoginHelper {

    public UserLoginHelper() {
    }

    public String authorizationUrl() {
      String[] scopes = {
          "account",
          "edit",             // For editing comments and submissions
          "history",
          "identity",
          "mysubreddits",
          "privatemessages",
          "read",
          "report",           // For hiding or reporting a thread.
          "save",
          "submit",
          "subscribe",
          "vote",
          "wikiread"
      };

      OAuthHelper oAuthHelper = redditClient.getOAuthHelper();
      return oAuthHelper.getAuthorizationUrl(loggedInUserCredentials, true /* permanent */, true /* useMobileSite */, scopes).toString();
    }

    /**
     * Emits an item when the app is successfully able to authenticate the user in.
     */
    public Completable parseOAuthSuccessUrl(String successUrl) {
      return Completable.fromAction(() -> {
        OAuthData oAuthData = redditClient.getOAuthHelper().onUserChallenge(successUrl, loggedInUserCredentials);
        redditClient.authenticate(oAuthData);

        String username = redditClient.getAuthenticatedUser();
        userSessionRepository.setLoggedInUsername(username);
      });
    }
  }

// ======== LOGGED IN USER ACCOUNT ======== //

  // TODO: Move to UserSession.java
  public Single<LoggedInAccount> loggedInUserAccount() {
    return withAuth(Single.fromCallable(() -> redditClient.me()));
  }

  public AccountManager userAccountManager() {
    return new AccountManager(redditClient);
  }

  net.dean.jraw.managers.InboxManager redditInboxManager() {
    return new net.dean.jraw.managers.InboxManager(redditClient);
  }

  /**
   * @param folder See {@link InboxPaginator#InboxPaginator(RedditClient, String)}.
   */
  public InboxPaginator userMessagePaginator(InboxFolder folder) {
    return new InboxPaginator(redditClient, folder.value());
  }

// ======== OTHER USERS ======== //

  public Account userAccount(String username) {
    return redditClient.getUser(username);
  }

// ======== SUBREDDITS ======== //

  @CheckResult
  public Single<List<Subreddit>> userSubreddits() {
    return withAuth(Single.fromCallable(() -> {
      UserSubredditsPaginator subredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");
      subredditsPaginator.setLimit(200);
      return subredditsPaginator.accumulateMergedAllSorted();
    }));
  }

  @Deprecated
  @CheckResult
  public Single<Subscribeable> findSubreddit(String name) {
    if (name.equalsIgnoreCase("frontpage")) {
      return Single.just(Subscribeable.local("Frontpage"));
    }
    if (name.equalsIgnoreCase("popular")) {
      return Single.just(Subscribeable.local("Popular"));
    }

    return withAuth(Single.fromCallable(() -> redditClient.getSubreddit(name)))
        .map(subreddit -> Subscribeable.create(subreddit));
  }

  @CheckResult
  public Single<SubredditSearchResult> findSubreddit2(String name) {
    // TODO: Find a better way to keep these two methods in sync.
    // IMPORTANT: Keep them in sync with needsRemoteSubscription().
    if (name.equalsIgnoreCase("frontpage")) {
      return Single.just(SubredditSearchResult.success(Subscribeable.local("Frontpage")));
    }
    if (name.equalsIgnoreCase("popular")) {
      return Single.just(SubredditSearchResult.success(Subscribeable.local("Popular")));
    }

    return withAuth(Single.fromCallable(() -> redditClient.getSubreddit(name)))
        .map(subreddit -> Subscribeable.create(subreddit))
        .map(dankSubreddit -> SubredditSearchResult.success(dankSubreddit))
        .onErrorReturn(error -> {
          if (error instanceof IllegalArgumentException && error.getMessage().contains("is private")) {
            return SubredditSearchResult.privateError();

          } else if (error instanceof IllegalArgumentException && error.getMessage().contains("does not exist")
              || (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 404))
          {
            return SubredditSearchResult.notFound();

          } else {
            return SubredditSearchResult.unknownError(error);
          }
        });
  }

  public boolean needsRemoteSubscription(String subredditName) {
    return !subredditName.equalsIgnoreCase("frontpage") && !subredditName.equalsIgnoreCase("popular");
  }

  public Completable subscribeTo(Subreddit subreddit) {
    return withAuth(Completable.fromAction(() -> userAccountManager().subscribe(subreddit)));
  }

  public Completable unsubscribeFrom(Subreddit subreddit) {
    return Completable.fromAction(() -> userAccountManager().unsubscribe(subreddit));
  }
}
