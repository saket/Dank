package me.saket.dank.utils;

import android.net.Uri;
import android.support.v4.util.LruCache;
import android.text.Html;
import android.text.TextUtils;

import net.dean.jraw.models.Submission;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.saket.dank.data.links.ExternalLink;
import me.saket.dank.data.links.GenericMediaLink;
import me.saket.dank.data.links.GfycatLink;
import me.saket.dank.data.links.GiphyLink;
import me.saket.dank.data.links.ImgurAlbumUnresolvedLink;
import me.saket.dank.data.links.ImgurLink;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.RedditCommentLink;
import me.saket.dank.data.links.RedditSubmissionLink;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.data.links.RedditUserLink;
import me.saket.dank.data.links.StreamableUnresolvedLink;

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

  private static LruCache<String, Link> cache = new LruCache<>(100);

  /**
   * Determine type of the url.
   *
   * @return null if the url couldn't be identified. A class implementing {@link Link} otherwise.
   */
  public static Link parse(String url) {
    // TODO: Support "np" subdomain?
    // TODO: Support wiki pages.

    Link cachedLink = cache.get(url);
    if (cachedLink != null) {
      return cachedLink;
    }

    Link parsedLink;

    Uri linkURI = Uri.parse(url);
    String urlDomain = linkURI.getHost() != null ? linkURI.getHost() : "";
    String urlPath = linkURI.getPath() != null ? linkURI.getPath() : "";  // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

    Matcher subredditMatcher = SUBREDDIT_PATTERN.matcher(urlPath);
    if (subredditMatcher.matches()) {
      parsedLink = RedditSubredditLink.create(url, subredditMatcher.group(1));

    } else {
      Matcher userMatcher = USER_PATTERN.matcher(urlPath);
      if (userMatcher.matches()) {
        parsedLink = RedditUserLink.create(url, userMatcher.group(1));

      } else if (urlDomain.endsWith("reddit.com")) {
        Matcher submissionOrCommentMatcher = SUBMISSION_OR_COMMENT_PATTERN.matcher(urlPath);
        if (submissionOrCommentMatcher.matches()) {
          String subredditName = submissionOrCommentMatcher.group(2);
          String submissionId = submissionOrCommentMatcher.group(3);
          String commentId = submissionOrCommentMatcher.group(5);

          if (TextUtils.isEmpty(commentId)) {
            parsedLink = RedditSubmissionLink.create(url, submissionId, subredditName);

          } else {
            String contextParamValue = linkURI.getQueryParameter("context");
            int contextCount = TextUtils.isEmpty(contextParamValue) ? 0 : Integer.parseInt(contextParamValue);
            RedditCommentLink initialComment = RedditCommentLink.create(url, commentId, contextCount);
            parsedLink = RedditSubmissionLink.createWithComment(url, submissionId, subredditName, initialComment);
          }

        } else {
          //Matcher liveThreadMatcher = LIVE_THREAD_PATTERN.matcher(urlPath);
          //if (liveThreadMatcher.matches()) {
          //  parsedLink = ExternalLink.create(url);
          //}
          parsedLink = ExternalLink.create(url);
        }

      } else if (urlDomain.endsWith("redd.it") && !isImageOrGifUrlPath(urlPath) && !isVideoPath(urlPath)) {
        // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
        String submissionId = urlPath.substring(1);  // Remove the leading slash.
        parsedLink = RedditSubmissionLink.create(url, submissionId, null);

      } else if (urlDomain.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
        // Google AMP url.
        // https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
        String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
        parsedLink = parse(nonAmpUrl);

      } else {
        parsedLink = parseNonRedditUrl(url);
      }
    }

    cache.put(url, parsedLink);
    return parsedLink;
  }

  private static Link parseNonRedditUrl(String url) {
    Uri linkURI = Uri.parse(url);

    String urlDomain = linkURI.getHost() != null ? linkURI.getHost() : "";
    String urlPath = linkURI.getPath() != null ? linkURI.getPath() : "";

    if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
      if (isUnsupportedImgurLink(urlPath)) {
        // These are links that Imgur no longer uses so Dank does not expect them either.
        return ExternalLink.create(url);

      } else if (isImgurAlbum(urlPath)) {
        return createUnresolvedImgurAlbum(url);

      } else {
        return createImgurLink(url, null, null);
      }

    } else if (urlDomain.contains("gfycat.com")) {
      return createGfycatLink(linkURI);

    } else if (urlDomain.contains("giphy.com")) {
      return createGiphyLink(linkURI);

    } else if (urlDomain.contains("streamable.com")) {
      return createUnresolvedStreamableLink(linkURI);

    } else if ((urlDomain.contains("reddituploads.com"))) {
      // Reddit sends HTML-escaped URLs for reddituploads.com. Decode them again.
      //noinspection deprecation
      String htmlUnescapedUrl = Html.fromHtml(url).toString();
      return GenericMediaLink.create(htmlUnescapedUrl, Link.Type.SINGLE_IMAGE_OR_GIF);

    } else if (isImageOrGifUrlPath(urlPath)) {
      return GenericMediaLink.create(url, Link.Type.SINGLE_IMAGE_OR_GIF);

    } else if (isVideoPath(urlPath)) {
      return GenericMediaLink.create(url, Link.Type.SINGLE_VIDEO);

    } else {
      return ExternalLink.create(url);
    }
  }

  /**
   * It's titled as unresolved because we don't know if the gallery contains a single image or multiple images.
   */
  private static ImgurAlbumUnresolvedLink createUnresolvedImgurAlbum(String albumUrl) {
    Matcher albumUrlMatcher = IMGUR_ALBUM_PATTERN.matcher(Uri.parse(albumUrl).getPath());
    if (albumUrlMatcher.matches()) {  // matches() is important or else groups don't get formed.
      String albumId = albumUrlMatcher.group(1);
      return ImgurAlbumUnresolvedLink.create(albumUrl, albumId);
    } else {
      throw new IllegalStateException("Couldn't match regex. Album URL: " + albumUrl);
    }
  }

  public static ImgurLink createImgurLink(String url, String title, String description) {
    // Convert GIFs to MP4s that are insanely light weight in size.
    String[] gifFormats = new String[] { ".gif", ".gifv" };
    for (String gifFormat : gifFormats) {
      if (url.endsWith(gifFormat)) {
        url = url.substring(0, url.length() - gifFormat.length()) + ".mp4";
      }
    }

    Uri directLinkURI = Uri.parse(url);

    // Attempt to get direct links to images from Imgur submissions.
    // For example, convert 'http://imgur.com/djP1IZC' to 'http://i.imgur.com/djP1IZC.jpg'.
    if (!isImageOrGifUrlPath(url) && !url.endsWith("mp4")) {
      // If this happened to be a GIF submission, the user sadly will be forced to see it instead of its GIFV.
      directLinkURI = Uri.parse(directLinkURI.getScheme() + "://i.imgur.com" + directLinkURI.getPath() + ".jpg");
    }
    return ImgurLink.create(url, title, description, directLinkURI.toString());
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
      String url = gfycatURI.getScheme() + "://gfycat.com" + gfycatThreeWordId;
      String highQualityVideoUrl = gfycatURI.getScheme() + "://zippy.gfycat.com" + gfycatURI.getPath() + ".webm";
      String lowQualityVideoUrl = gfycatURI.getScheme() + "://thumbs.gfycat.com" + gfycatURI.getPath() + "-mobile.mp4";
      return GfycatLink.create(url, highQualityVideoUrl, lowQualityVideoUrl);

    } else {
      // Fallback.
      return ExternalLink.create(gfycatURI.toString());
    }
  }

  private static Link createGiphyLink(Uri giphyURI) {
    String url = giphyURI.toString();
    String urlPath = giphyURI.getPath();

    Matcher giphyIdMatcher = GIPHY_ID_PATTERN.matcher(urlPath);
    if (giphyIdMatcher.matches()) {
      String videoId = giphyIdMatcher.group(1);
      String gifVideoUrl = giphyURI.getScheme() + "://i.giphy.com/" + videoId + ".mp4";
      return GiphyLink.create(url, gifVideoUrl);

    } else {
      // Fallback.
      return ExternalLink.create(url);
    }
  }

  private static Link createUnresolvedStreamableLink(Uri streamableUri) {
    String url = streamableUri.toString();

    Matcher streamableIdMatcher = STREAMABLE_ID_PATTERN.matcher(streamableUri.getPath());
    if (streamableIdMatcher.matches()) {
      String videoId = streamableIdMatcher.group(1);
      return StreamableUnresolvedLink.create(url, videoId);

    } else {
      // Fallback.
      return ExternalLink.create(url);
    }
  }

  static boolean isImgurAlbum(String urlPath) {
    return IMGUR_ALBUM_PATTERN.matcher(urlPath).matches();
  }

  private static boolean isUnsupportedImgurLink(String urlPath) {
    return urlPath.contains(",") || urlPath.startsWith("/g/");
  }

  public static boolean isImagePath(String urlPath) {
    return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg");
  }

  private static boolean isImageOrGifUrlPath(String urlPath) {
    return isImagePath(urlPath) || isGifUrlPath(urlPath);
  }

  private static boolean isGifUrlPath(String urlPath) {
    return urlPath.endsWith(".gif");
  }

  private static boolean isVideoPath(String urlPath) {
    return urlPath.endsWith(".mp4") || urlPath.endsWith(".webm");
  }

  public static boolean isGooglePlayUrl(Uri URI) {
    return URI.getHost().endsWith("play.google.com") && URI.getPath().startsWith("/store");
  }

  public static boolean isGooglePlayUrl(String urlHost, String uriPath) {
    return urlHost.endsWith("play.google.com") && uriPath.startsWith("/store");
  }
}
