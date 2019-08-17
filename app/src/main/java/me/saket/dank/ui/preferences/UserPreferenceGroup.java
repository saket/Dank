package me.saket.dank.ui.preferences;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import me.saket.dank.R;

public enum UserPreferenceGroup {

  LOOK_AND_FEEL(
      R.drawable.ic_look_and_feel_24dp,
      R.string.userpreferences_look_and_feel,
      R.string.userpreferences_look_and_feel_description),

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

  @DrawableRes final int iconRes;
  @StringRes final int titleRes;
  @StringRes final int summaryRes;

  UserPreferenceGroup(@DrawableRes int iconRes, @StringRes int titleRes, @StringRes int summaryRes) {
    this.iconRes = iconRes;
    this.titleRes = titleRes;
    this.summaryRes = summaryRes;
  }
}
