package me.saket.dank.ui.preferences.adapter;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import me.saket.dank.R;
import me.saket.dank.data.AppInfo;
import me.saket.dank.urlparser.RedditSubredditLink;
import me.saket.dank.utils.Intents;

public class AboutDankPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final AppInfo appInfo;

  @Inject
  public AboutDankPreferencesConstructor(AppInfo appInfo) {
    this.appInfo = appInfo;
  }

  @Override
  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(String.format("Dank v%s", appInfo.appVersionName())));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_join_subreddit),
        c.getString(R.string.userprefs_about_join_subreddit_summary),
        (clickHandler, event) -> {
          RedditSubredditLink dankSubredditLink = RedditSubredditLink.create("GetDank");
          clickHandler.openLink(dankSubredditLink);
        }));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_rate_on_play_store),
        "<3",
        (clickHandler, event) -> {
          Intent playStoreIntent = Intents.createForPlayStoreListing(c, "me.thanel.dank");
          clickHandler.openIntent(playStoreIntent);
        }));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_about_feedback_and_source)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_report_issue),
        c.getString(R.string.userprefs_about_report_issue_summary),
        (clickHandler, event) -> {
          Intent githubIssuesIntent = Intents.createForOpeningUrl("https://github.com/Tunous/Dank/issues/new");
          clickHandler.openIntent(githubIssuesIntent);
        }));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_source_code),
        (clickHandler, event) -> {
          Intent githubSourceIntent = Intents.createForOpeningUrl("https://github.com/Tunous/Dank");
          clickHandler.openIntent(githubSourceIntent);
        }));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_changelog),
        (clickHandler, event) -> {
          Intent githubSourceIntent = Intents.createForOpeningUrl("https://github.com/Tunous/Dank/blob/HEAD/CHANGELOG.md");
          clickHandler.openIntent(githubSourceIntent);
        }));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_about_attributions),
        (clickHandler, event) -> Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show()));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_about_group_contact)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        "@Saketme",
        c.getString(R.string.userprefs_about_follow_on_twitter_summary),
        (clickHandler, event) -> {
          Intent twitterIntent = Intents.createForOpeningUrl("https://twitter.com/saketme");
          clickHandler.openIntent(twitterIntent);
        }));

    uiModels.add(UserPreferenceButton.UiModel.create(
        "dank@saket.me",
        c.getString(R.string.userprefs_about_send_email_summary),
        (clickHandler, event) -> {
          Intent emailIntent = Intents.createForEmail("dank@saket.me");
          if (Intents.hasAppToHandleIntent(c, emailIntent)) {
            clickHandler.openIntent(emailIntent);
          } else {
            Toast.makeText(c, R.string.userprefs_about_error_no_email_app_found, Toast.LENGTH_SHORT).show();
          }
        }));

    return uiModels;
  }
}
