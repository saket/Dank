package me.saket.dank.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.RedditLink;

/**
 * Parses URLs found in the wilderness of Reddit and categorizes them into {@link Link} subclasses.
 * <p>
 * DankUrlParser identifies URLs and mapping them to known websites like imgur, giphy, etc.
 * This class exists because Reddit's {@link Submission#getPostHint()} is not very accurate and
 * fails to identify a lot of URLs. For instance, it returns {@link Submission.PostHint#LINK}
 * for its own image hosting domain, redditupload.com images.
 * <p>
 * Use {@link #parse(String) to start}.
 */
public class UrlParser {

  /**
   * /r/$subreddit.
   */
  private static final Pattern SUBREDDIT_PATTERN = Pattern.compile("^/r/([a-zA-Z0-9-_.]+)(/)*$");

  /**
   * /u/$user.
   */
  private static final Pattern USER_PATTERN = Pattern.compile("^/u/([a-zA-Z0-9-_.]+)(/)*$");

  /**
   * Submission: /r/$subreddit/comments/$post_id/post_title.
   * Comment:    /r/$subreddit/comments/$post_id/post_title/$comment_id.
   * <p>
   * ('post_title' and '/r/$subreddit/' can be empty).
   */
  private static final Pattern SUBMISSION_OR_COMMENT_PATTERN = Pattern.compile("^(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)(/\\w*/(\\w*))?.*");

  /**
   * /live/$thread_id.
   */
  private static final Pattern LIVE_THREAD_PATTERN = Pattern.compile("^/live/\\w*(/)*$");

  /**
   * Extracts the three-word name of a gfycat until a '.' or '-' is encountered. Example URLs:
   * <p>
   * /MessySpryAfricancivet
   * /MessySpryAfricancivet.gif
   * /MessySpryAfricancivet-size_restricted.gif
   * /MessySpryAfricancivet.webm
   * /MessySpryAfricancivet-mobile.mp4
   */
  private static final Pattern GFYCAT_ID_PATTERN = Pattern.compile("^(/[^-.]*).*$");

  /**
   * Extracts the ID of a giphy link. In these examples, the ID is 'l2JJyLbhqCF4va86c
   * <p>
   * /media/l2JJyLbhqCF4va86c/giphy.mp4
   * /media/l2JJyLbhqCF4va86c/giphy.gif
   * /gifs/l2JJyLbhqCF4va86c/html5
   * /l2JJyLbhqCF4va86c.gif
   */
  private static final Pattern GIPHY_ID_PATTERN = Pattern.compile("^/(?:(?:media)?(?:gifs)?/)?(\\w*)[/.].*$");

  /**
   * Extracts the ID of a streamable link. Eg., https://streamable.com/fxn88 -> 'fxn88'.
   */
  private static final Pattern STREAMABLE_ID_PATTERN = Pattern.compile("/(\\w*\\d*)[^/.?]*");

  /**
   * Extracts the ID of an Imgur album.
   * <p>
   * /gallery/9Uq7u
   * /gallery/coZb0HC
   * /t/a_day_in_the_life/85Egn
   * /a/RBpAe
   */
  private static final Pattern IMGUR_ALBUM_PATTERN = Pattern.compile("/(?:gallery)?(?:a)?(?:t/\\w*)?/(\\w*).*");

  /**
   * Determine type of the url.
   *
   * @return null if the url couldn't be identified. A class implementing {@link RedditLink} otherwise.
   */
  @NonNull
  public static Link parse(String url) {
    // TODO: Support "np" subdomain?
    // TODO: Support wiki pages.

    Uri linkUri = Uri.parse(url);
    String urlDomain = linkUri.getHost() != null ? linkUri.getHost() : "";
    String urlPath = linkUri.getPath() != null ? linkUri.getPath() : "";

    Matcher subredditMatcher = SUBREDDIT_PATTERN.matcher(urlPath);
    if (subredditMatcher.matches()) {
      return RedditLink.Subreddit.create(subredditMatcher.group(1));
    }

    Matcher userMatcher = USER_PATTERN.matcher(urlPath);
    if (userMatcher.matches()) {
      return RedditLink.User.create(userMatcher.group(1));
    }

    if (urlDomain.endsWith("reddit.com")) {
      Matcher submissionOrCommentMatcher = SUBMISSION_OR_COMMENT_PATTERN.matcher(urlPath);
      if (submissionOrCommentMatcher.matches()) {
        String subredditName = submissionOrCommentMatcher.group(2);
        String submissionId = submissionOrCommentMatcher.group(3);
        String commentId = submissionOrCommentMatcher.group(5);

        if (TextUtils.isEmpty(commentId)) {
          return RedditLink.Submission.create(url, submissionId, subredditName);

        } else {
          String contextParamValue = linkUri.getQueryParameter("context");
          int contextCount = TextUtils.isEmpty(contextParamValue) ? 0 : Integer.parseInt(contextParamValue);
          RedditLink.Comment initialComment = RedditLink.Comment.create(commentId, contextCount);
          return RedditLink.Submission.createWithComment(url, submissionId, subredditName, initialComment);
        }
      }

      Matcher liveThreadMatcher = LIVE_THREAD_PATTERN.matcher(urlPath);
      if (liveThreadMatcher.matches()) {
        return RedditLink.UnsupportedYet.create(url);
      }

    } else if (urlDomain.endsWith("redd.it") && !isImageUrlPath(urlPath)) {
      // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
      String submissionId = urlPath.substring(1);  // Remove the leading slash.
      return RedditLink.Submission.create(url, submissionId, null);

    } else if (urlDomain.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
      // Google AMP url.
      // https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
      String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
      return parse(nonAmpUrl);
    }

    return parseNonRedditUrl(url);
  }

  public static Link parse(String url, Thumbnails redditSuppliedThumbnails) {
    Link parsedLink = parse(url);
    if (parsedLink instanceof MediaLink) {
      ((MediaLink) parsedLink).setRedditSuppliedImages(redditSuppliedThumbnails);
    }
    return parsedLink;
  }

  @NonNull
  private static Link parseNonRedditUrl(String url) {
    Uri contentURI = Uri.parse(url);
    String urlDomain = contentURI.getHost();
    String urlPath = contentURI.getPath();    // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

    if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
      if (isUnsupportedImgurLink(urlPath)) {
        // These are links that Imgur no longer uses so Dank does not expect them either.
        return Link.External.create(url);

      } else if (isImgurAlbum(urlPath)) {
        return createUnresolvedImgurGallery(url);

      } else {
        return createImgurLink(url);
      }

    } else if (urlDomain.contains("gfycat.com")) {
      return createGfycatLink(contentURI);

    } else if (urlDomain.contains("giphy.com")) {
      return createGiphyLink(contentURI);

    } else if (urlDomain.contains("streamable.com")) {
      return createUnknownStreamableLink(contentURI);

    } else if ((urlDomain.contains("reddituploads.com"))) {
      // Reddit sends HTML-escaped URLs. Decode them again.
      //noinspection deprecation
      String htmlUnescapedUrl = Html.fromHtml(url).toString();
      return MediaLink.createGeneric(htmlUnescapedUrl, !isGifUrlPath(urlPath), Link.Type.IMAGE_OR_GIF);

    } else if (isImageUrlPath(urlPath)) {
      return MediaLink.createGeneric(url, !isGifUrlPath(urlPath), Link.Type.IMAGE_OR_GIF);

    } else if (urlPath.endsWith(".mp4")) {
      return MediaLink.createGeneric(url, !isGifUrlPath(urlPath), Link.Type.VIDEO);

    } else {
      return Link.External.create(url);
    }
  }

  /**
   * It's titled as unresolved because we don't know if the gallery contains a single image or multiple images.
   */
  private static Link createUnresolvedImgurGallery(String albumUrl) {
    Matcher albumUrlMatcher = IMGUR_ALBUM_PATTERN.matcher(Uri.parse(albumUrl).getPath());
    if (albumUrlMatcher.matches()) {
      String albumId = albumUrlMatcher.group(1);
      return MediaLink.ImgurUnresolvedGallery.create(albumUrl, albumId);

    } else {
      // Fallback.
      return Link.External.create(albumUrl);
    }
  }

  private static MediaLink.Imgur createImgurLink(String url) {
    // Convert GIFs to MP4s that are insanely light weight in size.
    String[] gifFormats = new String[] { ".gif", ".gifv" };
    for (String gifFormat : gifFormats) {
      if (url.endsWith(gifFormat)) {
        url = url.substring(0, url.length() - gifFormat.length()) + ".mp4";
      }
    }

    Uri contentURI = Uri.parse(url);

    // Attempt to get direct links to images from Imgur submissions.
    // For example, convert 'http://imgur.com/djP1IZC' to 'http://i.imgur.com/djP1IZC.jpg'.
    if (!isImageUrlPath(url) && !url.endsWith("mp4")) {
      // If this happened to be a GIF submission, the user sadly will be forced to see it
      // instead of its GIFV.
      contentURI = Uri.parse(contentURI.getScheme() + "://i.imgur.com" + contentURI.getPath() + ".jpg");
    }

    // Reddit provides its own copies for the content in multiple sizes. Use that only in
    // case of images because otherwise it'll be a static image for GIFs or videos.
    boolean canUseRedditOptimizedImageUrl = isImageUrlPath(url);
    return MediaLink.Imgur.create(contentURI.toString(), canUseRedditOptimizedImageUrl);
  }

  /**
   * Gfycat uses different type URL structures. This method converts these:
   * <p>
   * https://giant.gfycat.com/MessySpryAfricancivet.gif
   * https://thumbs.gfycat.com/MessySpryAfricancivet-size_restricted.gif
   * https://zippy.gfycat.com/MessySpryAfricancivet.webm
   * https://thumbs.gfycat.com/MessySpryAfricancivet-mobile.mp4
   * <p>
   * to this:
   * <p>
   * https://gfycat.com/MessySpryAfricancivet
   */
  private static Link createGfycatLink(Uri gfycatURI) {
    Matcher matcher = GFYCAT_ID_PATTERN.matcher(gfycatURI.getPath());
    if (matcher.matches()) {
      String gfycatThreeWordId = matcher.group(1);
      return MediaLink.Gfycat.create(gfycatURI.getScheme() + "://gfycat.com" + gfycatThreeWordId);

    } else {
      // Fallback.
      return Link.External.create(gfycatURI.toString());
    }
  }

  private static Link createGiphyLink(Uri giphyURI) {
    String url = giphyURI.toString();
    String urlPath = giphyURI.getPath();

    Matcher giphyIdMatcher = GIPHY_ID_PATTERN.matcher(urlPath);
    if (giphyIdMatcher.matches()) {
      String videoId = giphyIdMatcher.group(1);
      return MediaLink.Giphy.create(giphyURI.getScheme() + "://i.giphy.com/" + videoId + ".mp4");

    } else {
      // Fallback.
      return Link.External.create(url);
    }
  }

  private static Link createUnknownStreamableLink(Uri streamableUri) {
    String url = streamableUri.toString();

    Matcher streamableIdMatcher = STREAMABLE_ID_PATTERN.matcher(streamableUri.getPath());
    if (streamableIdMatcher.matches()) {
      String videoId = streamableIdMatcher.group(1);
      return MediaLink.StreamableUnknown.create(url, videoId);

    } else {
      // Fallback.
      return Link.External.create(url);
    }
  }

  static boolean isImgurAlbum(String urlPath) {
    return urlPath.startsWith("/gallery/") || urlPath.startsWith("/a/") || urlPath.startsWith("/t/");
  }

  private static boolean isUnsupportedImgurLink(String urlPath) {
    return urlPath.contains(",") || urlPath.startsWith("/g/");
  }

  private static boolean isImageUrlPath(String urlPath) {
    return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg") || isGifUrlPath(urlPath);
  }

  private static boolean isGifUrlPath(String urlPath) {
    return urlPath.endsWith(".gif");
  }

}
