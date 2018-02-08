package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.utils.Clipboards;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.NestedOptionsPopupMenu;
import me.saket.dank.utils.Truss;

public class SubmissionOptionsPopupMenu extends NestedOptionsPopupMenu {

  private static final int ID_SHOW_USER_PROFILE = 1;
  private static final int ID_SHOW_SUBREDDIT = 2;
  private static final int ID_SHARE_REDDIT_COMMENTS_LINK = 4;
  private static final int ID_SHARE_CONTENT_LINK = 5;
  private static final int ID_COPY_REDDIT_COMMENTS_LINK = 6;
  private static final int ID_COPY_CONTENT_LINK = 7;

  @Inject Lazy<UrlRouter> urlRouter;
  private final Submission submission;

  public SubmissionOptionsPopupMenu(Context c, Submission submission, boolean showVisitSubredditOption) {
    super(c, createMenuInfo(c, submission, showVisitSubredditOption));
    Dank.dependencyInjector().inject(this);
    this.submission = submission;
  }

  private static MenuInfo createMenuInfo(Context c, Submission submission, boolean showVisitSubredditOption) {
    List<MenuInfo.SingleLineItem> topLevelItems = new ArrayList<>(4);

    topLevelItems.add(MenuInfo.SingleLineItem.create(
        ID_SHOW_USER_PROFILE,
        c.getString(R.string.user_name_u_prefix, submission.getAuthor()),
        R.drawable.ic_user_profile_20dp,
        null
    ));

    if (showVisitSubredditOption) {
      topLevelItems.add(MenuInfo.SingleLineItem.create(
          ID_SHOW_SUBREDDIT,
          c.getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()),
          R.drawable.ic_subreddits_20dp,
          null
      ));
    }

    CharSequence redditCommentsText = new Truss()
        .append(c.getString(R.string.submission_option_reddit_link))
        .append("\n")
        .pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.submission_option_button_byline)))
        .append(stripSchemeAndWww(submissionPermalink(submission)))
        .popSpan()
        .build();
    CharSequence contentLinkText = new Truss()
        .append(c.getString(R.string.submission_option_content_link))
        .append("\n")
        .pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.submission_option_button_byline)))
        .append(stripSchemeAndWww(submission.getUrl()))
        .popSpan()
        .build();

    boolean isSelfPost = submission.isSelfPost();

    topLevelItems.add(MenuInfo.SingleLineItem.create(
        isSelfPost ? ID_SHARE_REDDIT_COMMENTS_LINK : -1,
        c.getString(R.string.submission_option_share),
        R.drawable.ic_share_20dp,
        isSelfPost
            ? null
            : Arrays.asList(
                MenuInfo.ThreeLineItem.create(ID_SHARE_REDDIT_COMMENTS_LINK, redditCommentsText, R.string.cd_submission_option_share_reddit_link),
                MenuInfo.ThreeLineItem.create(ID_SHARE_CONTENT_LINK, contentLinkText, R.string.cd_submission_option_share_content_link))
    ));

    topLevelItems.add(MenuInfo.SingleLineItem.create(
        isSelfPost ? ID_COPY_REDDIT_COMMENTS_LINK : -1,
        c.getString(R.string.submission_option_copy),
        R.drawable.ic_content_copy_20dp,
        isSelfPost
            ? null
            : Arrays.asList(
                MenuInfo.ThreeLineItem.create(ID_COPY_REDDIT_COMMENTS_LINK, redditCommentsText, R.string.cd_submission_option_copy_reddit_link),
                MenuInfo.ThreeLineItem.create(ID_COPY_CONTENT_LINK, contentLinkText, R.string.cd_submission_option_copy_content_link))
    ));

    return MenuInfo.create(submission.getTitle(), topLevelItems);
  }

  @Override
  protected void handleAction(Context c, int actionId) {
    switch (actionId) {
      case ID_SHOW_USER_PROFILE:
        Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show();
        break;

      case ID_SHOW_SUBREDDIT:
        String subredditName = submission.getSubredditName();
        urlRouter.get()
            .forLink(RedditSubredditLink.create("https://reddit.com/r/" + subredditName, subredditName))
            .expandFromBelowToolbar()
            .open(c);
        break;

      case ID_SHARE_REDDIT_COMMENTS_LINK:
        c.startActivity(Intents.createForSharingUrl(null, submissionPermalink(submission)));
        break;

      case ID_SHARE_CONTENT_LINK:
        c.startActivity(Intents.createForSharingUrl(null, submission.getUrl()));
        break;

      case ID_COPY_REDDIT_COMMENTS_LINK:
        Clipboards.save(c, submissionPermalink(submission));
        Toast.makeText(c, R.string.copy_to_clipboard_confirmation, Toast.LENGTH_SHORT).show();
        break;

      case ID_COPY_CONTENT_LINK:
        Clipboards.save(c, submission.getUrl());
        Toast.makeText(c, R.string.copy_to_clipboard_confirmation, Toast.LENGTH_SHORT).show();
        break;

      default:
        throw new AssertionError();
    }
    dismiss();
  }

  private static String submissionPermalink(Submission submission) {
    return "https://reddit.com" + submission.getPermalink();
  }

  private static String stripSchemeAndWww(String url) {
    try {
      Uri URI = Uri.parse(url);
      String schemeStripped = url.substring(URI.getScheme().length() + "://" .length());
      if (schemeStripped.startsWith("www.")) {
        return schemeStripped.substring("www." .length());
      } else {
        return schemeStripped;
      }
    } catch (Exception e) {
      return url;
    }
  }
}
