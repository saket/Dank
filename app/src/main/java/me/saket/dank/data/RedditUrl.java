package me.saket.dank.data;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.RedditUrlParser;

/**
 * Contains information of a reddit.com URL parsed by {@link RedditUrlParser}.
 */
public interface RedditUrl extends Parcelable {

    enum Type {
        SUBMISSION,
        COMMENT,
        SUBREDDIT,
        USER,

        // Unsupported for now.
        LIVE,
        WIKI,
    }

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
    abstract class Comment implements RedditUrl, Parcelable {
        @Nullable
        public abstract String subredditName();

        public abstract String submissionId();

        public abstract String id();

        public static Comment create(String subredditName, String submissionId, String id) {
            return new AutoValue_RedditUrl_Comment(subredditName, submissionId, id);
        }
    }

    @AutoValue
    abstract class Submission implements RedditUrl, Parcelable {
        @Nullable
        public abstract String subredditName();

        public abstract String id();

        public static Submission create(String subredditName, String id) {
            return new AutoValue_RedditUrl_Submission(subredditName, id);
        }
    }

}
