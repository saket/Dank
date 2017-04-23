package me.saket.dank.data;

import android.support.annotation.Nullable;

/**
 * Details of a Reddit.com URL.
 */
public abstract class RedditLink extends Link {

    @Override
    public Type type() {
        return Type.REDDIT_HOSTED;
    }

    public static class Subreddit extends RedditLink {
        public String name;

        public static Subreddit create(String subredditName) {
            Subreddit subreddit = new Subreddit();
            subreddit.name = subredditName;
            return subreddit;
        }
    }

    public static class User extends RedditLink {
        public String name;

        public static User create(String userName) {
            User user = new User();
            user.name = userName;
            return user;
        }
    }

    public static class Submission extends RedditLink {
        @Nullable public String subredditName;
        public String url;
        public String id;

        /**
         * This is non-null if a comment's permalink was clicked. This comment is shown as
         * the root comment of the submission.
         */
        @Nullable public Comment initialComment;

        public Submission(String url, String id, @Nullable String subredditName, @Nullable Comment initialComment) {
            this.url = url;
            this.id = id;
            this.subredditName = subredditName;
            this.initialComment = initialComment;
        }

        public static Submission create(String url, String id, String subredditName) {
            return new Submission(url, id, subredditName, null);
        }

        public static Submission createWithComment(String url, String id, String subredditName, Comment initialComment) {
            return new Submission(url, id, subredditName, initialComment);
        }

        @Override
        public String toString() {
            return "Submission{" +
                    "subredditName='" + subredditName + '\'' +
                    ", url='" + url + '\'' +
                    ", id='" + id + '\'' +
                    ", initialComment=" + initialComment +
                    '}';
        }
    }

    public static class Comment extends RedditLink {
        public String id;

        /**
         * Number of parents to show.
         */
        public Integer contextCount;

        public static Comment create(String id, Integer contextCount) {
            Comment comment = new Comment();
            comment.id = id;
            comment.contextCount = contextCount;
            return comment;
        }

        @Override
        public String toString() {
            return "Comment{" +
                    "id='" + id + '\'' +
                    ", contextCount=" + contextCount +
                    '}';
        }
    }

    /**
     * A reddit.com URL that Dank doesn't support yet. E.g., a live thread.
     */
    public static class UnsupportedYet extends Link.External {
        public UnsupportedYet(String url) {
            super(url);
        }

        public static UnsupportedYet create(String url) {
            return new UnsupportedYet(url);
        }
    }

}
