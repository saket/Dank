package me.saket.dank.walkthrough;

import java.util.Locale;

import javax.inject.Inject;

import me.saket.dank.data.DankRedditClient;

public class SyntheticData {

  public final String ID_SUBMISSION_FOR_GESTURE_WALKTHROUGH = "syntheticsubmissionforgesturewalkthrough";
  public final String SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH = "https://i.imgur.com/NaWfFWR.jpg";

  @Inject
  public SyntheticData() {
  }

  public String submissionForGesturesWalkthroughJson() {
    String defaultSortStringUppercase = DankRedditClient.DEFAULT_COMMENT_SORT.name();
    String defaultSortStringLowercase = DankRedditClient.DEFAULT_COMMENT_SORT.name().toLowerCase(Locale.ENGLISH);
    String subredditName = "GetDank";
    String submissionAuthorName = "Saketme";
    String submissionPermalink = "/r/" + subredditName;
    long createdTimeUtc = System.currentTimeMillis();

    String submissionTitle = "Here's a heart-warming photo to start your journey";

    String commentSubredditId = "t5_3kfea";
    String commentSubmissionId = "t3_8h801w";
    String commentAuthorName = "Dank";
    long commentCreatedTimeUtc = System.currentTimeMillis();

    String comment1 = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc.";
    String comment1Html = comment1;

    String comment2 = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc.";
    String comment2Html = comment2;

    String comment3 = "Comments (and their replies) can be collapsed by tapping on them.";
    String comment3Html = comment3;

    String comment4 = "Drag the cat's image downwards and release to close this tutorial.";
    String comment4Html = comment4;

    return "\"[{\\\"kind\\\":\\\" Listing\\\",\\\"data\\\":{\\\"children\\\":[{\\\"kind\\\":\\\"t3\\\",\\\"data\\\":{\\n" +
        "  \\\"approved_at_utc\\\" : null,\\n" +
        "  \\\"subreddit\\\" : \\\"" + subredditName + "\\\",\\n" +
        "  \\\"selftext\\\" : \\\"\\\",\\n" +
        "  \\\"user_reports\\\" : [ ],\\n" +
        "  \\\"saved\\\" : false,\\n" +
        "  \\\"mod_reason_title\\\" : null,\\n" +
        "  \\\"gilded\\\" : 0,\\n" +
        "  \\\"clicked\\\" : false,\\n" +
        "  \\\"title\\\" : \\\"" + submissionTitle + "\\\",\\n" +
        "  \\\"link_flair_richtext\\\" : [ ],\\n" +
        "  \\\"subreddit_name_prefixed\\\" : \\\"r/" + subredditName + "\\\",\\n" +
        "  \\\"hidden\\\" : false,\\n" +
        "  \\\"pwls\\\" : null,\\n" +
        "  \\\"link_flair_css_class\\\" : null,\\n" +
        "  \\\"downs\\\" : 0,\\n" +
        "  \\\"thumbnail_height\\\" : 140,\\n" +
        "  \\\"parent_whitelist_status\\\" : null,\\n" +
        "  \\\"hide_score\\\" : false,\\n" +
        "  \\\"name\\\" : \\\"t3_" + ID_SUBMISSION_FOR_GESTURE_WALKTHROUGH + "\\\",\\n" +
        "  \\\"quarantine\\\" : false,\\n" +
        "  \\\"link_flair_text_color\\\" : \\\"dark\\\",\\n" +
        "  \\\"ignore_reports\\\" : false,\\n" +
        "  \\\"subreddit_type\\\" : \\\"private\\\",\\n" +
        "  \\\"ups\\\" : 1,\\n" +
        "  \\\"domain\\\" : \\\"i.imgur.com\\\",\\n" +
        "  \\\"media_embed\\\" : { },\\n" +
        "  \\\"thumbnail_width\\\" : 140,\\n" +
        "  \\\"author_flair_template_id\\\" : null,\\n" +
        "  \\\"is_original_content\\\" : false,\\n" +
        "  \\\"secure_media\\\" : null,\\n" +
        "  \\\"is_reddit_media_domain\\\" : false,\\n" +
        "  \\\"category\\\" : null,\\n" +
        "  \\\"secure_media_embed\\\" : { },\\n" +
        "  \\\"link_flair_text\\\" : null,\\n" +
        "  \\\"can_mod_post\\\" : true,\\n" +
        "  \\\"score\\\" : 1,\\n" +
        "  \\\"approved_by\\\" : null,\\n" +
        "  \\\"author_flair_background_color\\\" : \\\"\\\",\\n" +
        "  \\\"thumbnail\\\" : \\\"https://b.thumbs.redditmedia.com/ztH5P-mWT63znn-I4dk3cwAqOZWB7m9c4ZVLJ5Wc8vg.jpg\\\",\\n" +
        "  \\\"edited\\\" : false,\\n" +
        "  \\\"author_flair_css_class\\\" : null,\\n" +
        "  \\\"author_flair_richtext\\\" : null,\\n" +
        "  \\\"post_hint\\\" : \\\"image\\\",\\n" +
        "  \\\"is_self\\\" : false,\\n" +
        "  \\\"mod_note\\\" : null,\\n" +
        "  \\\"created\\\" : 1.525054876E9,\\n" +
        "  \\\"link_flair_type\\\" : \\\"text\\\",\\n" +
        "  \\\"wls\\\" : null,\\n" +
        "  \\\"banned_by\\\" : null,\\n" +
        "  \\\"author_flair_type\\\" : \\\"richtext\\\",\\n" +
        "  \\\"contest_mode\\\" : false,\\n" +
        "  \\\"selftext_html\\\" : null,\\n" +
        "  \\\"likes\\\" : true,\\n" +
        "  \\\"suggested_sort\\\" : \\\"" + defaultSortStringLowercase + "\\\",\\n" +
        "  \\\"banned_at_utc\\\" : null,\\n" +
        "  \\\"view_count\\\" : 2,\\n" +
        "  \\\"archived\\\" : false,\\n" +
        "  \\\"no_follow\\\" : false,\\n" +
        "  \\\"spam\\\" : false,\\n" +
        "  \\\"is_crosspostable\\\" : false,\\n" +
        "  \\\"pinned\\\" : false,\\n" +
        "  \\\"over_18\\\" : false,\\n" +
        "  \\\"preview\\\" : {\\n" +
        "    \\\"images\\\" : [ {\\n" +
        "      \\\"source\\\" : {\\n" +
        "        \\\"url\\\" : \\\"" + SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH + "\\\",\\n" +
        "        \\\"width\\\" : 1429,\\n" +
        "        \\\"height\\\" : 2000\\n" +
        "      },\\n" +
        "      \\\"resolutions\\\" : null,\\n" +
        "      \\\"variants\\\" : { },\\n" +
        "      \\\"id\\\" : \\\"cis2Rd-BcILx_RarFlNdfCimvNx2g3sjO36vuuwW2p4\\\"\\n" +
        "    } ],\\n" +
        "    \\\"enabled\\\" : true\\n" +
        "  },\\n" +
        "  \\\"can_gild\\\" : false,\\n" +
        "  \\\"removed\\\" : false,\\n" +
        "  \\\"spoiler\\\" : false,\\n" +
        "  \\\"locked\\\" : false,\\n" +
        "  \\\"author_flair_text\\\" : null,\\n" +
        "  \\\"rte_mode\\\" : \\\"markdown\\\",\\n" +
        "  \\\"visited\\\" : false,\\n" +
        "  \\\"num_reports\\\" : 0,\\n" +
        "  \\\"distinguished\\\" : null,\\n" +
        "  \\\"subreddit_id\\\" : \\\"t5_32wow\\\",\\n" +
        "  \\\"mod_reason_by\\\" : null,\\n" +
        "  \\\"removal_reason\\\" : null,\\n" +
        "  \\\"id\\\" : \\\"" + ID_SUBMISSION_FOR_GESTURE_WALKTHROUGH + "\\\",\\n" +
        "  \\\"report_reasons\\\" : [ ],\\n" +
        "  \\\"author\\\" : \\\"" + submissionAuthorName + "\\\",\\n" +
        "  \\\"num_crossposts\\\" : 0,\\n" +
        "  \\\"num_comments\\\" : 4,\\n" +
        "  \\\"send_replies\\\" : true,\\n" +
        "  \\\"mod_reports\\\" : [ ],\\n" +
        "  \\\"approved\\\" : false,\\n" +
        "  \\\"author_flair_text_color\\\" : \\\"dark\\\",\\n" +
        "  \\\"permalink\\\" : \\\"" + submissionPermalink + "\\\",\\n" +
        "  \\\"whitelist_status\\\" : null,\\n" +
        "  \\\"stickied\\\" : false,\\n" +
        "  \\\"url\\\" : \\\"" + SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH + "\\\",\\n" +
        "  \\\"subreddit_subscribers\\\" : 1,\\n" +
        "  \\\"created_utc\\\" : " + createdTimeUtc + ",\\n" +
        "  \\\"media\\\" : null,\\n" +
        "  \\\"is_video\\\" : false\\n" +
        "}}]}}," +
        "{" +
        "  \\\"kind\\\":\\\"Listing\\\"," +
        "  \\\"data\\\":{\\\"children\\\":[{\\\"kind\\\":\\\"t1\\\",\\\"data\\\":{\\n" +
        "  \\\"subreddit_id\\\" : \\\"" + commentSubredditId + "\\\",\\n" +
        "  \\\"approved_at_utc\\\" : null,\\n" +
        "  \\\"ups\\\" : 1,\\n" +
        "  \\\"mod_reason_by\\\" : null,\\n" +
        "  \\\"banned_by\\\" : null,\\n" +
        "  \\\"author_flair_type\\\" : \\\"richtext\\\",\\n" +
        "  \\\"removal_reason\\\" : null,\\n" +
        "  \\\"link_id\\\" : \\\"" + commentSubmissionId + "\\\",\\n" +
        "  \\\"author_flair_template_id\\\" : null,\\n" +
        "  \\\"likes\\\" : null,\\n" +
        "  \\\"no_follow\\\" : false,\\n" +
        "  \\\"replies\\\" : {\\n" +
        "    \\\"kind\\\" : \\\"Listing\\\",\\n" +
        "    \\\"data\\\" : {\\n" +
        "      \\\"modhash\\\" : null,\\n" +
        "      \\\"dist\\\" : null,\\n" +
        "      \\\"children\\\" : [ {\\n" +
        "        \\\"kind\\\" : \\\"t1\\\",\\n" +
        "        \\\"data\\\" : {\\n" +
        "          \\\"subreddit_id\\\" : \\\"" + commentSubredditId + "\\\",\\n" +
        "          \\\"approved_at_utc\\\" : null,\\n" +
        "          \\\"ups\\\" : 1,\\n" +
        "          \\\"mod_reason_by\\\" : null,\\n" +
        "          \\\"banned_by\\\" : null,\\n" +
        "          \\\"author_flair_type\\\" : \\\"richtext\\\",\\n" +
        "          \\\"removal_reason\\\" : null,\\n" +
        "          \\\"link_id\\\" : \\\"" + commentSubmissionId + "\\\",\\n" +
        "          \\\"author_flair_template_id\\\" : null,\\n" +
        "          \\\"likes\\\" : null,\\n" +
        "          \\\"no_follow\\\" : false,\\n" +
        "          \\\"replies\\\" : \\\"\\\",\\n" +
        "          \\\"user_reports\\\" : [ ],\\n" +
        "          \\\"saved\\\" : false,\\n" +
        "          \\\"id\\\" : \\\"dyhg7y2\\\",\\n" +
        "          \\\"banned_at_utc\\\" : null,\\n" +
        "          \\\"mod_reason_title\\\" : null,\\n" +
        "          \\\"gilded\\\" : 0,\\n" +
        "          \\\"archived\\\" : false,\\n" +
        "          \\\"report_reasons\\\" : [ ],\\n" +
        "          \\\"author\\\" : \\\"" + commentAuthorName + "\\\",\\n" +
        "          \\\"can_mod_post\\\" : true,\\n" +
        "          \\\"send_replies\\\" : true,\\n" +
        "          \\\"parent_id\\\" : \\\"t1_dyhfxah\\\",\\n" +
        "          \\\"score\\\" : 1,\\n" +
        "          \\\"approved_by\\\" : null,\\n" +
        "          \\\"ignore_reports\\\" : false,\\n" +
        "          \\\"downs\\\" : 0,\\n" +
        "          \\\"body\\\" : \\\"" + comment3 + "\\\",\\n" +
        "          \\\"edited\\\" : false,\\n" +
        "          \\\"author_flair_css_class\\\" : null,\\n" +
        "          \\\"collapsed\\\" : false,\\n" +
        "          \\\"author_flair_richtext\\\" : null,\\n" +
        "          \\\"is_submitter\\\" : true,\\n" +
        "          \\\"collapsed_reason\\\" : null,\\n" +
        "          \\\"body_html\\\" : \\\"" + comment3Html + "\\\",\\n" +
        "          \\\"spam\\\" : false,\\n" +
        "          \\\"stickied\\\" : false,\\n" +
        "          \\\"subreddit_type\\\" : \\\"private\\\",\\n" +
        "          \\\"can_gild\\\" : false,\\n" +
        "          \\\"removed\\\" : false,\\n" +
        "          \\\"approved\\\" : false,\\n" +
        "          \\\"author_flair_text_color\\\" : \\\"dark\\\",\\n" +
        "          \\\"score_hidden\\\" : false,\\n" +
        "          \\\"permalink\\\" : \\\"" + submissionPermalink + "\\\",\\n" +
        "          \\\"num_reports\\\" : 0,\\n" +
        "          \\\"name\\\" : \\\"t1_dyhg7y2\\\",\\n" +
        "          \\\"created\\\" : 1.525544912E9,\\n" +
        "          \\\"subreddit\\\" : \\\"" + subredditName + "\\\",\\n" +
        "          \\\"author_flair_text\\\" : null,\\n" +
        "          \\\"rte_mode\\\" : \\\"markdown\\\",\\n" +
        "          \\\"created_utc\\\" : " + commentCreatedTimeUtc + ",\\n" +
        "          \\\"subreddit_name_prefixed\\\" : \\\"r/" + subredditName + "\\\",\\n" +
        "          \\\"controversiality\\\" : 0,\\n" +
        "          \\\"depth\\\" : 1,\\n" +
        "          \\\"author_flair_background_color\\\" : \\\"\\\",\\n" +
        "          \\\"mod_reports\\\" : [ ],\\n" +
        "          \\\"mod_note\\\" : null,\\n" +
        "          \\\"distinguished\\\" : null\\n" +
        "        }\\n" +
        "      } ],\\n" +
        "      \\\"after\\\" : null,\\n" +
        "      \\\"before\\\" : null\\n" +
        "    }\\n" +
        "  },\\n" +
        "  \\\"user_reports\\\" : [ ],\\n" +
        "  \\\"saved\\\" : false,\\n" +
        "  \\\"id\\\" : \\\"dyhfxah\\\",\\n" +
        "  \\\"banned_at_utc\\\" : null,\\n" +
        "  \\\"mod_reason_title\\\" : null,\\n" +
        "  \\\"gilded\\\" : 0,\\n" +
        "  \\\"archived\\\" : false,\\n" +
        "  \\\"report_reasons\\\" : [ ],\\n" +
        "  \\\"author\\\" : \\\"" + commentAuthorName + "\\\",\\n" +
        "  \\\"can_mod_post\\\" : true,\\n" +
        "  \\\"send_replies\\\" : true,\\n" +
        "  \\\"parent_id\\\" : \\\"t3_8ft61d\\\",\\n" +
        "  \\\"score\\\" : 1,\\n" +
        "  \\\"approved_by\\\" : null,\\n" +
        "  \\\"ignore_reports\\\" : false,\\n" +
        "  \\\"downs\\\" : 0,\\n" +
        "  \\\"body\\\" : \\\"" + comment1 + "\\\",\\n" +
        "  \\\"edited\\\" : false,\\n" +
        "  \\\"author_flair_css_class\\\" : null,\\n" +
        "  \\\"collapsed\\\" : false,\\n" +
        "  \\\"author_flair_richtext\\\" : null,\\n" +
        "  \\\"is_submitter\\\" : true,\\n" +
        "  \\\"collapsed_reason\\\" : null,\\n" +
        "  \\\"body_html\\\" : \\\"" + comment1Html + "\\\",\\n" +
        "  \\\"spam\\\" : false,\\n" +
        "  \\\"stickied\\\" : false,\\n" +
        "  \\\"subreddit_type\\\" : \\\"private\\\",\\n" +
        "  \\\"can_gild\\\" : false,\\n" +
        "  \\\"removed\\\" : false,\\n" +
        "  \\\"approved\\\" : false,\\n" +
        "  \\\"author_flair_text_color\\\" : \\\"dark\\\",\\n" +
        "  \\\"score_hidden\\\" : false,\\n" +
        "  \\\"permalink\\\" : \\\"" + submissionPermalink + "\\\",\\n" +
        "  \\\"num_reports\\\" : 0,\\n" +
        "  \\\"name\\\" : \\\"t1_dyhfxah\\\",\\n" +
        "  \\\"created\\\" : 1.525544142E9,\\n" +
        "  \\\"subreddit\\\" : \\\"" + subredditName + "\\\",\\n" +
        "  \\\"author_flair_text\\\" : null,\\n" +
        "  \\\"rte_mode\\\" : \\\"markdown\\\",\\n" +
        "  \\\"created_utc\\\" : " + commentCreatedTimeUtc + ",\\n" +
        "  \\\"subreddit_name_prefixed\\\" : \\\"r/" + subredditName + "\\\",\\n" +
        "  \\\"controversiality\\\" : 0,\\n" +
        "  \\\"depth\\\" : 0,\\n" +
        "  \\\"author_flair_background_color\\\" : \\\"\\\",\\n" +
        "  \\\"mod_reports\\\" : [ ],\\n" +
        "  \\\"mod_note\\\" : null,\\n" +
        "  \\\"distinguished\\\" : null\\n" +
        "}},{\\\"kind\\\":\\\"t1\\\",\\\"data\\\":{\\n" +
        "  \\\"subreddit_id\\\" : \\\"" + commentSubredditId + "\\\",\\n" +
        "  \\\"approved_at_utc\\\" : null,\\n" +
        "  \\\"ups\\\" : 1,\\n" +
        "  \\\"mod_reason_by\\\" : null,\\n" +
        "  \\\"banned_by\\\" : null,\\n" +
        "  \\\"author_flair_type\\\" : \\\"richtext\\\",\\n" +
        "  \\\"removal_reason\\\" : null,\\n" +
        "  \\\"link_id\\\" : \\\"" + commentSubmissionId + "\\\",\\n" +
        "  \\\"author_flair_template_id\\\" : null,\\n" +
        "  \\\"likes\\\" : null,\\n" +
        "  \\\"no_follow\\\" : false,\\n" +
        "  \\\"replies\\\" : \\\"\\\",\\n" +
        "  \\\"user_reports\\\" : [ ],\\n" +
        "  \\\"saved\\\" : false,\\n" +
        "  \\\"id\\\" : \\\"dyhg1i3\\\",\\n" +
        "  \\\"banned_at_utc\\\" : null,\\n" +
        "  \\\"mod_reason_title\\\" : null,\\n" +
        "  \\\"gilded\\\" : 0,\\n" +
        "  \\\"archived\\\" : false,\\n" +
        "  \\\"report_reasons\\\" : [ ],\\n" +
        "  \\\"author\\\" : \\\"" + commentAuthorName + "\\\",\\n" +
        "  \\\"can_mod_post\\\" : true,\\n" +
        "  \\\"send_replies\\\" : true,\\n" +
        "  \\\"parent_id\\\" : \\\"t3_8ft61d\\\",\\n" +
        "  \\\"score\\\" : 1,\\n" +
        "  \\\"approved_by\\\" : null,\\n" +
        "  \\\"ignore_reports\\\" : false,\\n" +
        "  \\\"downs\\\" : 0,\\n" +
        "  \\\"body\\\" : \\\"" + comment4 + "\\\",\\n" +
        "  \\\"edited\\\" : 1.525515914E9,\\n" +
        "  \\\"author_flair_css_class\\\" : null,\\n" +
        "  \\\"collapsed\\\" : false,\\n" +
        "  \\\"author_flair_richtext\\\" : null,\\n" +
        "  \\\"is_submitter\\\" : true,\\n" +
        "  \\\"collapsed_reason\\\" : null,\\n" +
        "  \\\"body_html\\\" : \\\"" + comment4Html + "\\\",\\n" +
        "  \\\"spam\\\" : false,\\n" +
        "  \\\"stickied\\\" : false,\\n" +
        "  \\\"subreddit_type\\\" : \\\"private\\\",\\n" +
        "  \\\"can_gild\\\" : false,\\n" +
        "  \\\"removed\\\" : false,\\n" +
        "  \\\"approved\\\" : false,\\n" +
        "  \\\"author_flair_text_color\\\" : \\\"dark\\\",\\n" +
        "  \\\"score_hidden\\\" : false,\\n" +
        "  \\\"permalink\\\" : \\\"" + submissionPermalink + "\\\",\\n" +
        "  \\\"num_reports\\\" : 0,\\n" +
        "  \\\"name\\\" : \\\"t1_dyhg1i3\\\",\\n" +
        "  \\\"created\\\" : 1.525544451E9,\\n" +
        "  \\\"subreddit\\\" : \\\"" + subredditName + "\\\",\\n" +
        "  \\\"author_flair_text\\\" : null,\\n" +
        "  \\\"rte_mode\\\" : \\\"markdown\\\",\\n" +
        "  \\\"created_utc\\\" : " + commentCreatedTimeUtc + ",\\n" +
        "  \\\"subreddit_name_prefixed\\\" : \\\"r/" + subredditName + "\\\",\\n" +
        "  \\\"controversiality\\\" : 0,\\n" +
        "  \\\"depth\\\" : 0,\\n" +
        "  \\\"author_flair_background_color\\\" : \\\"\\\",\\n" +
        "  \\\"mod_reports\\\" : [ ],\\n" +
        "  \\\"mod_note\\\" : null,\\n" +
        "  \\\"distinguished\\\" : null\\n" +
        "}}]}},{\\\"dank_comments_sort\\\":\\\"" + defaultSortStringUppercase + "\\\"" +
        "}]\"";
  }
}
