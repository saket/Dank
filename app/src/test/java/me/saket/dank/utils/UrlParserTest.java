package me.saket.dank.utils;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
@PrepareForTest({ Uri.class, TextUtils.class })
public class UrlParserTest {

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Uri.class);
        PowerMockito.mockStatic(TextUtils.class);

        PowerMockito.when(TextUtils.isEmpty(any(CharSequence.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                return a == null || a.length() == 0;
            }
        });
    }

    private static final String[] IMGUR_ALBUM_URLS = {
            "https://imgur.com/a/lBQGv",
            "http://imgur.com/a/RBpAe",
            "http://imgur.com/gallery/9Uq7u",
            "http://imgur.com/t/a_day_in_the_life/85Egn",
    };

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

// ======== MEDIA URLs ======== //

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
        }
    }

    @Test
    public void parseGfycatUrls() {
        // Key: Gfycat URL to pass. Value: Same URL, but without the sub-domain, file format and "-size-restricted" label.
        Map<String, String> gfycatUrlMap = new HashMap<>();
        gfycatUrlMap.put("https://giant.gfycat.com/MessySpryAfricancivet.gif", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("http://gfycat.com/MessySpryAfricancivet", "http://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://gfycat.com/MessySpryAfricancivet", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://thumbs.gfycat.com/MessySpryAfricancivet-size_restricted.gif", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://zippy.gfycat.com/MessySpryAfricancivet.webm", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://thumbs.gfycat.com/MessySpryAfricancivet-mobile.mp4", "https://gfycat.com/MessySpryAfricancivet");
        gfycatUrlMap.put("https://zippy.gfycat.com/MistySelfreliantFairybluebird.webm", "https://gfycat.com/MistySelfreliantFairybluebird");
        gfycatUrlMap.put("https://zippy.gfycat.com/CompetentRemoteBurro.webm", "https://gfycat.com/CompetentRemoteBurro");

        gfycatUrlMap.forEach((url, urlWithoutSubDomainAndFileFormat) -> {
            Uri mockUri = createMockUriFor(url);
            PowerMockito.when(Uri.parse(url)).thenReturn(mockUri);

            Uri mockUriWithoutSubDomainAndFileFormat = createMockUriFor(urlWithoutSubDomainAndFileFormat);
            PowerMockito.when(Uri.parse(urlWithoutSubDomainAndFileFormat)).thenReturn(mockUriWithoutSubDomainAndFileFormat);

            Link parsedLink = UrlParser.parse(url);
            assertEquals(parsedLink instanceof MediaLink.Gfycat, true);
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

    @NonNull
    private Uri createMockUriFor(String url) {
        Uri mockUri = Mockito.mock(Uri.class);
        when(mockUri.getPath()).thenReturn(url.substring(url.indexOf(".com") + ".com".length()));
        when(mockUri.getHost()).thenReturn(url.substring(url.indexOf("://") + "://".length(), url.indexOf(".com") + ".com".length()));
        when(mockUri.getScheme()).thenReturn(url.substring(0, url.indexOf("://")));
        return mockUri;
    }

}
