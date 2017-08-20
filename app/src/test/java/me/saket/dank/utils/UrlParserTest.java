package me.saket.dank.utils;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;

import me.saket.dank.data.Link;
import me.saket.dank.data.MediaLink;
import me.saket.dank.data.RedditLink;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Uri.class, TextUtils.class, Html.class })
public class UrlParserTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Uri.class);

        PowerMockito.mockStatic(TextUtils.class);
        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence text = (CharSequence) invocation.getArguments()[0];
                return text == null || text.length() == 0;
            }
        });

        PowerMockito.mockStatic(Html.class);
        //noinspection deprecation
        PowerMockito.when(Html.fromHtml(any(String.class))).thenAnswer(new Answer<Spanned>() {
            @Override
            public Spanned answer(InvocationOnMock invocation) throws Throwable {
                return mock(SpannableString.class);
            }
        });
    }

    private static final String[] IMGUR_ALBUM_URLS = {
            "https://imgur.com/a/lBQGv",
            "http://imgur.com/a/RBpAe",
            "http://imgur.com/gallery/9Uq7u",
            "http://imgur.com/t/a_day_in_the_life/85Egn",
    };

// ======== REDDIT ======== //

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseRedditSubmission_whenUrlOnlyHasSubmissionId() {
        String url = "https://www.reddit.com/comments/656e5z";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
        assertEquals(((RedditLink.Submission) parsedLink).id, "656e5z");
        assertEquals(((RedditLink.Submission) parsedLink).subredditName, null);
        assertEquals(((RedditLink.Submission) parsedLink).initialComment, null);
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
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof RedditLink.Submission, true);
            assertEquals(((RedditLink.Submission) parsedLink).id, "60e610");
            assertEquals(((RedditLink.Submission) parsedLink).subredditName, "Cricket");
            assertEquals(((RedditLink.Submission) parsedLink).initialComment, null);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseRedditSubmission_whenUrlHasSubmissionId_andCommentId() {
        String url = "https://www.reddit.com/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
        assertEquals(((RedditLink.Submission) parsedLink).id, "5zm7tt");
        assertEquals(((RedditLink.Submission) parsedLink).subredditName, null);
        assertEquals(((RedditLink.Submission) parsedLink).initialComment.id, "dezzmre");
        assertEquals((int) ((RedditLink.Submission) parsedLink).initialComment.contextCount, 0);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseRedditSubmission_whenUrlHasSubredditName_andSubmissionId_andCommentId() {
        String url = "https://www.reddit.com/r/androiddev/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
        assertEquals(((RedditLink.Submission) parsedLink).id, "5zm7tt");
        assertEquals(((RedditLink.Submission) parsedLink).subredditName, "androiddev");
        assertEquals(((RedditLink.Submission) parsedLink).initialComment.id, "dezzmre");
        assertEquals((int) ((RedditLink.Submission) parsedLink).initialComment.contextCount, 0);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseRedditSubmission_whenUrlHasSubredditName_andSubmissionId_andCommentId_andCommentContext() {
        String url = "https://www.reddit.com/r/androiddev/comments/5zm7tt/is_anyone_using_services_nowadays/dezzmre/?context=100";
        Uri mockUri = createMockUriFor(url);
        when(mockUri.getQueryParameter("context")).thenReturn("100");
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
        assertEquals(((RedditLink.Submission) parsedLink).id, "5zm7tt");
        assertEquals(((RedditLink.Submission) parsedLink).subredditName, "androiddev");
        assertEquals(((RedditLink.Submission) parsedLink).initialComment.id, "dezzmre");
        assertEquals((int) ((RedditLink.Submission) parsedLink).initialComment.contextCount, 100);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseRedditSubmission_whenShortUrl() {
        String url = "https://redd.it/5524cd";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
        assertEquals(((RedditLink.Submission) parsedLink).id, "5524cd");
        assertEquals(((RedditLink.Submission) parsedLink).subredditName, null);
        assertEquals(((RedditLink.Submission) parsedLink).initialComment, null);
    }

    @Test
    public void parseLiveThreadUrl() {
        String url = "https://www.reddit.com/live/ysrfjcdc2lt1";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);
        assertEquals(parsedLink instanceof RedditLink.UnsupportedYet, true);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseSubredditUrl() {
        String url = "https://www.reddit.com/r/pics";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Subreddit, true);
        assertEquals(((RedditLink.Subreddit) parsedLink).name, "pics");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseUserUrl() {
        String[] urls = { "https://www.reddit.com/u/saketme", "https://www.reddit.com/u/saketme/" };
        for (String url : urls) {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof RedditLink.User, true);
            assertEquals(((RedditLink.User) parsedLink).name, "saketme");
        }
    }

    @Test
    public void parseAmpRedditUrls() {
        String url = "https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/";
        Uri mockAmpUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockAmpUri);

        Uri mockUri = createMockUriFor("https://amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/");
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof RedditLink.Submission, true);
    }

// ======== MEDIA URLs ======== //

    @Test
    public void parseRedditUploadedImages() {
        String[] urls = {
                "https://i.redd.it/ih32ovc92asy.png",
                "https://i.redd.it/jh0d5uf5asry.jpg",
                "https://i.redd.it/ge0nqqgjwrsy.jpg",
                "https://i.redd.it/60dgs56ws4ny.gif",
                "https://i.reddituploads.com/df0af5450dd14902a3056ec73db8fa64?fit=max&h=1536&w=1536&s=8fa077352b28b8a3e94fcd845cf7ca83"
        };

        for (String url : urls) {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof MediaLink, true);
            assertEquals(parsedLink.type(), Link.Type.IMAGE_OR_GIF);
        }
    }

    @Test
    public void parseImgurAlbumUrls() {
        for (String url : IMGUR_ALBUM_URLS) {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof MediaLink.ImgurUnresolvedGallery, true);
        }
    }

    @Test
    public void parseImgurImageUrls() {
        // Key: Imgur URLs to pass. Value: normalized URLs.
        Map<String, String> imgurUrlMap = new HashMap<>();
        imgurUrlMap.put("http://i.imgur.com/0Jp0l2R.jpg", "http://i.imgur.com/0Jp0l2R.jpg");
        imgurUrlMap.put("http://imgur.com/sEpUFzt", "http://i.imgur.com/sEpUFzt.jpg");

        imgurUrlMap.forEach((url, normalizedUrl) -> {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Uri mockNormalizedUri = createMockUriFor(normalizedUrl);
            PowerMockito.when(Uri.parse(normalizedUrl)).thenReturn(mockNormalizedUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof MediaLink.Imgur, true);
            assertEquals(parsedLink.type(), Link.Type.IMAGE_OR_GIF);
        });
    }

    @Test
    public void parseGiphyUrls() {
        String[] urls = {
                "https://media.giphy.com/media/l2JJyLbhqCF4va86c/giphy.mp4",
                "https://media.giphy.com/media/l2JJyLbhqCF4va86c/giphy.gif",
                "https://giphy.com/gifs/l2JJyLbhqCF4va86c/html5",
                "https://i.giphy.com/l2JJyLbhqCF4va86c.gif",
                "https://i.giphy.com/l2JJyLbhqCF4va86c.mp4"
        };

        for (String url : urls) {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof MediaLink.Giphy, true);
            assertEquals(parsedLink.type(), Link.Type.VIDEO);
        }
    }

    @Test
    public void parseGfycatUrls() {
        // Key: Gfycat URL to pass. Value: Normalized URLs.
        Map<String, String> gfycatUrlMap = new HashMap<>();
        gfycatUrlMap.put("https://giant.gfycat.com/MessySpryAfricancivet.gif", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("http://gfycat.com/MessySpryAfricancivet", "http://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://gfycat.com/MessySpryAfricancivet", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://thumbs.gfycat.com/MessySpryAfricancivet-size_restricted.gif", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://zippy.gfycat.com/MessySpryAfricancivet.webm", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://thumbs.gfycat.com/MessySpryAfricancivet-mobile.mp4", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://zippy.gfycat.com/MistySelfreliantFairybluebird.webm", "https://gfycat.com/MistySelfreliantFairybluebird");
        gfycatUrlMap.put("https://zippy.gfycat.com/CompetentRemoteBurro.webm", "https://gfycat.com/CompetentRemoteBurro");

        gfycatUrlMap.forEach((url, normalizedUrl) -> {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Uri mockNormalizedUri = createMockUriFor(normalizedUrl);
            PowerMockito.when(Uri.parse(normalizedUrl)).thenReturn(mockNormalizedUri);

            Link parsedLink = UrlParser.parse(url);

            assertEquals(parsedLink instanceof MediaLink.Gfycat, true);
            assertEquals(parsedLink.type(), Link.Type.VIDEO);
        });
    }

    @Test
    public void isImgurAlbum() throws Exception {
        for (String imgurAlbumUrl : IMGUR_ALBUM_URLS) {
            Uri mockUri = createMockUriFor(imgurAlbumUrl);

            boolean isImgurAlbum = UrlParser.isImgurAlbum(mockUri.getPath());

            assertEquals(isImgurAlbum, true);
        }
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void parseStreamableUrl() {
        String url = "https://streamable.com/jawcl";
        Uri mockUri = createMockUriFor(url);
        PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

        Link parsedLink = UrlParser.parse(url);

        assertEquals(parsedLink instanceof MediaLink.StreamableUnresolved, true);
        assertEquals(((MediaLink.StreamableUnresolved) parsedLink).videoId(), "jawcl");
        assertEquals(parsedLink.type(), Link.Type.VIDEO);
    }

    @NonNull
    private Uri createMockUriFor(String url) {
        Uri mockUri = mock(Uri.class);

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
        return mockUri;
    }

}
