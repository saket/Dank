package me.saket.dank.ui.preferences;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import me.saket.dank.R;

public enum DankPreferenceGroup {

  LOOK_AND_FEEL(
      R.drawable.ic_look_and_feel_24dp,
      R.string.userpreferences_look_and_feel,
      R.string.userpreferences_look_and_feel_description),

  MANAGE_SUBREDDITS(
      R.drawable.ic_subreddits_24dp,
      R.string.userpreferences_manage_subreddits,
      R.string.userpreferences_manage_subreddits_description),

  FILTERS(
      R.drawable.ic_visibility_off_24dp,
      R.string.userpreferences_filters,
      R.string.userpreferences_filters_description),

  DATA_USAGE(
      R.drawable.ic_data_setting_24dp,
      R.string.userpreferences_data_usage,
      R.string.userpreferences_data_usage_description),

  MISCELLANEOUS(
      R.drawable.ic_settings_24dp,
      R.string.userpreferences_misc,
      R.string.userpreferences_misc_description),

  ABOUT_DANK(
      R.drawable.ic_adb_24dp,
      R.string.userpreferences_about,
      R.string.userpreferences_about_description);

  final int iconRes;
  final int titleRes;
  final int subtitleRes;

  DankPreferenceGroup(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int subtitleRes) {
    this.iconRes = iconRes;
    this.titleRes = titleRes;
    this.subtitleRes = subtitleRes;
  }
}
