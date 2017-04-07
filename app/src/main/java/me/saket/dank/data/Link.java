package me.saket.dank.data;

import java.io.Serializable;

/**
 * Details of an URL.
 */
public abstract class Link implements Serializable {

    public enum Type {
        /**
         * Submission / user / subreddit.
         */
        REDDIT_HOSTED,

        IMAGE_OR_GIF,

        VIDEO,

        /**
         * A link that will be opened in a browser.
         */
        EXTERNAL,

        /**
         * When the count of images in an Imgur gallery hasn't been found yet.
         */
        UNRESOLVED_IMGUR_GALLERY,
    }

    public abstract Type type();

    public boolean isImageOrGif() {
        return type() == Link.Type.IMAGE_OR_GIF;
    }

    public boolean isVideo() {
        return type() == Type.VIDEO;
    }

    public boolean isExternal() {
        return type() == Type.EXTERNAL;
    }

    public boolean isRedditHosted() {
        return type() == Type.REDDIT_HOSTED;
    }

    public boolean isUnresolvedImgurGallery() {
        return type() == Type.UNRESOLVED_IMGUR_GALLERY;
    }

    /**
     * Link that can only be opened in a browser.
     */
    public static class External extends Link {
        public String url;

        @Override
        public Type type() {
            return Type.EXTERNAL;
        }

        public External(String url) {
            this.url = url;
        }

        public static External create(String url) {
            return new External(url);
        }
    }

}
