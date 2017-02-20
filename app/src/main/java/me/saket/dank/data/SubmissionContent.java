package me.saket.dank.data;

import android.net.Uri;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import me.saket.dank.utils.SubmissionContentParser;

/**
 * Contains information of a submission's URL parsed by {@link SubmissionContentParser}.
 */
public class SubmissionContent {

    private Type type;
    private Host host;
    private Uri contentUri;

    public enum Type {
        SELF,
        LINK,
        IMAGE,
        VIDEO,
        UNKNOWN
    }

    public enum Host {
        // Images
        IMGUR,
        REDDIT,
        GFYCAT,
        QUICKMEME,

        // Videos
        YOUTUBE,
        TWITCH_TV,

        UNKNOWN,
        NONE,
    }

    public static SubmissionContent create(Uri contentUri, Type type, Host contentHost) {
        return new SubmissionContent(contentUri, type, contentHost);
    }

    private SubmissionContent(Uri contentUri, Type type, Host contentHost) {
        this.type = type;
        this.host = contentHost;
        this.contentUri = contentUri;
    }

    public String contentUrl() {
        return contentUri.toString();
    }

    public Type type() {
        return type;
    }

    public Host host() {
        return host;
    }

    /**
     * URI in caps so that we don't accidentally use this instead of {@link #contentUrl()}.
     */
    Uri contentURI() {
        return contentUri;
    }

    public boolean isImageOrVideo() {
        return type() == Type.IMAGE || type() == Type.VIDEO;
    }

    public boolean isSelfText() {
        return type() == Type.SELF;
    }

    public boolean isImage() {
        return type() == Type.IMAGE;
    }

    public boolean isLink() {
        return type() == Type.LINK;
    }

    /**
     * Whether or not the HTML can be scraped of certain hosts can be scraped manually to find the
     * image/video content.
     * <p>
     * TODO: Scrape.
     */
    public boolean canContentBeScraped() {
        switch (host) {
            case QUICKMEME:
            case TWITCH_TV:
                return true;

            default:
                return false;
        }
    }

    public <T extends SubmissionContent> T hostContent() {
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public String toString() {
        return "SubmissionContent{" +
                "type=" + type +
                ", host=" + host +
                ", contentUrl=" + contentUrl() +
                '}';
    }

    public static class Imgur extends SubmissionContent {

        private static final Set<String> DOMAINS = new HashSet<>(Arrays.asList(
                "imgur.com",        // Link to an Imgur submission.
                "i.imgur.com",      // Direct link to Imgur submission's image/video.
                "b.bildgur.de",     // Bildgur is Imgur's german cousin.
                "bildgur.de"
        ));

        public static SubmissionContent create(Uri contentUri, Type contentType, Host contentHost) {
            return new Imgur(contentUri, contentType, contentHost);
        }

        private Imgur(Uri contentUri, Type type, Host contentHost) {
            super(contentUri, type, contentHost);
        }

        public boolean isAlbum() {
            String urlPath = contentURI().getPath();
            return urlPath.startsWith("/a/") || urlPath.startsWith("/gallery/") || urlPath.startsWith("/g/") || urlPath.contains(",");
        }

    }

    public static class Gfycat extends SubmissionContent {

        private static final Set<String> VIDEO_DOMAINS = new HashSet<>(Arrays.asList(
                "gfycat.com",
                "zippy.gfycat.com",     // For MP4s.
                "fat.gfycat.com"        // For WebM.
        ));

        private static final Set<String> GIF_DOMAINS = new HashSet<>(Arrays.asList(
                "thumbs.gfycat.com",    // For thumbnail GIFs. URLs end with "-size_restricted.gif".
                "giant.gfycat.com"      // For original GIFs.
        ));

        public static Gfycat create(Uri contentUri, Host contentHost) {
            Type gfycatContentType = VIDEO_DOMAINS.contains(contentUri.getPath()) ? Type.VIDEO : Type.IMAGE;
            return new Gfycat(contentUri, gfycatContentType, contentHost);
        }

        private Gfycat(Uri contentUri, Type type, Host contentHost) {
            super(contentUri, type, contentHost);
        }

    }

}
