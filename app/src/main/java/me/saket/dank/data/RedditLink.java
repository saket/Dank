package me.saket.dank.data;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.RedditUrlParser;

/**
 * Contains information of a reddit.com URL parsed by {@link RedditUrlParser}.
 */
public interface RedditLink extends Parcelable {

    @AutoValue
    abstract class Subreddit implements RedditLink, Parcelable {
        public abstract String name();

        public static Subreddit create(String subredditName) {
            return new AutoValue_RedditLink_Subreddit(subredditName);
        }
    }

    @AutoValue
    abstract class User implements RedditLink, Parcelable {
        public abstract String name();

        public static User create(String userName) {
            return new AutoValue_RedditLink_User(userName);
        }
    }

    @AutoValue
    abstract class Submission implements RedditLink, Parcelable {
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
            return new AutoValue_RedditLink_Submission(subredditName, id, null);
        }

        public static Submission createWithComment(String subredditName, String id, Comment initialComment) {
            return new AutoValue_RedditLink_Submission(subredditName, id, initialComment);
        }
    }

    @AutoValue
    abstract class Comment implements RedditLink, Parcelable {
        public abstract String id();

        /**
         * Number of parents to show.
         */
        public abstract Integer contextCount();

        public static Comment create(String id, Integer contextCount) {
            return new AutoValue_RedditLink_Comment(id, contextCount);
        }
    }

    @AutoValue
    abstract class LiveThread implements RedditLink, Parcelable {
        public abstract String url();

        public static LiveThread create(String threadUrl) {
            return new AutoValue_RedditLink_LiveThread(threadUrl);
        }
    }

}
