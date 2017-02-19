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
 *
 * TODO: Store thumbnails provided by Reddit to optimize on data.
 */
public class SubmissionContentParser {

    public static SubmissionContent parse(Submission submission) {
        switch (submission.getPostHint()) {
            case SELF:
                return SubmissionContent.create(Uri.parse(submission.getUrl()), Type.SELF, Host.REDDIT);

            case IMAGE:
                // This could also be a gif, where we might want to further modify the URL.
                // For example, Imgur .gif links can be converted to .gifv links.
                return manuallyParse(submission, Type.IMAGE);

            case VIDEO:
                return manuallyParse(submission, Type.VIDEO);

            case UNKNOWN:
                if (!TextUtils.isEmpty(submission.getSelftext())) {
                    // Why Reddit? :O
                    return SubmissionContent.create(Uri.parse(submission.getUrl()), Type.SELF, Host.REDDIT);

                } else {
                    // Treat everything else as links.
                    return manuallyParse(submission, Type.LINK);
                }

            case LINK:
                return manuallyParse(submission, Type.LINK);

            default:
                throw new UnsupportedOperationException("Unknown post-hint: " + submission.getPostHint());
        }
    }

    /**
     * @param typeSuppliedByReddit Hint supplied by Reddit.
     */
    private static SubmissionContent manuallyParse(Submission submission, Type typeSuppliedByReddit) {
        Uri contentUri = Uri.parse(submission.getUrl());
        String urlDomain = contentUri.getHost();
        String urlPath = contentUri.getPath();    // Path is the part of the URL without the domain. E.g.,: /something/image.jpg.

        if ((urlDomain.contains("imgur.com") || urlDomain.contains("bildgur.de"))) {
            return SubmissionContent.Imgur.create(contentUri, Host.IMGUR);

        } else if (urlDomain.contains("gfycat.com") && (typeSuppliedByReddit == Type.VIDEO || typeSuppliedByReddit == Type.IMAGE)) {
            return SubmissionContent.Gfycat.create(contentUri, Host.GFYCAT);

        } else if ((urlDomain.contains("reddituploads.com"))) {
            // Reddit sends HTML-escaped URLs. Decode them again.
            //noinspection deprecation
            contentUri = Uri.parse(Html.fromHtml(submission.getUrl()).toString());
            return SubmissionContent.create(contentUri, Type.IMAGE /* TODO cam this be video? */ , Host.REDDIT);

        } else if (urlDomain.contains("i.redd.it")) {
            return SubmissionContent.create(contentUri, typeSuppliedByReddit, Host.REDDIT);

        } else if (isImageUrlPath(urlPath)) {
            return SubmissionContent.create(contentUri, Type.IMAGE, Host.UNKNOWN);

        } else if (urlPath.endsWith(".mp4")) {
            // TODO: 19/02/17 Can we display .webm?
            return SubmissionContent.create(contentUri, Type.VIDEO, Host.UNKNOWN);

        } else {
            return SubmissionContent.create(contentUri, typeSuppliedByReddit, Host.UNKNOWN);
        }
    }

    public static boolean isImageUrlPath(String urlPath) {
        return urlPath.endsWith(".png") || urlPath.endsWith(".jpg") || urlPath.endsWith(".jpeg") || urlPath.endsWith(".gif");
    }

}
