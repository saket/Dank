package me.saket.dank.utils;

import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;

import net.dean.jraw.models.Submission;

import me.saket.dank.data.SubmissionContent;
import me.saket.dank.data.SubmissionContent.Host;
import me.saket.dank.data.SubmissionContent.Type;

/**
 * Does the job of identifying URLs and mapping them to popular websites like imgur, giphy, etc.
 * This class exists because Reddit's {@link Submission#getPostHint()} is not very accurate and
 * fails to identify a lot of URLs. For instance, it returns {@link Submission.PostHint#LINK}
 * for its own image hosting domain, redditupload.com images. Use {@link #parse(Submission) to start}.
 */
public class SubmissionContentParser {

    public static SubmissionContent parse(Submission submission) {
        SubmissionContent content;

        switch (submission.getPostHint()) {
            case SELF:
                content = SubmissionContent.create(Uri.parse(submission.getUrl()), Type.SELF, Host.REDDIT);
                break;

            case IMAGE:
                // This could also be a gif, where we might want to further modify the URL.
                // For example, Imgur .gif links can be converted to .gifv links.
                content = manuallyParse(submission, Type.IMAGE);
                break;

            case VIDEO:
                content = manuallyParse(submission, Type.VIDEO);
                break;

            case UNKNOWN:
                if (!TextUtils.isEmpty(submission.getSelftext())) {
                    // Why Reddit? :O
                    content = SubmissionContent.create(Uri.parse(submission.getUrl()), Type.SELF, Host.REDDIT);
                    break;

                } else {
                    // Treat everything else as links.
                    content = manuallyParse(submission, Type.LINK);
                    break;
                }

            case LINK:
                content = manuallyParse(submission, Type.LINK);
                break;

            default:
                throw new UnsupportedOperationException("Unknown post-hint: " + submission.getPostHint());
        }

        content.setRedditSuppliedThumbnails(submission.getThumbnails());
        return content;
    }

    /**
     * @param typeSuppliedByReddit Hint supplied by Reddit.
     */
    private static SubmissionContent manuallyParse(Submission submission, Type typeSuppliedByReddit) {
        Uri contentURI = Uri.parse(submission.getUrl());
        String urlDomain = contentURI.getHost();
        String urlPath = contentURI.getPath();    // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

        if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
            return createImgurContent(submission);

        } else if (urlDomain.contains("gfycat.com") && (typeSuppliedByReddit == Type.VIDEO || typeSuppliedByReddit == Type.IMAGE)) {
            return SubmissionContent.Gfycat.create(contentURI, Host.GFYCAT);

        } else if ((urlDomain.contains("reddituploads.com"))) {
            // Reddit sends HTML-escaped URLs. Decode them again.
            //noinspection deprecation
            contentURI = Uri.parse(Html.fromHtml(submission.getUrl()).toString());
            return SubmissionContent.create(contentURI, Type.IMAGE /* TODO can this be video? */ , Host.REDDIT);

        } else if (urlDomain.contains("i.redd.it")) {
            return SubmissionContent.create(contentURI, typeSuppliedByReddit, Host.REDDIT);

        } else if (isImageUrlPath(urlPath)) {
            return SubmissionContent.create(contentURI, Type.IMAGE, Host.UNKNOWN);

        } else if (urlPath.endsWith(".mp4")) {
            // TODO: 19/02/17 Can we display .webm?
            return SubmissionContent.create(contentURI, Type.VIDEO, Host.UNKNOWN);

        } else {
            return SubmissionContent.create(contentURI, typeSuppliedByReddit, Host.UNKNOWN);
        }
    }

    private static SubmissionContent createImgurContent(Submission submission) {
        // Convert GIFs to MP4s that are insanely light weight in size.
        String contentUrl = submission.getUrl();
        if (contentUrl.endsWith(".gif")) {
            contentUrl += "v";
        }

        Type imgurContentType = contentUrl.endsWith("gifv") ? Type.VIDEO : Type.IMAGE;
        Uri contentURI = Uri.parse(contentUrl);

        // Attempt to get direct links to images from Imgur submissions.
        // For example, convert 'http://imgur.com/djP1IZC' to 'http://i.imgur.com/djP1IZC.jpg'.
        if (!isImageUrlPath(contentUrl) && !contentUrl.endsWith("gifv")) {
            // If this happened to be a GIF submission, the user sadly will be forced to see it
            // instead of its GIFV.
            contentURI = Uri.parse(contentURI.getScheme() + "://i.imgur.com" + contentURI.getPath() + ".jpg");
        }

        SubmissionContent.Imgur imgurContent = SubmissionContent.Imgur.create(contentURI, imgurContentType, Host.IMGUR);

        // Reddit provides its own copies for the content in multiple sizes. Use that only in
        // case of images because otherwise it'll be a static image for GIFs or videos.
        imgurContent.setCanUseRedditOptimizedImageUrl(isImageUrlPath(contentUrl) && imgurContentType == Type.IMAGE);

        return imgurContent;
    }

    public static boolean isImageUrlPath(String urlPath) {
        return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg") || urlPath.endsWith(".gif");
    }

}
