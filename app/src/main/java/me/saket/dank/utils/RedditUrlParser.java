package me.saket.dank.utils;

import static android.text.TextUtils.isEmpty;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.saket.dank.data.RedditUrl;

/**
 *
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
     * /r/$subreddit/comments/$post_id/post_title/$comment_id. post_title can be empty.
     */
    private static final Pattern COMMENT_PATTERN = Pattern.compile("(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)/\\w*/(\\w+).");

    /**
     * /r/$subreddit/comments/$post_id/ or /comments/$post_id/
     */
    private static final Pattern SUBMISSION_PATTERN = Pattern.compile("(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)/.*");

    /**
     * Determine type of the url.
     *
     * @return null if the url couldn't be identified. A class implementing {@link RedditUrl} otherwise.
     */
    @Nullable
    public static RedditUrl parse(String url) {
        // TODO: Should we support "np" subdomain?
        // TODO: Support context count in comments.

        Uri linkUri = Uri.parse(url);
        String urlHost = linkUri.getHost() != null ? linkUri.getHost() : "";
        String urlPath = linkUri.getPath() != null ? linkUri.getPath() : "";

        if (urlHost.endsWith("reddit.com")) {
            Matcher commentMatcher = COMMENT_PATTERN.matcher(urlPath);
            if (commentMatcher.matches()) {
                return parseCommentUrl(commentMatcher);
            }

            Matcher submMatcher = SUBMISSION_PATTERN.matcher(urlPath);
            if (submMatcher.matches()) {
                return parseSubmissionUrl(submMatcher);
            }

        } else if (isEmpty(urlHost)) {
            Matcher subredditMatcher = SUBREDDIT_PATTERN.matcher(urlPath);
            if (subredditMatcher.matches()) {
                return parseSubredditUrl(subredditMatcher);
            }

            Matcher userMatcher = USER_PATTERN.matcher(urlPath);
            if (userMatcher.matches()) {
                return parseUserUrl(userMatcher);
            }
        }

//            } else if (urlPath.matches("/live/[^/]*")) {
//                return RedditUrl.Type.LIVE;
//
//            } else if (urlPath.matches("(?:/r/[a-z0-9-_.]+)?/(?:wiki|help).*")) {
//                // Wiki url. Format: reddit.com/r/$subreddit/wiki/$page [page is optional]
//                return RedditUrl.Type.WIKI;
//
        else if (urlHost.endsWith("redd.it")) {
            // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
            return parseSubmissionShortUrl(urlPath);

        } else if (urlHost.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
            // Google AMP url: https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
            String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
            return parse(nonAmpUrl);
        }

        return null;
    }

    private static RedditUrl.Subreddit parseSubredditUrl(Matcher subredditMatcher) {
        return RedditUrl.Subreddit.create(subredditMatcher.group(1));
    }

    private static RedditUrl.User parseUserUrl(Matcher userMatcher) {
        return RedditUrl.User.create(userMatcher.group(1));
    }

    private static RedditUrl.Comment parseCommentUrl(Matcher commentMatcher) {
        String subredditName = commentMatcher.group(2);
        String submissionId = commentMatcher.group(3);
        String commentId = commentMatcher.group(4);
        return RedditUrl.Comment.create(subredditName, submissionId, commentId);
    }

    private static RedditUrl.Submission parseSubmissionUrl(Matcher submissionMatcher) {
        String subredditName = submissionMatcher.group(2);
        String submissionId = submissionMatcher.group(3);
        return RedditUrl.Submission.create(subredditName, submissionId);
    }

    private static RedditUrl.Submission parseSubmissionShortUrl(String urlPath) {
        return RedditUrl.Submission.create(null, urlPath);
    }

}
