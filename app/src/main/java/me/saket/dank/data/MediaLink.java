package me.saket.dank.data;

import android.net.Uri;
import android.support.annotation.Nullable;

import net.dean.jraw.models.Thumbnails;

import java.util.List;

import me.saket.dank.di.DankApi;
import me.saket.dank.utils.Commons;

/**
 * Details of an image (including GIF) or a video that can be played within in the app.
 */
public class MediaLink extends Link {

  protected String url;
  private String highQualityVideoUrl;
  private String lowQualityVideoUrl;
  private Type type;
  protected Thumbnails redditSuppliedImages;
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

  public String originalUrl() {
    return url;
  }

  /**
   * Used in {@link #optimizedImageUrl(int)}.
   */
  public MediaLink setRedditSuppliedImages(Thumbnails redditImages) {
    redditSuppliedImages = redditImages;
    return this;
  }

  /**
   * When {@link #canUseRedditOptimizedImageUrl} is true, this method checks copies provided by Reddit
   * and finds the image whose width is closest to user's device display width.
   * <p>
   * Use this only for images.
   */
  public String optimizedImageUrl(int optimizeForWidth) {
    if (isVideo()) {
      throw new IllegalStateException("Cannot optimize video URLs");
    }

    if (canUseRedditOptimizedImageUrl && redditSuppliedImages != null) {
      return Commons.findOptimizedImage(redditSuppliedImages, optimizeForWidth);
    } else {
      return url;
    }
  }

  public String highQualityVideoUrl() {
    if (!isVideo()) {
      throw new IllegalStateException("Not a video");
    }
    return highQualityVideoUrl;
  }

  public String lowQualityVideoUrl() {
    if (!isVideo()) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MediaLink)) {
      return false;
    }

    MediaLink mediaLink = (MediaLink) o;

    if (canUseRedditOptimizedImageUrl != mediaLink.canUseRedditOptimizedImageUrl) {
      return false;
    }
    if (!url.equals(mediaLink.url)) {
      return false;
    }
    if (type != mediaLink.type) {
      return false;
    }
    return redditSuppliedImages != null ? redditSuppliedImages.equals(mediaLink.redditSuppliedImages) : mediaLink.redditSuppliedImages == null;
  }

  @Override
  public int hashCode() {
    int result = url.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + (redditSuppliedImages != null ? redditSuppliedImages.hashCode() : 0);
    result = 31 * result + (canUseRedditOptimizedImageUrl ? 1 : 0);
    return result;
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
  public static class StreamableUnresolved extends MediaLink {
    private final String videoId;

    public StreamableUnresolved(String url, String videoId) {
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

    public static StreamableUnresolved create(String url, String videoId) {
      return new StreamableUnresolved(url, videoId);
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

  public static class Imgur extends MediaLink {
    @Nullable private String title;
    @Nullable private String description;

    public static Imgur create(String url, boolean canUseRedditOptimizedImageUrl, String title, String description) {
      Type type = url.endsWith("mp4") ? Type.VIDEO : Type.IMAGE_OR_GIF;
      return new Imgur(url, canUseRedditOptimizedImageUrl, type, title, description);
    }

    private Imgur(String url, boolean canUseRedditOptimizedImageUrl, Type type, @Nullable String title, @Nullable String description) {
      super(url, canUseRedditOptimizedImageUrl, type);
      this.title = title;
      this.description = description;
    }

    @Nullable
    public String title() {
      return title;
    }

    @Nullable
    public String description() {
      return description;
    }
  }

  /**
   * A Imgur.com/gallery link, whose actual image count is unknown. It could be a single image/gif or an album.
   */
  public static class ImgurUnresolvedGallery extends MediaLink {
    private String albumId;

    protected ImgurUnresolvedGallery(String albumUrl, String albumId) {
      super(albumUrl, false, Type.UNRESOLVED_IMGUR_GALLERY);
      this.albumId = albumId;
    }

    public String albumId() {
      return albumId;
    }

    public String albumUrl() {
      return url;
    }

    public static ImgurUnresolvedGallery create(String albumUrl, String albumId) {
      return new ImgurUnresolvedGallery(albumUrl, albumId);
    }
  }

  public static class ImgurAlbum extends MediaLink {
    private final String albumTitle;
    private final String coverImageUrl;
    private final List<MediaLink.Imgur> images;

    protected ImgurAlbum(String albumUrl, String albumTitle, String coverImageUrl, List<MediaLink.Imgur> images) {
      super(albumUrl, false, Type.EXTERNAL);
      this.albumTitle = albumTitle;
      this.coverImageUrl = coverImageUrl;
      this.images = images;
    }

    public String albumUrl() {
      return url;
    }

    public String coverImageUrl() {
      return coverImageUrl;
    }

    public String albumTitle() {
      return albumTitle;
    }

    public List<MediaLink.Imgur> images() {
      return images;
    }

    public ImgurAlbum withCoverImageUrl(String newCoverImageUrl) {
      return create(albumUrl(), albumTitle(), newCoverImageUrl, images());
    }

    public static ImgurAlbum create(String albumUrl, String albumTitle, String coverImageUrl, List<MediaLink.Imgur> images) {
      return new ImgurAlbum(albumUrl, albumTitle, coverImageUrl, images);
    }
  }
}
