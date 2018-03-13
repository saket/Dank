package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import me.saket.dank.R;

public class LookAndFeelPreferencesConstructor implements UserPreferencesConstructor.ChildConstructor {

  private final Preference<Boolean> showSubmissionThumbnails;

  @Inject
  public LookAndFeelPreferencesConstructor(
      @Named("show_submission_thumbnails") Preference<Boolean> showSubmissionThumbnails)
  {
    this.showSubmissionThumbnails = showSubmissionThumbnails;
  }

  public List<UserPreferencesScreenUiModel> construct(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_subreddit)));

    uiModels.add(UserPreferenceSwitch.UiModel.create(
        c.getString(R.string.userprefs_submission_thumbnails),
        showSubmissionThumbnails.get()
            ? c.getString(R.string.userprefs_submission_thumbnail_summary_on)
            : c.getString(R.string.userprefs_submission_thumbnail_summary_off),
        showSubmissionThumbnails
    ));

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_gestures)));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_submission_gestures),
        "Options and save. Upvote and downvote.",
        "poop1"
    ));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_comment_gestures),
        "Options and save. Upvote and downvote.",
        "poop2"
    ));

    return uiModels;
  }
}
