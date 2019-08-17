package me.saket.dank.urlparser;

import android.net.Uri;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.nytimes.android.external.cache3.Cache;

import net.dean.jraw.models.Submission;

import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.exceptions.Exceptions;
import me.saket.dank.BuildConfig;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Urls;
import okhttp3.HttpUrl;

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

  private final Cache<String, Link> cache;
  private final UrlParserConfig config;

  @Inject
  public UrlParser(@Named("url_parser") Cache<String, Link> cache, UrlParserConfig config) {
    this.cache = cache;
    this.config = config;
  }

  /**
   * Determine type of the url.
   *
   * @return null if the url couldn't be identified. A class implementing {@link Link} otherwise.
   */
  public Link parse(String url) {
    try {
      return cache.get(url, () -> parseInternal(url, Optional.empty()));
    } catch (ExecutionException e) {
      throw Exceptions.propagate(e);
    }
  }

  /**
   * Determine type of the url.
   *
   * @return null if the url couldn't be identified. A class implementing {@link Link} otherwise.
   */
  public Link parse(String url, Submission submission) {
    try {
      return cache.get(url, () -> parseInternal(url, Optional.of(submission)));
    } catch (ExecutionException e) {
      throw Exceptions.propagate(e);
    }
  }

  /**
   * Determine type of the url.
   *
   * @return null if the url couldn't be identified. A class implementing {@link Link} otherwise.
   */
  private Link parseInternal(String url, Optional<Submission> submission) {
    // TODO: Support "np" subdomain?
    // TODO: Support wiki pages.
    Link parsedLink;
    Uri linkURI = Uri.parse(url);
    String urlDomain = linkURI.getHost() != null ? linkURI.getHost() : "";
    String urlPath = linkURI.getPath() != null ? linkURI.getPath() : "";  // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

    Matcher subredditMatcher = config.subredditPattern().matcher(urlPath);
    if (subredditMatcher.matches()) {
      parsedLink = RedditSubredditLink.create(url, subredditMatcher.group(1));

    } else {
      Matcher userMatcher = config.userPattern().matcher(urlPath);
      if (userMatcher.matches()) {
        parsedLink = RedditUserLink.create(url, userMatcher.group(1));

      } else if (urlDomain.endsWith("reddit.com")) {
        Matcher submissionOrCommentMatcher = config.submissionOrCommentPattern().matcher(urlPath);
        if (submissionOrCommentMatcher.matches()) {
          String subredditName = submissionOrCommentMatcher.group(2);
          String submissionId = submissionOrCommentMatcher.group(3);
          String commentId = submissionOrCommentMatcher.group(5);

          if (TextUtils.isEmpty(commentId)) {
            parsedLink = RedditSubmissionLink.create(url, submissionId, subredditName);
          } else {
            String contextParamValue = linkURI.getQueryParameter(Reddit.CONTEXT_QUERY_PARAM);
            int contextCount = TextUtils.isEmpty(contextParamValue) ? 0 : Integer.parseInt(contextParamValue);
            RedditCommentLink initialComment = RedditCommentLink.create(url, commentId, contextCount);
            parsedLink = RedditSubmissionLink.createWithComment(url, submissionId, subredditName, initialComment);
          }

        } else if (urlDomain.contains("i.reddit.com")) {
          // Old mobile website that nobody uses anymore. Format: i.reddit.com/post_id. Eg., https://i.reddit.com/5524cd
          String submissionId = urlPath.substring(1);  // Remove the leading slash.
          parsedLink = RedditSubmissionLink.create(url, submissionId, null);
        } else {
          Optional<String> urlSubdomain = Urls.subdomain(linkURI);
          if (urlSubdomain.isPresent() && urlSubdomain.get().equals("v")) {
            // TODO: When submission optional isn't present, treat it as an unresolved reddit video link.
            parsedLink = createRedditHostedVideoLink(url, submission);
          } else {
            parsedLink = ExternalLink.create(url);
          }
        }

      } else if (urlDomain.endsWith("redd.it")) {
        Optional<String> urlSubdomain = Urls.subdomain(linkURI);
        if (urlSubdomain.isPresent() && urlSubdomain.get().equals("v")) {
          parsedLink = createRedditHostedVideoLink(url, submission);

        } else if ((urlSubdomain.isEmpty() || urlSubdomain.get().equals("i")) // i.redd.it
            && (!isImageOrGifUrlPath(urlPath) && !isVideoPath(urlPath)))
        {
          // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
          String submissionId = urlPath.substring(1);  // Remove the leading slash.
          parsedLink = RedditSubmissionLink.create(url, submissionId, null);

        } else {
          parsedLink = parseNonRedditUrl(url);
        }

      } else {
        if (urlDomain.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
          // Google AMP url.
          // https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
          String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
          parsedLink = parse(nonAmpUrl);

        } else if (urlDomain.isEmpty() && url.startsWith("/") && !url.contains("@")) {
          return parseInternal("https://reddit.com" + url, submission);

        } else {
          parsedLink = parseNonRedditUrl(url);
        }
      }
    }

    cache.put(url, parsedLink);
    return parsedLink;
  }

  private Link parseNonRedditUrl(String url) {
    Uri linkURI = Uri.parse(url);

    String urlDomain = linkURI.getHost() != null ? linkURI.getHost() : "";
    String urlPath = linkURI.getPath() != null ? linkURI.getPath() : "";

    if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
      if (isUnsupportedImgurLink(urlPath)) {
        // These are links that Imgur no longer uses so Dank does not expect them either.
        return ExternalLink.create(url);

      } else {
        Matcher albumUrlMatcher = config.imgurAlbumPattern().matcher(Uri.parse(url).getPath());
        // matches() is important or else groups don't get formed.
        if (albumUrlMatcher.matches()) {
          String albumId = albumUrlMatcher.group(1);
          // It's titled as unresolved because we don't know if the gallery
          // contains a single image or multiple images.
          return ImgurAlbumUnresolvedLink.create(url, albumId);

        } else {
          return createImgurLink(url, null, null);
        }
      }

    } else if (urlDomain.contains("gfycat.com")) {
      return createGfycatLink(linkURI);

    } else if (urlDomain.contains("giphy.com")) {
      return createGiphyLink(linkURI);

    } else if (urlDomain.contains("streamable.com")) {
      return createUnresolvedStreamableLink(linkURI);

    } else if (urlDomain.contains("reddituploads.com") || urlDomain.contains("redditmedia.com")) {
      // Reddit sends HTML-escaped URLs for reddituploads.com. Decode them again.
      //noinspection deprecation
      String htmlUnescapedUrl = org.jsoup.parser.Parser.unescapeEntities(url, true);
      return GenericMediaLink.create(htmlUnescapedUrl, Link.Type.SINGLE_IMAGE);

    } else if (isImageOrGifUrlPath(urlPath) || isVideoPath(urlPath)) {
      return GenericMediaLink.create(url, getMediaUrlType(urlPath));

    } else {
      return ExternalLink.create(url);
    }
  }

  private static Link createRedditHostedVideoLink(String url, Optional<Submission> optionalSubmission) {
    return optionalSubmission
        .flatMap(submission -> JrawUtils2.redditVideoDashPlaylistUrl(submission))
        .map(playlist -> RedditHostedVideoLink.create(url, playlist))
        // Or else, probably just a "v.redd.it" link to a submission.
        // TODO v2: treat this as an unresolved reddit video link.
        .orElse(ExternalLink.create(url));
  }

  private static Link.Type getMediaUrlType(String urlPath) {
    if (isVideoPath(urlPath)) {
      return Link.Type.SINGLE_VIDEO;
    } else if (isGifPath(urlPath)) {
      return Link.Type.SINGLE_GIF;
    } else if (isImagePath(urlPath)) {
      return Link.Type.SINGLE_IMAGE;
    } else {
      throw new AssertionError();
    }
  }

  @SuppressWarnings("ConstantConditions")
  public ImgurLink createImgurLink(String url, @Nullable String title, @Nullable String description) {
    // Convert GIFs to MP4s that are insanely light weight in size.
    String[] gifFormats = new String[] { ".gif", ".gifv" };
    for (String gifFormat : gifFormats) {
      if (url.endsWith(gifFormat)) {
        url = url.substring(0, url.length() - gifFormat.length()) + ".mp4";
      }
    }

    HttpUrl imageUrl = HttpUrl.parse(url)
        .newBuilder()
        .scheme("https")
        .build();

    // Attempt to get direct links to images from Imgur submissions.
    // For example, convert 'http://imgur.com/djP1IZC' to 'http://i.imgur.com/djP1IZC.jpg'.
    if (!isImageOrGifUrlPath(url) && !url.endsWith("mp4")) {
      // If this happened to be a GIF submission, the user sadly will be forced to see it instead of its GIFV.
      imageUrl = imageUrl.newBuilder(imageUrl.encodedPath() + ".jpg")
          .host("i.imgur.com")
          .build();
    }

    //noinspection ConstantConditions
    Link.Type urlType = getMediaUrlType(imageUrl.encodedPath());
    return ImgurLink.create(url, urlType, title, description, imageUrl.toString());
  }

  /**
   * Gfycat uses different type URL structures. This method recognizes these:
   * <p>
   * https://giant.gfycat.com/MessySpryAfricancivet.gif
   * https://thumbs.gfycat.com/MessySpryAfricancivet-size_restricted.gif
   * https://zippy.gfycat.com/MessySpryAfricancivet.webm
   * https://thumbs.gfycat.com/MessySpryAfricancivet-mobile.mp4
   * <p>
   * as this:
   * <p>
   * https://gfycat.com/MessySpryAfricancivet
   * <p>
   * Links not containing three capital letters are converted to {@link GfycatUnresolvedLink}.
   */
  private Link createGfycatLink(Uri gfycatURI) {
    Matcher matcher = config.gfycatIdPattern().matcher(gfycatURI.getPath());
    if (matcher.matches()) {
      String threeWordId = matcher.group(1);
      String url = config.gfycatUnparsedUrlPlaceholder(threeWordId);

      int capitalLetterCount = 0;
      for (int i = 0; i < threeWordId.length(); i++) {
        if (Character.isUpperCase(threeWordId.charAt(i))) {
          ++capitalLetterCount;
        }
      }

      if (capitalLetterCount == 3) {
        String highQualityVideoUrl = config.gfycatHighQualityUrlPlaceholder(threeWordId);
        String lowQualityVideoUrl = config.gfycatLowQualityUrlPlaceholder(threeWordId);
        return GfycatLink.create(url, threeWordId, highQualityVideoUrl, lowQualityVideoUrl);

      } else {
        return GfycatUnresolvedLink.create(url, threeWordId);
      }

    } else {
      // Fallback.
      return ExternalLink.create(gfycatURI.toString());
    }
  }

  @SuppressWarnings("ConstantConditions")
  private Link createGiphyLink(Uri giphyURI) {
    String url = giphyURI.toString();

    HttpUrl httpUrl = HttpUrl.parse(giphyURI.toString());
    String urlPath = httpUrl.encodedPath();

    Matcher giphyIdMatcher = config.giphyIdPattern().matcher(urlPath);
    if (giphyIdMatcher.matches()) {
      String videoId = giphyIdMatcher.group(1);
      String gifVideoUrl = giphyURI.getScheme() + "://i.giphy.com/" + videoId + ".mp4";

      HttpUrl giphyUrl = httpUrl.newBuilder(urlPath + ".mp4")
          .scheme("https")
          .host("i.giphy.com")
          .build();
      return GiphyLink.create(url, giphyUrl.toString());

    } else {
      // Fallback.
      return ExternalLink.create(url);
    }
  }

  private Link createUnresolvedStreamableLink(Uri streamableUri) {
    String url = streamableUri.toString();

    Matcher streamableIdMatcher = config.streamableIdPattern().matcher(streamableUri.getPath());
    if (streamableIdMatcher.matches()) {
      String videoId = streamableIdMatcher.group(1);
      return StreamableUnresolvedLink.create(url, videoId);

    } else {
      // Fallback.
      return ExternalLink.create(url);
    }
  }

  private static boolean isUnsupportedImgurLink(String urlPath) {
    return urlPath.contains(",") || urlPath.startsWith("/g/");
  }

  public static boolean isImagePath(String urlPath) {
    return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg");
  }

  private static boolean isImageOrGifUrlPath(String urlPath) {
    return isImagePath(urlPath) || isGifPath(urlPath);
  }

  private static boolean isGifPath(String urlPath) {
    return urlPath.endsWith(".gif");
  }

  public static boolean isGifUrl(String url) {
    String path = Uri.parse(url).getPath();
    return path != null && isGifPath(path);
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

  public void clearCache() {
    if (!BuildConfig.DEBUG) {
      throw new AssertionError();
    }
    cache.invalidateAll();
  }
}
