package me.saket.dank.utils;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.saket.dank.data.RedditUrl;

/**
 * Parses reddit.com links and categorizes them into {@link RedditUrl} subclasses.
 */
public class RedditUrlParser {

    /**
     * /r/$subreddit
     */
    private static final Pattern SUBREDDIT_PATTERN = Pattern.compile("^/r/([a-zA-Z0-9-_.]+)(/)*$");

    /**
     * /u/$user
     */
    private static final Pattern USER_PATTERN = Pattern.compile("^/u/([a-zA-Z0-9-_.]+)(/)*$");

    /**
     * Submission: /r/$subreddit/comments/$post_id/post_title
     * Comment:    /r/$subreddit/comments/$post_id/post_title/$comment_id.
     * <p>
     * ('post_title' and '/r/$subreddit/' can be empty).
     */
    private static final Pattern SUBMISSION_OR_COMMENT_PATTERN = Pattern.compile("^(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)/\\w*/(\\w*).*");

    private static final Pattern LIVE_THREAD_PATTERN = Pattern.compile("^/live/\\w*(/)*$");

    /**
     * Determine type of the url.
     *
     * @return null if the url couldn't be identified. A class implementing {@link RedditUrl} otherwise.
     */
    @Nullable
    public static RedditUrl parse(String url) {
        // TODO: Should we support "np" subdomain?
        // TODO: Support context count in comments.
        // TODO: Support wiki pages.

        Uri linkUri = Uri.parse(url);
        String urlHost = linkUri.getHost() != null ? linkUri.getHost() : "";
        String urlPath = linkUri.getPath() != null ? linkUri.getPath() : "";

        if (urlHost.endsWith("reddit.com")) {
            Matcher submissionOrCommentMatcher = SUBMISSION_OR_COMMENT_PATTERN.matcher(urlPath);
            if (submissionOrCommentMatcher.matches()) {
                String subredditName = submissionOrCommentMatcher.group(2);
                String submissionId = submissionOrCommentMatcher.group(3);
                String commentId = submissionOrCommentMatcher.group(4);
                if (commentId.isEmpty()) {
                    return RedditUrl.Submission.create(subredditName, submissionId);
                } else {
                    return RedditUrl.Comment.create(subredditName, submissionId, commentId);
                }
            }

            Matcher liveThreadMatcher = LIVE_THREAD_PATTERN.matcher(urlPath);
            if (liveThreadMatcher.matches()) {
                return RedditUrl.LiveThread.create(url);
            }

        } else if (urlHost.isEmpty()) {
            Matcher subredditMatcher = SUBREDDIT_PATTERN.matcher(urlPath);
            if (subredditMatcher.matches()) {
                return RedditUrl.Subreddit.create(subredditMatcher.group(1));
            }

            Matcher userMatcher = USER_PATTERN.matcher(urlPath);
            if (userMatcher.matches()) {
                return RedditUrl.User.create(userMatcher.group(1));
            }

        } else if (urlHost.endsWith("redd.it")) {
            // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
            return RedditUrl.Submission.create(null, urlPath);

        } else if (urlHost.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
            // Google AMP url: https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
            String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
            return parse(nonAmpUrl);
        }

        return null;
    }

}
