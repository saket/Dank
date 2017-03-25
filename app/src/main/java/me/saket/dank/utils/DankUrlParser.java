package me.saket.dank.utils;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import net.dean.jraw.models.Submission;

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
public class DankUrlParser {

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
    private static final Pattern SUBMISSION_OR_COMMENT_PATTERN = Pattern.compile("^(/r/([a-zA-Z0-9-_.]+))*/comments/(\\w+)/\\w*/(\\w*).*");

    /**
     * /live/$thread_id.
     */
    private static final Pattern LIVE_THREAD_PATTERN = Pattern.compile("^/live/\\w*(/)*$");

    /**
     * Determine type of the url.
     *
     * @return null if the url couldn't be identified. A class implementing {@link RedditLink} otherwise.
     */
    @NonNull
    public static Link parse(String url) {
        // TODO: Should we support "np" subdomain?
        // TODO: Support wiki pages.

        Uri linkUri = Uri.parse(url);
        String urlHost = linkUri.getHost() != null ? linkUri.getHost() : "";
        String urlPath = linkUri.getPath() != null ? linkUri.getPath() : "";

        if (urlHost.endsWith("reddit.com")) {
            Matcher submissionOrCommentMatcher = SUBMISSION_OR_COMMENT_PATTERN.matcher(urlPath);
            if (submissionOrCommentMatcher.matches()) {
                String subredditName = submissionOrCommentMatcher.group(2);
                String submissionId = submissionOrCommentMatcher.group(3);
                String commentId = submissionOrCommentMatcher.group(4);

                if (commentId.isEmpty()) {
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

        } else if (urlHost.isEmpty()) {
            Matcher subredditMatcher = SUBREDDIT_PATTERN.matcher(urlPath);
            if (subredditMatcher.matches()) {
                return RedditLink.Subreddit.create(subredditMatcher.group(1));
            }

            Matcher userMatcher = USER_PATTERN.matcher(urlPath);
            if (userMatcher.matches()) {
                return RedditLink.User.create(userMatcher.group(1));
            }

        } else if (urlHost.endsWith("redd.it") && !isImageUrlPath(urlPath)) {
            // Short redd.it url. Format: redd.it/post_id. Eg., https://redd.it/5524cd
            return RedditLink.Submission.create(url, urlPath, null);

        } else if (urlHost.contains("google") && urlPath.startsWith("/amp/s/amp.reddit.com")) {
            // Google AMP url.
            // https://www.google.com/amp/s/amp.reddit.com/r/NoStupidQuestions/comments/2qwyo7/what_is_red_velvet_supposed_to_taste_like/
            String nonAmpUrl = "https://" + url.substring(url.indexOf("/amp/s/") + "/amp/s/".length());
            return parse(nonAmpUrl);
        }

        return parseNonRedditUrl(url);
    }

    public static Link parse(Submission submission) {
        Link parsedLink = parse(submission.getUrl());
        if (parsedLink instanceof MediaLink) {
            ((MediaLink) parsedLink).setRedditSuppliedImages(submission.getThumbnails());
        }
        return parsedLink;
    }

    @NonNull
    private static Link parseNonRedditUrl(String url) {
        Uri contentURI = Uri.parse(url);
        String urlDomain = contentURI.getHost();
        String urlPath = contentURI.getPath();    // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

        if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
            return createImgurLink(url);

        } else if (urlDomain.contains("gfycat.com")) {
            return MediaLink.Gfycat.create(url);

        } else if ((urlDomain.contains("reddituploads.com"))) {
            // Reddit sends HTML-escaped URLs. Decode them again.
            //noinspection deprecation
            String htmlUnescapedUrl = Html.fromHtml(url).toString();
            return MediaLink.create(htmlUnescapedUrl, true, Link.Type.IMAGE_OR_GIF);

        } else if (isImageUrlPath(urlPath)) {
            return MediaLink.create(url, true, Link.Type.IMAGE_OR_GIF);

        } else if (urlPath.endsWith(".mp4")) {
            // TODO: 19/02/17 Can we display .webm?
            return MediaLink.create(url, true, Link.Type.VIDEO);

        } else {
            return Link.External.create(url);
        }
    }

    private static MediaLink.Imgur createImgurLink(String url) {
        // Convert GIFs to MP4s that are insanely light weight in size.
        if (url.endsWith(".gif")) {
            url += "v";
        }
        Uri contentURI = Uri.parse(url);

        // Attempt to get direct links to images from Imgur submissions.
        // For example, convert 'http://imgur.com/djP1IZC' to 'http://i.imgur.com/djP1IZC.jpg'.
        if (!isImageUrlPath(url) && !url.endsWith("gifv")) {
            // If this happened to be a GIF submission, the user sadly will be forced to see it
            // instead of its GIFV.
            contentURI = Uri.parse(contentURI.getScheme() + "://i.imgur.com" + contentURI.getPath() + ".jpg");
        }

        // Reddit provides its own copies for the content in multiple sizes. Use that only in
        // case of images because otherwise it'll be a static image for GIFs or videos.
        boolean canUseRedditOptimizedImageUrl = isImageUrlPath(url);
        return MediaLink.Imgur.create(contentURI.toString(), canUseRedditOptimizedImageUrl);
    }

    private static boolean isImageUrlPath(String urlPath) {
        return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg") || urlPath.endsWith(".gif");
    }

}
