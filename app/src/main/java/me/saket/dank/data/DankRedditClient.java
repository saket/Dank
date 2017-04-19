package me.saket.dank.data;

import android.content.Context;
import android.support.annotation.NonNull;

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
import net.dean.jraw.paginators.SubredditPaginator;
import net.dean.jraw.paginators.UserSubredditsPaginator;

import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.AndroidTokenStore;
import me.saket.dank.utils.DankSubmissionRequest;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;
import timber.log.Timber;

/**
 * Wrapper around {@link RedditClient}.
 */
public class DankRedditClient {

    public static final CommentSort DEFAULT_COMMENT_SORT = CommentSort.TOP;

    private final RedditClient redditClient;
    private final AuthenticationManager redditAuthManager;
    private final Credentials loggedInUserCredentials;
    private final Credentials userlessAppCredentials;
    private final RefreshTokenHandler tokenHandler;

    private boolean authManagerInitialized;
    private Subject<Boolean, Boolean> onRedditClientAuthenticatedRelay = BehaviorSubject.create();

    public DankRedditClient(Context context, RedditClient redditClient, AuthenticationManager redditAuthManager) {
        this.redditClient = redditClient;
        this.redditAuthManager = redditAuthManager;

        String redditAppClientId = context.getString(R.string.reddit_app_client_id);
        loggedInUserCredentials = Credentials.installedApp(redditAppClientId, context.getString(R.string.reddit_app_redirect_url));
        userlessAppCredentials = Credentials.userlessApp(redditAppClientId, Dank.sharedPrefs().getDeviceUuid());
        tokenHandler = new RefreshTokenHandler(new AndroidTokenStore(context), redditClient);
    }

    /**
     * A relay that emits an event when the Reddit client has been authenticated.
     * This should happen only once app cold start.
     */
    public Subject<Boolean, Boolean> onRedditClientAuthenticated() {
        return onRedditClientAuthenticatedRelay;
    }

    public SubredditPaginator subredditPaginator(String subredditName) {
        if (Dank.subscriptionManager().isFrontpage(subredditName)) {
            return new SubredditPaginator(redditClient);
        } else {
            return new SubredditPaginator(redditClient, subredditName);
        }
    }

    public Single<Submission> submission(DankSubmissionRequest submissionRequest) {
        SubmissionRequest jrawSubmissionRequest = new SubmissionRequest.Builder(submissionRequest.id())
                .sort(submissionRequest.commentSort())
                .focus(submissionRequest.focusComment())
                .context(submissionRequest.contextCount())
                .limit(submissionRequest.commentLimit())
                .build();

        return Single.fromCallable(() -> redditClient.getSubmission(jrawSubmissionRequest));
    }

    /**
     * Load more replies of a comment node.
     */
    public Func1<CommentNode, CommentNode> loadMoreComments() {
        return commentNode -> {
            commentNode.loadMoreComments(redditClient);
            return commentNode;
        };
    }

// ======== AUTHENTICATION ======== //

    /**
     * Ensures that the app is authorized to make Reddit API calls and execute <var>wrappedObservable</var> to be specific.
     */
    public <T> Single<T> withAuth(Single<T> wrappedObservable) {
        return Dank.reddit()
                .authenticateIfNeeded()
                .andThen(wrappedObservable)
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired());
    }

    public Completable withAuth(Completable completable) {
        return Dank.reddit()
                .authenticateIfNeeded()
                .andThen(completable)
                .retryWhen(Dank.reddit().refreshApiTokenAndRetryIfExpired());
    }

    /**
     * Get a new API token if we haven't already or refresh the existing API token if the last one has expired.
     */
    private Completable authenticateIfNeeded() {
        return Completable.fromCallable(() -> {
            if (!authManagerInitialized) {
                redditAuthManager.init(redditClient, tokenHandler);
                authManagerInitialized = true;
            }

            AuthenticationState authState = redditAuthManager.checkAuthState();
            if (authState != AuthenticationState.READY) {
                switch (authState) {
                    case NONE:
                        //Timber.d("Authenticating userless app");
                        redditClient.authenticate(redditClient.getOAuthHelper().easyAuth(userlessAppCredentials));
                        onRedditClientAuthenticatedRelay.onNext(true);
                        break;

                    case NEED_REFRESH:
                        boolean isFirstRefreshSinceAppStart = !redditClient.isAuthenticated();
                        Timber.d("Refreshing token. isFirstRefreshSinceAppStart? %s", isFirstRefreshSinceAppStart);
                        redditAuthManager.refreshAccessToken(loggedInUserCredentials);

                        if (isFirstRefreshSinceAppStart) {
                            onRedditClientAuthenticatedRelay.onNext(true);
                        }
                        break;
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
    private Func1<Observable<? extends Throwable>, Observable<?>> refreshApiTokenAndRetryIfExpired() {
        return errors -> errors.flatMap(error -> {
            if (error instanceof NetworkException && ((NetworkException) error).getResponse().getStatusCode() == 401) {
                // Re-try authenticating.
                Timber.w("Attempting to refresh token");

                return isUserLoggedIn()
                        .flatMapObservable(loggedIn -> Observable.fromCallable(() -> {
                            redditAuthManager.refreshAccessToken(loggedIn ? loggedInUserCredentials : userlessAppCredentials);
                            return true;
                        }));

            } else {
                return Observable.error(error);
            }
        });
    }

    public UserLoginHelper userLoginHelper() {
        return new UserLoginHelper();
    }

    public Completable logout() {
        Action0 revokeAccessTokenAction = () -> {
            // Bug workaround: revokeAccessToken() method crashes if logging is enabled.
            LoggingMode modeBackup = redditClient.getLoggingMode();
            redditClient.setLoggingMode(LoggingMode.NEVER);
            redditClient.getOAuthHelper().revokeAccessToken(loggedInUserCredentials);
            redditClient.setLoggingMode(modeBackup);
        };

        return Completable
                .fromAction(revokeAccessTokenAction)
                .andThen(Dank.subscriptionManager().removeAll());
    }

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
        public Observable<Boolean> parseOAuthSuccessUrl(String successUrl) {
            return Observable.fromCallable(() -> {
                OAuthData oAuthData = redditClient.getOAuthHelper().onUserChallenge(successUrl, loggedInUserCredentials);
                redditClient.authenticate(oAuthData);
                return true;
            });
        }

    }

// ======== USER ACCOUNT ======== //

    public String loggedInUserName() {
        return redditClient.getAuthenticatedUser();
    }

    public Single<Boolean> isUserLoggedIn() {
        return onRedditClientAuthenticated()
                .map(o -> redditClient.isAuthenticated() && redditClient.hasActiveUserContext())
                .first()
                .toSingle();
    }

    public Single<LoggedInAccount> loggedInUserAccount() {
        return Single.fromCallable(() -> redditClient.me());
    }

    public AccountManager userAccountManager() {
        return new AccountManager(redditClient);
    }

// ======== SUBREDDITS ======== //

    public UserSubredditsPaginator userSubredditsPaginator() {
        UserSubredditsPaginator subredditsPaginator = new UserSubredditsPaginator(redditClient, "subscriber");
        subredditsPaginator.setLimit(200);
        return subredditsPaginator;
    }

    public Single<Subreddit> findSubreddit(String name) {
        return Single.fromCallable(() -> redditClient.getSubreddit(name));
    }

    public Completable subscribeTo(Subreddit subreddit) {
        return withAuth(Completable.fromAction(() -> userAccountManager().subscribe(subreddit)));
    }

    public Completable unsubscribeFrom(Subreddit subreddit) {
        return Completable.fromAction(() -> userAccountManager().unsubscribe(subreddit));
    }

}
