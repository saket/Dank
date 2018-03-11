package me.saket.dank.ui.preferences.adapter;

import android.content.Context;

import com.f2prateek.rx.preferences2.Preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.UserPreferenceGroup;

public class UserPreferencesConstructor {

  private final Preference<Boolean> showSubmissionThumbnails;
  private final Observable<Object> userPrefChanges;

  @Inject
  public UserPreferencesConstructor(
      @Named("show_submission_thumbnails") Preference<Boolean> showSubmissionThumbnails,
      @Named("user_preferences") Observable<Object> userPrefChanges)
  {
    this.showSubmissionThumbnails = showSubmissionThumbnails;
    this.userPrefChanges = userPrefChanges;
  }

  public Observable<List<UserPreferencesScreenUiModel>> stream(Context c, UserPreferenceGroup group) {
    return userPrefChanges
        .map(o -> construct(c, group));
  }

  private List<UserPreferencesScreenUiModel> construct(Context c, UserPreferenceGroup group) {
    switch (group) {
      case LOOK_AND_FEEL:
        return lookAndFeel(c);

      case FILTERS:
      case DATA_USAGE:
      case MISCELLANEOUS:
      case ABOUT_DANK:
        return Collections.emptyList();

      default:
        throw new AssertionError();
    }
  }

  private List<UserPreferencesScreenUiModel> lookAndFeel(Context c) {
    List<UserPreferencesScreenUiModel> uiModels = new ArrayList<>();

    uiModels.add(UserPreferenceSectionHeader.UiModel.create(c.getString(R.string.userprefs_group_gestures)));

    uiModels.add(UserPreferenceSwitch.UiModel.create(
        c.getString(R.string.userprefs_submission_thumbnails),
        c.getString(R.string.userprefs_submission_thumbnail_summary_on),
        showSubmissionThumbnails
    ));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_submission_gestures),
        "Options and save. Upvote and downvote."
    ));

    uiModels.add(UserPreferenceButton.UiModel.create(
        c.getString(R.string.userprefs_customize_comment_gestures),
        "Options and save. Upvote and downvote."
    ));

    return uiModels;
  }
}
