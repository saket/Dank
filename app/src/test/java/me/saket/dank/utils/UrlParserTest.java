package me.saket.dank.utils;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Patterns;

import com.nytimes.android.external.cache3.CacheBuilder;

import net.dean.jraw.models.EmbeddedMedia;
import net.dean.jraw.models.Submission;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.saket.dank.urlparser.ExternalLink;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.GfycatUnresolvedLink;
import me.saket.dank.urlparser.GiphyLink;
import me.saket.dank.urlparser.ImgurAlbumUnresolvedLink;
import me.saket.dank.urlparser.ImgurLink;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditHostedVideoLink;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.urlparser.RedditUserLink;
import me.saket.dank.urlparser.StreamableUnresolvedLink;
import me.saket.dank.urlparser.UnresolvedMediaLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.urlparser.UrlParserConfig;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Uri.class, TextUtils.class, Html.class })
public class UrlParserTest {

  private static final String[] IMGUR_ALBUM_URLS = {
      "https://imgur.com/a/lBQGv",
      "http://imgur.com/a/RBpAe",
      "http://imgur.com/gallery/9Uq7u",
      "http://imgur.com/t/a_day_in_the_life/85Egn",
  };

  private UrlParser urlParser;

  @Before
  public void setUp() {
    urlParser = new UrlParser(CacheBuilder.newBuilder().build(), new UrlParserConfig());

    PowerMockito.mockStatic(Uri.class);

    PowerMockito.mockStatic(TextUtils.class);
    PowerMockito.when(TextUtils.isEmpty(or(any(CharSequence.class), isNull()))).thenAnswer(invocation -> {
      CharSequence text = invocation.getArgument(0);
      return text == null || text.length() == 0;
    });

    PowerMockito.mockStatic(Html.class);
    PowerMockito.when(Html.fromHtml(any(String.class))).thenAnswer(invocation -> {
      Spannable spannable = mock(Spannable.class);
      PowerMockito.when(spannable.toString()).thenReturn(invocation.getArgument(0));
      return spannable;
    });

    PowerMockito.when(Uri.parse(anyString())).thenAnswer(invocation -> {
      String url = invocation.getArgument(0);
      return createMockUriFor(url);
    });
  }

  @Test
  public void parseEmail() {
    String url = "saket@saket.me";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof ExternalLink, true);
    assertEquals(parsedLink.type(), Link.Type.EXTERNAL);
    assertEquals(parsedLink.unparsedUrl(), url);
  }

  @Test
  public void whenGivenGibberish_shouldParseAsAnExternalLink() {
    String url = "iturnedmyselfintoapickle";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof ExternalLink, true);
    assertEquals(parsedLink.type(), Link.Type.EXTERNAL);
    assertEquals(parsedLink.unparsedUrl(), url);
  }

// ======== REDDIT ======== //

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenUrlOnlyHasSubmissionId() {
    String url = "https://www.reddit.com/comments/656e5z";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof RedditSubmissionLink, true);
    assertEquals(((RedditSubmissionLink) parsedLink).id(), "656e5z");
    assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), null);
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment(), null);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenUrlHasSubredditName_andSubmissionId() {
    String[] urls = {
        "https://www.reddit.com/r/Cricket/comments/60e610/",
        "https://www.reddit.com/r/Cricket/comments/60e610",
        "https://www.reddit.com/r/Cricket/comments/60e610/match_thread_india_vs_australia_at_jsca/"
    };

    for (String url : urls) {
      Link parsedLink = urlParser.parse(url);

      assertEquals(parsedLink instanceof RedditSubmissionLink, true);
      assertEquals(((RedditSubmissionLink) parsedLink).id(), "60e610");
      assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), "Cricket");
      assertEquals(((RedditSubmissionLink) parsedLink).initialComment(), null);
    }
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenUrlHasSubmissionId_andCommentId() {
    String url = "https://www.reddit.com/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof RedditSubmissionLink, true);
    assertEquals(((RedditSubmissionLink) parsedLink).id(), "5zm7tt");
    assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), null);
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().id(), "dezzmre");
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().contextCount(), 0);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenUrlHasSubredditName_andSubmissionId_andCommentId() {
    String url = "https://www.reddit.com/r/androiddev/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof RedditSubmissionLink, true);
    assertEquals(((RedditSubmissionLink) parsedLink).id(), "5zm7tt");
    assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), "androiddev");
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().id(), "dezzmre");
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().contextCount(), 0);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenUrlHasSubredditName_andSubmissionId_andCommentId_andCommentContext() {
    String url = "https://www.reddit.com/r/androiddev/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/?context=100";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof RedditSubmissionLink, true);
    assertEquals(((RedditSubmissionLink) parsedLink).id(), "5zm7tt");
    assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), "androiddev");
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().id(), "dezzmre");
    assertEquals(((RedditSubmissionLink) parsedLink).initialComment().contextCount(), 100);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseRedditSubmission_whenShortUrl() {
    String[] urls = { "https://redd.it/5524cd", "http://i.reddit.com/5524cd" };

    for (String url : urls) {
      Link parsedLink = urlParser.parse(url);

      assertEquals(parsedLink instanceof RedditSubmissionLink, true);
      assertEquals(((RedditSubmissionLink) parsedLink).id(), "5524cd");
      assertEquals(((RedditSubmissionLink) parsedLink).subredditName(), null);
      assertEquals(((RedditSubmissionLink) parsedLink).initialComment(), null);
    }
  }

  @Test
  public void parseLiveThreadUrl() {
    String url = "https://www.reddit.com/live/ysrfjcdc2lt1";

    Link parsedLink = urlParser.parse(url);
    assertEquals(parsedLink instanceof ExternalLink, true);
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseSubredditUrl() {
    String url = "https://www.reddit.com/r/pics";

    Link parsedLink = urlParser.parse(url);

    assertEquals(parsedLink instanceof RedditSubredditLink, true);
    assertEquals(((RedditSubredditLink) parsedLink).name(), "pics");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseUserUrl() {
    String[] urls = { "https://www.reddit.com/u/saketme", "https://www.reddit.com/u/saketme/" };
    for (String url : urls) {
      Link parsedLink = urlParser.parse(url);

      assertEquals(parsedLink instanceof RedditUserLink, true);
      assertEquals(((RedditUserLink) parsedLink).name(), "saketme");
    }
  }

  @Test
  public void parseAmpRedditUrls() {
    String[] urls = {
        "https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/",
        "https://amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/"
    };

    for (String url : urls) {
      Link parsedLink = urlParser.parse(url);
      assertEquals(true, parsedLink instanceof RedditSubmissionLink);
    }
  }

  @Test
  public void parseRedditUrlWithoutDomain() {
    String url = "/gold";
    Link parsedLink = urlParser.parse(url);
    assertEquals(true, parsedLink instanceof ExternalLink);
    assertEquals(parsedLink.unparsedUrl(), "https://reddit.com/gold");
  }

// ======== MEDIA URLs ======== //

  @Test
  public void parseRedditHostedImages() {
    String[] imageUrls = {
        "https://i.redd.it/ih32ovc92asy.png",
        "https://i.redd.it/jh0d5uf5asry.jpg",
        "https://i.redd.it/ge0nqqgjwrsy.jpg",
        "https://i.reddituploads.com/df0af5450dd14902a3056ec73db8fa64?fit=max&h=1536&w=1536&s=8fa077352b28b8a3e94fcd845cf7ca83"
    };

    String[] gifUrls = {
        "https://i.redd.it/60dgs56ws4ny.gif",
        "https://i.redd.it/ygp9vu6lo3c01.gif"
    };

    for (String url : imageUrls) {
      Link parsedLink = urlParser.parse(url);

      assertEquals(parsedLink instanceof MediaLink, true);
      assertEquals(parsedLink.type(), Link.Type.SINGLE_IMAGE);
    }

    for (String url : gifUrls) {
      Link parsedLink = urlParser.parse(url);

      assertEquals(parsedLink instanceof MediaLink, true);
      assertEquals(parsedLink.type(), Link.Type.SINGLE_GIF);
    }
  }

  @Test
  public void escapeRedditHostedMediaUrls() {
    String[] imageUrls = {
        "https://i.reddituploads.com/df0af5450dd14902a3056ec73db8fa64?fit=max&h=1536&w=1536&amp;s=8fa077352b28b8a3e94fcd845cf7ca83",
        "https://i.redditmedia.com/H672N4aueEuGzTypOO3Gqzlf3xw3P61tVssYu2ObxDM.jpg?w=640&amp;s=76edfb936f168c9c4dd3bc45a0c45509"
    };
    String[] expecteds = {
        "https://i.reddituploads.com/df0af5450dd14902a3056ec73db8fa64?fit=max&h=1536&w=1536&s=8fa077352b28b8a3e94fcd845cf7ca83",
        "https://i.redditmedia.com/H672N4aueEuGzTypOO3Gqzlf3xw3P61tVssYu2ObxDM.jpg?w=640&s=76edfb936f168c9c4dd3bc45a0c45509"
    };

    for (int i = 0; i < imageUrls.length; i++) {
      String imageUrl = imageUrls[i];
      Link parsedLink = urlParser.parse(imageUrl);
      assertEquals(true, parsedLink instanceof MediaLink);
      //noinspection ConstantConditions
      assertEquals(((MediaLink) parsedLink).highQualityUrl(), expecteds[i]);
    }
  }

  @Test
  public void parseRedditHostedVideos() {
    String[] videoUrls = {
        "https://v.redd.it/fjpqnd127wf01",
        "https://v.reddit.com/fjpqnd127wf01"
    };
    for (String videoUrl : videoUrls) {
      String dashPlaylistUrl = "https://v.redd.it/nwypmagtjvf01/DASHPlaylist.mpd";
      Submission submission = mock(Submission.class);
      EmbeddedMedia mockMedia = embeddedMedia(dashPlaylistUrl);
      when(submission.getEmbeddedMedia()).thenReturn(mockMedia);

      Link parsedLink = urlParser.parse(videoUrl, submission);
      assertEquals(true, parsedLink instanceof RedditHostedVideoLink);
      //noinspection ConstantConditions
      assertEquals(dashPlaylistUrl, ((RedditHostedVideoLink) parsedLink).highQualityUrl());
    }
  }

  private EmbeddedMedia embeddedMedia(String dashPlaylistUrl) {
    EmbeddedMedia embeddedMedia = mock(EmbeddedMedia.class);
    EmbeddedMedia.RedditVideo redditVideo = mock(EmbeddedMedia.RedditVideo.class);
    when(redditVideo.getDashUrl()).thenReturn(dashPlaylistUrl);
    when(redditVideo.getFallbackUrl()).thenReturn("https://v.redd.it/fjpqnd127wf01/DASH_4_8_M");
    when(embeddedMedia.getRedditVideo()).thenReturn(redditVideo);
    return embeddedMedia;
  }

  @Test
  public void parseRedditHostedVideosForCrossPostParent() {
    String[] videoUrls = {
        "https://v.redd.it/fjpqnd127wf01",
        "https://v.reddit.com/fjpqnd127wf01"
    };
    for (String videoUrl : videoUrls) {
      String dashPlaylistUrl = "https://v.redd.it/nwypmagtjvf01/DASHPlaylist.mpd";
      Submission submission = mock(Submission.class);
      EmbeddedMedia media = embeddedMedia(dashPlaylistUrl);
      when(submission.getEmbeddedMedia()).thenReturn(media);

      Link parsedLink = urlParser.parse(videoUrl, submission);
      assertEquals(true, parsedLink instanceof RedditHostedVideoLink);
      //noinspection ConstantConditions
      assertEquals(dashPlaylistUrl, ((RedditHostedVideoLink) parsedLink).highQualityUrl());
    }
  }

  @Test
  public void parseUnresolvedRedditHostedVideos() {
    String[] videoUrls = {
        "https://v.redd.it/fjpqnd127wf01",
        "https://v.reddit.com/fjpqnd127wf01"
    };
    for (String videoUrl : videoUrls) {
      Submission submission = mock(Submission.class);
      when(submission.getEmbeddedMedia()).thenReturn(null);

      Link parsedLink = urlParser.parse(videoUrl, submission);
      assertEquals(true, parsedLink instanceof ExternalLink);
    }
  }

  @Test
  public void parseImgurAlbumUrls() {
    for (String url : IMGUR_ALBUM_URLS) {
      try {
        Link parsedLink = urlParser.parse(url);
        assertEquals(parsedLink instanceof ImgurAlbumUnresolvedLink, true);

      } catch (Exception e) {
        throw new IllegalStateException("Exception for url: " + url, e);
      }
    }

    Link link = urlParser.parse("http://i.imgur.com/0Jp0l2R.jpg");
    assertEquals(false, link instanceof ImgurAlbumUnresolvedLink);
  }

  @Test
  public void parseImgurImageUrls() {
    // Key: Imgur URLs to pass. Value: normalized URLs.
    String[] imageUrls = {
        "http://i.imgur.com/0Jp0l2R.jpg",
        "https://imgur.com/sEpUFzt",
    };

    for (String url : imageUrls) {
      Link parsedLink = urlParser.parse(url);
      assertThat(parsedLink).isInstanceOf(ImgurLink.class);
      assertThat(parsedLink.type()).isEqualTo(Link.Type.SINGLE_IMAGE);

      assert parsedLink instanceof ImgurLink;
      assertThat(((ImgurLink) parsedLink).highQualityUrl()).startsWith("https://");
      assertThat(((ImgurLink) parsedLink).lowQualityUrl()).startsWith("https://");
    }

    Link parsedGifLink = urlParser.parse("https://i.imgur.com/cuPUfRY.gif");
    assertThat(parsedGifLink).isInstanceOf(ImgurLink.class);
    assertThat(parsedGifLink.type()).isEqualTo(Link.Type.SINGLE_VIDEO);

    // Redirects to a GIF, but Dank will recognize it as a static image.
    // Glide will eventually load a GIF though.
    Link parsedGifLinkWithoutExtension = urlParser.parse("https://imgur.com/a/qU24g");
    assertThat(parsedGifLinkWithoutExtension).isInstanceOf(ImgurAlbumUnresolvedLink.class);
  }

  @Test
  public void parseGiphyUrls() {
    String[] urls = {
        "http://media.giphy.com/media/l2JJyLbhqCF4va86c/giphy.mp4",
        "http://media.giphy.com/media/l2JJyLbhqCF4va86c/giphy.gif",
        "http://giphy.com/gifs/l2JJyLbhqCF4va86c/html5",
        "http://i.giphy.com/l2JJyLbhqCF4va86c.gif",
        "https://i.giphy.com/l2JJyLbhqCF4va86c.mp4"
    };

    for (String url : urls) {
      Link parsedLink = urlParser.parse(url);

      assertThat(parsedLink).isInstanceOf(GiphyLink.class);
      assertThat(parsedLink.type()).isEqualTo(Link.Type.SINGLE_VIDEO);
      assertThat(((GiphyLink) parsedLink).highQualityUrl()).startsWith("https://");
      assertThat(((GiphyLink) parsedLink).lowQualityUrl()).startsWith("https://");
    }
  }

  @Test
  public void parseGfycatUrls() {
    // Key: Gfycat URL to pass. Value: Normalized URLs.
    Map<String, String> gfycatUrlMap = new HashMap<>();
    gfycatUrlMap.put("https://giant.gfycat.com/MessySpryAfricancivet.gif", "https://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("http://gfycat.com/MessySpryAfricancivet", "http://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("https://gfycat.com/MessySpryAfricancivet", "https://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("http://thumbs.gfycat.com/MessySpryAfricancivet-size_restricted.gif", "https://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("http://zippy.gfycat.com/MessySpryAfricancivet.webm", "https://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("http://thumbs.gfycat.com/MessySpryAfricancivet-mobile.mp4", "https://gfycat.com/MessySpryAfricancivet");
    gfycatUrlMap.put("https://zippy.gfycat.com/MistySelfreliantFairybluebird.webm", "https://gfycat.com/MistySelfreliantFairybluebird");
    gfycatUrlMap.put("https://zippy.gfycat.com/CompetentRemoteBurro.webm", "https://gfycat.com/CompetentRemoteBurro");

    gfycatUrlMap.forEach((url, normalizedUrl) -> {
      Link parsedLink = urlParser.parse(url);

      assertThat(parsedLink).isInstanceOf(GfycatLink.class);
      assertThat(parsedLink.type()).isEqualTo(Link.Type.SINGLE_VIDEO);
      assertThat(((GfycatLink) parsedLink).highQualityUrl()).startsWith("https://");
      assertThat(((GfycatLink) parsedLink).lowQualityUrl()).startsWith("https://");
    });

    List<String> urlsWithoutCapitalWords = gfycatUrlMap.keySet()
        .stream()
        .map(url -> url.toLowerCase(Locale.ENGLISH))
        .collect(Collectors.toList());

    urlsWithoutCapitalWords.forEach(url -> {
      Link parsedLink = urlParser.parse(url);

      assertThat(parsedLink).isInstanceOf(GfycatUnresolvedLink.class);
      assertThat(parsedLink).isInstanceOf(UnresolvedMediaLink.class);

      String path = Uri.parse(url).getPath().substring(1);
      assertThat(path).startsWith(((GfycatUnresolvedLink) parsedLink).threeWordId());
    });
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void parseStreamableUrl() {
    String url = "http://streamable.com/jawcl";

    Link parsedLink = urlParser.parse(url);

    assertThat(parsedLink).isInstanceOf(StreamableUnresolvedLink.class);
    assertThat(parsedLink.type()).isEqualTo(Link.Type.SINGLE_VIDEO);
    assertThat(((StreamableUnresolvedLink) parsedLink).videoId()).isEqualTo("jawcl");
  }

  // TODO: Extract this into an @Rule.
  public static Uri createMockUriFor(String url) {
    Uri mockUri = mock(Uri.class);

    if (PatternsCopy.WEB_URL.matcher(url).matches()) {
      // Really hacky way, but couldn't think of a better way.
      String domainTld;
      if (url.contains(".com/")) {
        domainTld = ".com";
      } else if (url.contains(".it/")) {
        domainTld = ".it";
      } else {
        throw new UnsupportedOperationException("Unknown tld");
      }

      when(mockUri.getPath()).thenReturn(url.substring(url.indexOf(domainTld) + domainTld.length()));
      when(mockUri.getHost()).thenReturn(url.substring(url.indexOf("://") + "://".length(), url.indexOf(domainTld) + domainTld.length()));
      when(mockUri.getScheme()).thenReturn(url.substring(0, url.indexOf("://")));
      when(mockUri.toString()).thenReturn(url);

      Map<String, String> queryAndArgs = new HashMap<>();

      if (url.contains("?")) {
        String[] queryAndArgParams = url.substring(url.indexOf("?") + 1).split("&");
        for (String queryAndArgParam : queryAndArgParams) {
          String[] queryAndArg = queryAndArgParam.split("=");
          queryAndArgs.put(queryAndArg[0], queryAndArg[1]);
        }
        when(mockUri.getQueryParameter(anyString())).thenAnswer(invocation -> {
          String parameter = invocation.getArgument(0);
          return queryAndArgs.get(parameter);
        });
      }
    }
    return mockUri;
  }

  /**
   * Copied from {@link Patterns} for test.
   */
  private static class PatternsCopy {
    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
        "\u00A0-\uD7FF" +
        "\uF900-\uFDCF" +
        "\uFDF0-\uFFEF" +
        "\uD800\uDC00-\uD83F\uDFFD" +
        "\uD840\uDC00-\uD87F\uDFFD" +
        "\uD880\uDC00-\uD8BF\uDFFD" +
        "\uD8C0\uDC00-\uD8FF\uDFFD" +
        "\uD900\uDC00-\uD93F\uDFFD" +
        "\uD940\uDC00-\uD97F\uDFFD" +
        "\uD980\uDC00-\uD9BF\uDFFD" +
        "\uD9C0\uDC00-\uD9FF\uDFFD" +
        "\uDA00\uDC00-\uDA3F\uDFFD" +
        "\uDA40\uDC00-\uDA7F\uDFFD" +
        "\uDA80\uDC00-\uDABF\uDFFD" +
        "\uDAC0\uDC00-\uDAFF\uDFFD" +
        "\uDB00\uDC00-\uDB3F\uDFFD" +
        "\uDB44\uDC00-\uDB7F\uDFFD" +
        "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;
    private static final String IRI_LABEL =
        "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "_\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";
    private static final String PUNYCODE_TLD = "xn\\-\\-[\\w\\-]{0,58}\\w";
    private static final String TLD_CHAR = "a-zA-Z" + UCS_CHAR;
    private static final String TLD = "(" + PUNYCODE_TLD + "|" + "[" + TLD_CHAR + "]{2,63}" + ")";
    private static final String HOST_NAME = "(" + IRI_LABEL + "\\.)+" + TLD;
    private static final String IP_ADDRESS_STRING =
        "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))";
    private static final String PROTOCOL = "(?i:http|https|rtsp)://";
    private static final String USER_INFO = "(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
        + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
        + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@";
    private static final String DOMAIN_NAME_STR = "(" + HOST_NAME + "|" + IP_ADDRESS_STRING + ")";
    private static final String PATH_AND_QUERY = "[/\\?](?:(?:[" + LABEL_CHAR
        + ";/\\?:@&=#~"  // plus optional query params
        + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";
    /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";
    private static final String PORT_NUMBER = "\\:\\d{1,5}";

    /**
     * Regular expression pattern to match most part of RFC 3987
     * Internationalized URLs, aka IRIs.
     */
    public static final Pattern WEB_URL = Pattern.compile("("
        + "("
        + "(?:" + PROTOCOL + "(?:" + USER_INFO + ")?" + ")?"
        + "(?:" + DOMAIN_NAME_STR + ")"
        + "(?:" + PORT_NUMBER + ")?"
        + ")"
        + "(" + PATH_AND_QUERY + ")?"
        + WORD_BOUNDARY
        + ")");
  }
}
