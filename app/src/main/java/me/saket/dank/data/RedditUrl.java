package me.saket.dank.data;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.RedditUrlParser;

/**
 * Contains information of a reddit.com URL parsed by {@link RedditUrlParser}.
 */
public interface RedditUrl extends Parcelable {

    @AutoValue
    abstract class Subreddit implements RedditUrl, Parcelable {
        public abstract String name();

        public static Subreddit create(String subredditName) {
            return new AutoValue_RedditUrl_Subreddit(subredditName);
        }
    }

    @AutoValue
    abstract class User implements RedditUrl, Parcelable {
        public abstract String name();

        public static User create(String userName) {
            return new AutoValue_RedditUrl_User(userName);
        }
    }

    @AutoValue
    abstract class Submission implements RedditUrl, Parcelable {
        @Nullable
        public abstract String subredditName();

        public abstract String id();

        /**
         * This is non-null if a comment's permalink was clicked. We should show this
         * comment as the root comment of the submission.
         */
        @Nullable
        public abstract Comment initialComment();

        public static Submission create(String subredditName, String id) {
            return new AutoValue_RedditUrl_Submission(subredditName, id, null);
        }

        public static Submission createWithComment(String subredditName, String id, Comment initialComment) {
            return new AutoValue_RedditUrl_Submission(subredditName, id, initialComment);
        }
    }

    @AutoValue
    abstract class Comment implements RedditUrl, Parcelable {
        public abstract String id();

        /**
         * Number of parents to show.
         */
        public abstract Integer contextCount();

        public static Comment create(String id, Integer contextCount) {
            return new AutoValue_RedditUrl_Comment(id, contextCount);
        }
    }

    @AutoValue
    abstract class LiveThread implements RedditUrl, Parcelable {
        public abstract String url();

        public static LiveThread create(String threadUrl) {
            return new AutoValue_RedditUrl_LiveThread(threadUrl);
        }
    }

}
