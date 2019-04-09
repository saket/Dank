package me.saket.dank.urlparser;

import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UrlParserConfig {

  /**
   * /r/$subreddit.
   */
  private static final String SUBREDDIT_PATTERN = "(?:^|[ ]+)\\/?r\\/([a-zA-Z0-9-_.]+)(\\/)*";
  private static final Pattern DEFAULT_BOUNDED_SUBREDDIT_PATTERN = Pattern.compile("^" + SUBREDDIT_PATTERN + "$");
  private static final Pattern DEFAULT_UNBOUNDED_SUBREDDIT_PATTERN = Pattern.compile(SUBREDDIT_PATTERN);

  /**
   * /u/$user.
   */
  private static final String USER_PATTERN = "(?:^|[ ]+)\\/?u(?:ser)?\\/([a-zA-Z0-9-_.]+)(?:\\/)*";
  private static final Pattern DEFAULT_BOUNDED_USER_PATTERN = Pattern.compile("^" + USER_PATTERN + "$");
  private static final Pattern DEFAULT_UNBOUNDED_USER_PATTERN = Pattern.compile(USER_PATTERN);

  /**
   * Submission: /r/$subreddit/comments/$post_id/post_title.
   * Comment:    /r/$subreddit/comments/$post_id/post_title/$comment_id.
   * <p>
   * ('post_title' and '/r/$subreddit/' can be empty).
   */
  private static final Pattern DEFAULT_SUBMISSION_OR_COMMENT_PATTERN = Pattern.compile("^(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)(/\\w*/(\\w*))?.*");

  /**
   * /live/$thread_id.
   */
  private static final Pattern DEFAULT_LIVE_THREAD_PATTERN = Pattern.compile("^/live/\\w*(/)*$");

  /**
   * Extracts the three-word name of a gfycat until a '.' or '-' is encountered. Example URLs:
   * <p>
   * /MessySpryAfricancivet
   * /MessySpryAfricancivet.gif
   * /MessySpryAfricancivet-size_restricted.gif
   * /MessySpryAfricancivet.webm
   * /MessySpryAfricancivet-mobile.mp4
   */
  private static final Pattern DEFAULT_GFYCAT_ID_PATTERN_2 = Pattern.compile("^/([^-.]*).*$");

  /**
   * Extracts the ID of a giphy link. In these examples, the ID is 'l2JJyLbhqCF4va86c
   * <p>
   * /media/l2JJyLbhqCF4va86c/giphy.mp4
   * /media/l2JJyLbhqCF4va86c/giphy.gif
   * /gifs/l2JJyLbhqCF4va86c/html5
   * /l2JJyLbhqCF4va86c.gif
   */
  private static final Pattern DEFAULT_GIPHY_ID_PATTERN = Pattern.compile("^/(?:(?:media)?(?:gifs)?/)?(\\w*)[/.].*$");

  /**
   * Extracts the ID of a streamable link. Eg., https://streamable.com/fxn88 -> 'fxn88'.
   */
  private static final Pattern DEFAULT_STREAMABLE_ID_PATTERN = Pattern.compile("/(\\w*\\d*)[^/.?]*");

  /**
   * Extracts the ID of an Imgur album.
   * <p>
   * /gallery/9Uq7u
   * /gallery/coZb0HC
   * /t/a_day_in_the_life/85Egn
   * /a/RBpAe
   */
  private static final Pattern DEFAULT_IMGUR_ALBUM_PATTERN = Pattern.compile("/(?:gallery)?(?:a)?(?:t/\\w*)?/(\\w*).*");

  @Inject
  public UrlParserConfig() {
  }

  public Pattern userPattern() {
    return DEFAULT_BOUNDED_USER_PATTERN;
  }

  public Pattern autoLinkUserPattern() {
    return DEFAULT_UNBOUNDED_USER_PATTERN;
  }

  Pattern submissionOrCommentPattern() {
    return DEFAULT_SUBMISSION_OR_COMMENT_PATTERN;
  }

  Pattern liveThreadPattern() {
    return DEFAULT_LIVE_THREAD_PATTERN;
  }

  public Pattern subredditPattern() {
    return DEFAULT_BOUNDED_SUBREDDIT_PATTERN;
  }

  public Pattern autoLinkSubredditPattern() {
    return DEFAULT_UNBOUNDED_SUBREDDIT_PATTERN;
  }

  Pattern gfycatIdPattern() {
    return DEFAULT_GFYCAT_ID_PATTERN_2;
  }

  public String gfycatUnparsedUrlPlaceholder(String videoId) {
    return String.format("https://gfycat.com/%s", videoId);
  }

  String gfycatHighQualityUrlPlaceholder(String videoId) {
    return String.format("https://giant.gfycat.com/%s.webm", videoId);
  }

  String gfycatLowQualityUrlPlaceholder(String videoId) {
    return String.format("https://thumbs.gfycat.com/%s-mini.mp4", videoId);
  }

  Pattern giphyIdPattern() {
    return DEFAULT_GIPHY_ID_PATTERN;
  }

  Pattern streamableIdPattern() {
    return DEFAULT_STREAMABLE_ID_PATTERN;
  }

  Pattern imgurAlbumPattern() {
    return DEFAULT_IMGUR_ALBUM_PATTERN;
  }
}
