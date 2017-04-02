package me.saket.dank.data;

import android.net.Uri;
import android.text.Html;

import net.dean.jraw.models.Thumbnails;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import me.saket.dank.di.DankApi;

/**
 * Details of an image (including GIF) or a video that can be played within in the app.
 */
public class MediaLink extends Link {

    protected String url;
    private String highQualityVideoUrl;
    private String lowQualityVideoUrl;
    private Type type;
    private Thumbnails redditSuppliedImages;
    private boolean canUseRedditOptimizedImageUrl;

    /**
     * @param canUseRedditOptimizedImages Reddit provides self-hosted copies of the content for images (For GIFs and videos, it
     *                                    provides static images). When this is true, Reddit's copies will be used instead of the
     */
    protected MediaLink(String url, String lowQualityVideoUrl, String highQualityVideoUrl, boolean canUseRedditOptimizedImages, Type type) {
        this.url = url;
        this.highQualityVideoUrl = highQualityVideoUrl;
        this.lowQualityVideoUrl = lowQualityVideoUrl;
        this.canUseRedditOptimizedImageUrl = canUseRedditOptimizedImages;
        this.type = type;
    }

    protected MediaLink(String url, boolean canUseRedditOptimizedImages, Type type) {
        this(url, url, url, canUseRedditOptimizedImages, type);
    }

    @Override
    public Type type() {
        return type;
    }

    /**
     * Used in {@link #optimizedImageUrl(int)}.
     */
    public void setRedditSuppliedImages(Thumbnails redditImages) {
        redditSuppliedImages = redditImages;
    }

    /**
     * When {@link #canUseRedditOptimizedImageUrl} is true, this method checks copies provided by Reddit
     * and finds the image whose width is closest to user's device display width.
     * <p>
     * Use this only for images.
     */
    public String optimizedImageUrl(int optimizeForWidth) {
        if (canUseRedditOptimizedImageUrl && redditSuppliedImages != null) {
            Thumbnails.Image closestImage = redditSuppliedImages.getSource();
            int closestDifference = optimizeForWidth - redditSuppliedImages.getSource().getWidth();

            for (Thumbnails.Image redditCopy : redditSuppliedImages.getVariations()) {
                int differenceAbs = Math.abs(optimizeForWidth - redditCopy.getWidth());

                if (differenceAbs < Math.abs(closestDifference)
                        // If another image is found with the same difference, choose the higher-res image.
                        || differenceAbs == closestDifference && redditCopy.getWidth() > closestImage.getWidth())
                {
                    closestDifference = optimizeForWidth - redditCopy.getWidth();
                    closestImage = redditCopy;
                }
            }

            //noinspection deprecation
            return Html.fromHtml(closestImage.getUrl()).toString();
        }

        return url;
    }

    public String highQualityVideoUrl() {
        if (type() != Type.VIDEO) {
            throw new IllegalStateException("Not a video");
        }
        return highQualityVideoUrl;
    }

    public String lowQualityVideoUrl() {
        if (type() != Type.VIDEO) {
            throw new IllegalStateException("Not a video");
        }
        return lowQualityVideoUrl;
    }

    @Override
    public String toString() {
        return "MediaLink{" +
                "url='" + url + '\'' +
                ", type=" + type +
                ", redditSuppliedImages=" + redditSuppliedImages +
                ", canUseRedditOptimizedImageUrl=" + canUseRedditOptimizedImageUrl +
                '}';
    }

    public static MediaLink createGeneric(String url, boolean canUseRedditOptimizedImageUrl, Type type) {
        return new MediaLink(url, canUseRedditOptimizedImageUrl, type);
    }

    public static class Imgur extends MediaLink {
        private static final Set<String> DOMAINS = new HashSet<>(Arrays.asList(
                "imgur.com",        // Link to an Imgur submission.
                "i.imgur.com",      // Direct link to Imgur submission's image/video.
                "b.bildgur.de",     // Bildgur is Imgur's german cousin.
                "bildgur.de"
        ));

        private Imgur(String url, boolean canUseRedditOptimizedImageUrl, Type type) {
            super(url, canUseRedditOptimizedImageUrl, type);
        }

        public boolean isAlbum() {
            String urlPath = Uri.parse(url).getPath();
            return urlPath.startsWith("/a/") || urlPath.startsWith("/gallery/") || urlPath.startsWith("/g/") || urlPath.contains(",");
        }

        public static Imgur create(String url, boolean canUseRedditOptimizedImageUrl) {
            Type type = url.endsWith("mp4") ? Type.VIDEO : Type.IMAGE_OR_GIF;
            return new Imgur(url, canUseRedditOptimizedImageUrl, type);
        }
    }

    public static class Gfycat extends MediaLink {
        public Gfycat(String url, String lowQualityVideoUrl, String highQualityVideoUrl, Type type) {
            super(url, lowQualityVideoUrl, highQualityVideoUrl, false, type);
        }

        public static Gfycat create(String url) {
            Uri gfycatURI = Uri.parse(url);
            String lowQualityVideoUrl = gfycatURI.getScheme() + "://thumbs.gfycat.com" + gfycatURI.getPath() + "-mobile.mp4";
            String highQualityVideoUrl = gfycatURI.getScheme() + "://zippy.gfycat.com" + gfycatURI.getPath() + ".webm";
            return new Gfycat(url, lowQualityVideoUrl, highQualityVideoUrl, Type.VIDEO);
        }
    }

    public static class Giphy extends MediaLink {
        private Giphy(String url) {
            super(url, false, Type.VIDEO);
        }

        public static Giphy create(String url) {
            return new Giphy(url);
        }
    }

    /**
     * Container for a streamable video's ID, which can only be used after its video URL has been
     * fetched using {@link DankApi#streamableVideoDetails(String)}.
     */
    public static class StreamableUnknown extends MediaLink {
        private final String videoId;

        public StreamableUnknown(String url, String videoId) {
            super(url, false, Type.VIDEO);
            this.videoId = videoId;
        }

        public String videoId() {
            return videoId;
        }

        @Override
        public String toString() {
            return "StreamableUnknown{" +
                    "videoId='" + videoId + '\'' +
                    '}';
        }

        public static StreamableUnknown create(String url, String videoId) {
            return new StreamableUnknown(url, videoId);
        }
    }

    public static class Streamable extends MediaLink {
        private Streamable(String url, String lowQualityVideoUrl, String highQualityVideoUrl) {
            super(url, lowQualityVideoUrl, highQualityVideoUrl, false, Type.VIDEO);
        }

        public static Streamable create(String streamableUrl, String lowQualityVideoUrl, String highQualityVideoUrl) {
            return new Streamable(streamableUrl, lowQualityVideoUrl, highQualityVideoUrl);
        }
    }

}
