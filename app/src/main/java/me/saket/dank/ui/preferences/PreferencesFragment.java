package me.saket.dank.ui.preferences;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewFlipper;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankFragment;

/**
 * Uses custom layouts for preference items because they customizing them + having custom design & controls is easier.
 */
public class PreferencesFragment extends DankFragment {

  private static final String KEY_ACTIVE_PREFERENCE_GROUP = "activePreferenceGroup";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_viewswitcher) ViewFlipper layoutFlipper;

  private UserPreferenceGroup activePreferenceGroup;

  public static PreferencesFragment create() {
    return new PreferencesFragment();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.fragment_user_preferences, container, false);
    ButterKnife.bind(this, layout);

    toolbar.setNavigationOnClickListener(v -> ((UserPreferencesActivity) getActivity()).onClickPreferencesToolbarUp());

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ACTIVE_PREFERENCE_GROUP)) {
      populatePreferences((UserPreferenceGroup) savedInstanceState.getSerializable(KEY_ACTIVE_PREFERENCE_GROUP));
    }

    return layout;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    if (activePreferenceGroup != null) {
      outState.putSerializable(KEY_ACTIVE_PREFERENCE_GROUP, activePreferenceGroup);
    }
    super.onSaveInstanceState(outState);
  }

  public void populatePreferences(UserPreferenceGroup preferenceGroup) {
    activePreferenceGroup = preferenceGroup;

    @IdRes int groupLayoutRes;
    switch (preferenceGroup) {
      case LOOK_AND_FEEL:
        groupLayoutRes = R.id.userpreferences_container_empty;
        break;

      case FILTERS:
        groupLayoutRes = R.id.userpreferences_container_empty;
        break;

      case DATA_USAGE:
        groupLayoutRes = R.id.userpreferences_container_data_usage;
        break;

      case MISCELLANEOUS:
        groupLayoutRes = R.id.userpreferences_container_misc;
        break;

      case ABOUT_DANK:
        groupLayoutRes = R.id.userpreferences_container_empty;
        break;

      default:
        throw new UnsupportedOperationException("Unknown preference" + preferenceGroup);
    }

    toolbar.setTitle(preferenceGroup.titleRes);
    layoutFlipper.setDisplayedChild(layoutFlipper.indexOfChild(layoutFlipper.findViewById(groupLayoutRes)));
  }
}
