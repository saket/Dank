package me.saket.dank.data;

import android.net.Uri;
import android.text.Html;

import net.dean.jraw.models.Thumbnails;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Details of an image (including GIF) or a video that can be played within in the app.
 */
public class MediaLink extends Link {

    protected String url;
    private Type type;
    private Thumbnails redditSuppliedImages;
    private boolean canUseRedditOptimizedImageUrl;

    /**
     * @param canUseRedditOptimizedImages Reddit provides self-hosted copies of the content for images (For GIFs and videos, it
     *                                    provides static images). When this is true, Reddit's copies will be used instead of the
     *                                    actual URL. This will always be true for static images and false for GIFs and videos.
     */
    protected MediaLink(String url, boolean canUseRedditOptimizedImages, Type type) {
        this.url = url;
        this.canUseRedditOptimizedImageUrl = canUseRedditOptimizedImages;
        this.type = type;
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
        return url;
    }

    public String lowQualityVideoUrl() {
        if (type() != Type.VIDEO) {
            throw new IllegalStateException("Not a video");
        }
        return url;
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

    public static MediaLink create(String url, boolean canUseRedditOptimizedImageUrl, Type type) {
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
        public Gfycat(String url, Type type) {
            super(url, false, type);
        }

        public static Gfycat create(String url) {
            return new Gfycat(url, Type.VIDEO);
        }

        @Override
        public String lowQualityVideoUrl() {
            Uri gfycatURI = Uri.parse(url);
            return gfycatURI.getScheme() + "://thumbs.gfycat.com" + gfycatURI.getPath() + "-mobile.mp4";
        }

        @Override
        public String highQualityVideoUrl() {
            Uri gfycatURI = Uri.parse(url);
            return gfycatURI.getScheme() + "://zippy.gfycat.com" + gfycatURI.getPath() + ".webm";
        }
    }

}
