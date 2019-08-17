package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.utils.Clipboards;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.NestedOptionsPopupMenu;
import me.saket.dank.utils.Truss;

public class SubmissionOptionsPopup extends NestedOptionsPopupMenu {

  private static final int ID_SHOW_USER_PROFILE = 1;
  private static final int ID_SHOW_SUBREDDIT = 2;
  private static final int ID_SHARE_REDDIT_COMMENTS_LINK = 4;
  private static final int ID_SHARE_CONTENT_LINK = 5;
  private static final int ID_COPY_REDDIT_COMMENTS_LINK = 6;
  private static final int ID_COPY_CONTENT_LINK = 7;

  @Inject Lazy<UrlRouter> urlRouter;

  private final StartOptions startOptions;
  private final Submission submission;

  @AutoValue
  public abstract static class StartOptions {

    public abstract Context context();

    public abstract Submission submission();

    public abstract boolean showVisitSubreddit();

    public static Builder builder() {
      return new AutoValue_SubmissionOptionsPopup_StartOptions.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder context(Context context);

      public abstract Builder submission(Submission submission);

      public abstract Builder showVisitSubreddit(boolean show);

      abstract StartOptions buildOptions();

      public SubmissionOptionsPopup build() {
        StartOptions startOptions = buildOptions();
        return new SubmissionOptionsPopup(startOptions.context(), startOptions.submission(), startOptions);
      }
    }
  }

  public static StartOptions.Builder builder(Context context, Submission submission) {
    return new AutoValue_SubmissionOptionsPopup_StartOptions.Builder()
        .context(context)
        .submission(submission);
  }

  public SubmissionOptionsPopup(Context c, Submission submission, StartOptions startOptions) {
    super(c);
    this.startOptions = startOptions;
    this.submission = submission;

    Dank.dependencyInjector().inject(this);
    createMenuLayout(c, menuStructure(c));
  }

  private MenuStructure menuStructure(Context c) {
    List<MenuStructure.SingleLineItem> topLevelItems = new ArrayList<>(4);

    topLevelItems.add(MenuStructure.SingleLineItem.create(
        ID_SHOW_USER_PROFILE,
        c.getString(R.string.user_name_u_prefix, submission.getAuthor()),
        R.drawable.ic_user_profile_20dp
    ));

    if (startOptions.showVisitSubreddit()) {
      topLevelItems.add(MenuStructure.SingleLineItem.create(
          ID_SHOW_SUBREDDIT,
          c.getString(R.string.subreddit_name_r_prefix, submission.getSubreddit()),
          R.drawable.ic_subreddits_20dp
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

    topLevelItems.add(MenuStructure.SingleLineItem.create(
        isSelfPost ? ID_SHARE_REDDIT_COMMENTS_LINK : -1,
        c.getString(R.string.submission_option_share),
        R.drawable.ic_share_20dp,
        isSelfPost
            ? null
            : Arrays.asList(
                MenuStructure.ThreeLineItem.create(ID_SHARE_REDDIT_COMMENTS_LINK, redditCommentsText, R.string.cd_submission_option_share_reddit_link),
                MenuStructure.ThreeLineItem.create(ID_SHARE_CONTENT_LINK, contentLinkText, R.string.cd_submission_option_share_content_link))
    ));

    topLevelItems.add(MenuStructure.SingleLineItem.create(
        isSelfPost ? ID_COPY_REDDIT_COMMENTS_LINK : -1,
        c.getString(R.string.submission_option_copy),
        R.drawable.ic_copy_20dp,
        isSelfPost
            ? null
            : Arrays.asList(
                MenuStructure.ThreeLineItem.create(ID_COPY_REDDIT_COMMENTS_LINK, redditCommentsText, R.string.cd_submission_option_copy_reddit_link),
                MenuStructure.ThreeLineItem.create(ID_COPY_CONTENT_LINK, contentLinkText, R.string.cd_submission_option_copy_content_link))
    ));

    return MenuStructure.create(submission.getTitle(), topLevelItems);
  }

  @Override
  protected void handleAction(Context c, int actionId) {
    switch (actionId) {
      case ID_SHOW_USER_PROFILE:
        Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show();
        break;

      case ID_SHOW_SUBREDDIT:
        String subredditName = submission.getSubreddit();
        urlRouter.get()
            .forLink(RedditSubredditLink.create(subredditName))
            .expandFromBelowToolbar()
            .open(c);
        break;

      case ID_SHARE_REDDIT_COMMENTS_LINK:
        c.startActivity(Intents.createForSharingUrl(c, submissionPermalink(submission)));
        break;

      case ID_SHARE_CONTENT_LINK:
        c.startActivity(Intents.createForSharingUrl(c, submission.getUrl()));
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
      String schemeStripped = url.substring(URI.getScheme().length() + "://".length());
      if (schemeStripped.startsWith("www.")) {
        return schemeStripped.substring("www.".length());
      } else {
        return schemeStripped;
      }
    } catch (Exception e) {
      return url;
    }
  }
}
