package me.saket.dank.data;

import android.content.Context;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.SubmissionRequest;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.SubredditPaginator;

import me.saket.dank.di.Dank;
import me.saket.dank.utils.AndroidTokenStore;
import rx.Observable;
import timber.log.Timber;

/**
 * Wrapper around {@link RedditClient}.
 */
public class DankRedditClient {

    private final String redditAppSecret;
    private final RedditClient redditClient;
    private final AuthenticationManager redditAuthManager;
    private final Context context;

    private boolean authManagerInitialized;

    public DankRedditClient(Context context, String redditAppSecret, RedditClient redditClient, AuthenticationManager redditAuthManager) {
        this.context = context;
        this.redditAppSecret = redditAppSecret;
        this.redditClient = redditClient;
        this.redditAuthManager = redditAuthManager;
    }

    /**
     * Get a new API token if we haven't already or refresh the existing API token if the last one has expired.
     */
    public Observable<Boolean> authenticateIfNeeded() {
        return Observable.fromCallable(() -> {
            if (!authManagerInitialized) {
                redditAuthManager.init(redditClient, new RefreshTokenHandler(new AndroidTokenStore(context), redditClient));
                authManagerInitialized = true;
            }

            AuthenticationState authState = redditAuthManager.checkAuthState();
            if (authState != AuthenticationState.READY) {
                Credentials credentials = Credentials.userlessApp(redditAppSecret, Dank.sharedPrefs().getDeviceUuid());

                switch (authState) {
                    case NONE:
                        Timber.d("Authenticating app");
                        redditClient.authenticate(redditClient.getOAuthHelper().easyAuth(credentials));
                        break;

                    case NEED_REFRESH:
                        Timber.d("Refreshing token");
                        redditAuthManager.refreshAccessToken(credentials);
                        break;
                }
            } else {
                Timber.d("Already authenticated");
            }

            return true;
        });
    }

    public Observable<Boolean> refreshApiToken() {
        return Observable.fromCallable(() -> {
            Credentials credentials = Credentials.userlessApp(redditAppSecret, Dank.sharedPrefs().getDeviceUuid());
            redditAuthManager.refreshAccessToken(credentials);
            return true;
        });
    }

    public SubredditPaginator frontPagePaginator() {
        return new SubredditPaginator(redditClient);
    }

    /**
     * Get all details of submissions, including comments.
     */
    public Submission fullSubmissionData(String submissionId) {
        return redditClient.getSubmission(new SubmissionRequest.Builder(submissionId).build());
    }
}
