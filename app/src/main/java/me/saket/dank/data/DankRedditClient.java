package me.saket.dank.data;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;

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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.ui.user.messages.InboxFolder;
import me.saket.dank.utils.AndroidTokenStore;
import me.saket.dank.utils.DankSubmissionRequest;
import timber.log.Timber;

/**
 * Wrapper around {@link RedditClient}.
 */
public class DankRedditClient {

  public static final CommentSort DEFAULT_COMMENT_SORT = CommentSort.TOP;

  private final RedditClient redditClient;
  private final AuthenticationManager redditAuthManager;
  private UserSession userSession;
  private final Credentials loggedInUserCredentials;
  private final Credentials userlessAppCredentials;
  private final RefreshTokenHandler tokenHandler;

  private boolean authManagerInitialized;
  private BehaviorRelay<Boolean> onRedditClientAuthenticatedRelay;

  public DankRedditClient(Context context, RedditClient redditClient, AuthenticationManager redditAuthManager, UserSession userSession) {
    this.redditClient = redditClient;
    this.redditAuthManager = redditAuthManager;
    this.userSession = userSession;

    redditClient.setLoggingMode(LoggingMode.ON_FAIL);

    String redditAppClientId = context.getString(R.string.reddit_app_client_id);
    loggedInUserCredentials = Credentials.installedApp(redditAppClientId, context.getString(R.string.reddit_app_redirect_url));
    userlessAppCredentials = Credentials.userlessApp(redditAppClientId, Dank.sharedPrefs().getDeviceUuid());
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

  public SubredditPaginator subredditPaginator(String subredditName) {
    if (Dank.subscriptions().isFrontpage(subredditName)) {
      return new SubredditPaginator(redditClient);
    } else {
      return new SubredditPaginator(redditClient, subredditName);
    }
  }

  // TODO: Move to SubmissionManager.
  public Single<Submission> submission(DankSubmissionRequest submissionRequest) {
    SubmissionRequest jrawSubmissionRequest = new SubmissionRequest.Builder(submissionRequest.id())
        .sort(submissionRequest.commentSort())
        .focus(submissionRequest.focusComment())
        .context(submissionRequest.contextCount())
        .limit(submissionRequest.commentLimit())
        .build();

    return withAuth(Single.fromCallable(() -> redditClient.getSubmission(jrawSubmissionRequest)));
  }

  /**
   * Load more replies of a comment node.
   */
  public Function<CommentNode, CommentNode> loadMoreComments() {
    return commentNode -> {
      commentNode.loadMoreComments(redditClient);
      return commentNode;
    };
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
  @NonNull
  // TODO: Move to UserSession.java
  private Function<Flowable<Throwable>, Publisher<Boolean>> refreshApiTokenIfExpiredAndRetry() {
    return errors -> errors.flatMap(error -> {
      if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 401) {
        // Re-try authenticating.
        Timber.w("Attempting to refresh token");

        return Flowable.fromCallable(() -> {
          redditAuthManager.refreshAccessToken(userSession.isUserLoggedIn() ? loggedInUserCredentials : userlessAppCredentials);
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
    Action revokeAccessTokenAction = () -> {
      // Bug workaround: revokeAccessToken() method crashes if logging is enabled.
      redditClient.getOAuthHelper().revokeAccessToken(loggedInUserCredentials);
    };

    return Completable
        .fromAction(revokeAccessTokenAction)
        .andThen(Dank.subscriptions().removeAll());   // TODO: Use a broadcast instead.
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
        userSession.setLoggedInUsername(username);
      });
    }
  }

// ======== USER ACCOUNT ======== //

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

// ======== SUBREDDITS ======== //

  @CheckResult
  public Single<List<Subreddit>> userSubreddits() {
    return withAuth(Single.fromCallable(() -> {
      UserSubredditsPaginator subredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");
      subredditsPaginator.setLimit(200);
      return subredditsPaginator.accumulateMergedAllSorted();
    }));
  }

  @CheckResult
  public Single<Subreddit> findSubreddit(String name) {
    return withAuth(Single.fromCallable(() -> redditClient.getSubreddit(name)));
  }

  public Completable subscribeTo(Subreddit subreddit) {
    return withAuth(Completable.fromAction(() -> userAccountManager().subscribe(subreddit)));
  }

  public Completable unsubscribeFrom(Subreddit subreddit) {
    return Completable.fromAction(() -> userAccountManager().unsubscribe(subreddit));
  }

}
