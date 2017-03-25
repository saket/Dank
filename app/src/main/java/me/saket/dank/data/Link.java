package me.saket.dank.data;

import java.io.Serializable;

/**
 * Details of an URL.
 */
public abstract class Link implements Serializable {

    // TODO: 24/03/17 Is this required?
    public enum Type {
        REDDIT_HOSTED,
        IMAGE_OR_GIF,
        VIDEO,
        EXTERNAL;
    }
    public abstract Type type();

    public boolean isImageOrGif() {
        return type() == Link.Type.IMAGE_OR_GIF;
    }

    public boolean isExternal() {
        return type() == Type.EXTERNAL;
    }

    public boolean isRedditHosted() {
        return type() == Type.REDDIT_HOSTED;
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
