package me.saket.dank;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.util.Locale;

import me.saket.dank.ui.DankActivity;

public class RedditLinkParserActivity extends DankActivity {

    private static final String KEY_REDDIT_LINK_TYPE = "redditLinkType";

    public enum RedditLinkType {
        SUBMISSION,

        /**
         * Eg., https://redd.it/5524cd.
         */
        SUBMISSION_SHORT_LINK,

        /**
         * https://www.reddit.com/comments/5zqk2z/droids_interrupt_darth_vader_interview/.
         */
        SUBMISSION_WITHOUT_SUB,

        COMMENT_PERMALINK,
        SUBREDDIT,
        LIVE,
        WIKI,
        USER,
        UNKNOWN
    }

    /**
     * @return True if this link was handled. False otherwise.
     */
    public static boolean handle(Context context, String link) {
        RedditLinkType linkType = parseRedditLinkType(link);
        if (linkType != RedditLinkType.UNKNOWN) {
            Intent intent = new Intent(context, RedditLinkParserActivity.class);
            intent.putExtra(KEY_REDDIT_LINK_TYPE, linkType);
            context.startActivity(intent);
            return true;
        }
        return false;
    }

    /**
     * Determine type of the link.
     */
    public static RedditLinkType parseRedditLinkType(String link) {
        Uri linkUri = Uri.parse(link);
        String linkHost = linkUri.getHost() != null ? linkUri.getHost().toLowerCase(Locale.ENGLISH) : "";
        String linkPath = linkUri.getPath() != null ? linkUri.getPath().toLowerCase(Locale.ENGLISH) : "";

        if (isEmpty(linkHost) && linkPath.startsWith("/r/")) {
            return RedditLinkType.SUBMISSION;

        } else if (isEmpty(linkHost) && linkPath.startsWith("/u/")) {
            return RedditLinkType.USER;

        } else if (linkHost.contains("reddit.com")) {
            if (linkPath.matches("(?i)/r/[a-z0-9-_.]+/comments/\\w+.*")) {
                // Submission. Format: reddit.com/r/$subreddit/comments/$post_id/$post_title [post title is optional]
                return RedditLinkType.SUBMISSION;

            } else if (linkPath.matches("(?i)/comments/\\w+.*")) {
                // Submission without a given subreddit. Format: reddit.com/comments/$post_id/$post_title [post title is optional]
                return RedditLinkType.SUBMISSION_WITHOUT_SUB;

            } else if (linkPath.matches("(?i)/r/[a-z0-9-_.]+/comments/\\w+/\\w*/.*")) {
                // Permalink to comments. Format: reddit.com/r/$subreddit/comments/$post_id/$post_title [can be empty]/$comment_id
                return RedditLinkType.COMMENT_PERMALINK;

            } else if (linkPath.matches("(?i)/r/[a-z0-9-_.]+.*")) {
                // Subreddit. Format: reddit.com/r/$subreddit/$sort [sort is optional]
                return RedditLinkType.SUBREDDIT;

            } else if (linkPath.matches("(?i)/live/[^/]*")) {
                return RedditLinkType.LIVE;

            } else if (linkPath.matches("(?i)(?:/r/[a-z0-9-_.]+)?/(?:wiki|help).*")) {
                // Wiki link. Format: reddit.com/r/$subreddit/wiki/$page [page is optional]
                return RedditLinkType.WIKI;

            } else if (linkPath.matches("(?i)/u(?:ser)?/[a-z0-9-_]+.*")) {
                // User. Format: reddit.com/u [or user]/$username/$page [page is optional]
                return RedditLinkType.USER;
            }

        } else if (linkHost.endsWith("redd.it")) {
            // Short redd.it link. Format: redd.it/post_id. Eg., https://redd.it/5524cd
            return RedditLinkType.SUBMISSION_SHORT_LINK;

        } else {
            if (linkHost.contains("google")
                    && linkPath.startsWith("/amp/s/amp.reddit.com")) {
                // Google AMP link: https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
                return parseRedditLinkType("https://" + link.substring(link.indexOf("/amp/s/") + "/amp/s/".length()));
            }
        }

        return RedditLinkType.UNKNOWN;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RedditLinkType linkType = (RedditLinkType) getIntent().getSerializableExtra(KEY_REDDIT_LINK_TYPE);
        switch (linkType) {
            case SUBMISSION:
                break;

            case SUBMISSION_SHORT_LINK:
                break;

            case SUBMISSION_WITHOUT_SUB:
                break;

            case COMMENT_PERMALINK:
                break;

            case SUBREDDIT:
                break;

            case LIVE:
                break;

            case WIKI:
                break;

            case USER:
                break;

            case UNKNOWN:
                break;
        }
        Toast.makeText(this, linkType.toString(), Toast.LENGTH_SHORT).show();

        finish();
    }

}
