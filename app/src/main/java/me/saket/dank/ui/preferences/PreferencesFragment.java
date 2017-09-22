package me.saket.dank.ui.preferences;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.ViewFlipper;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

/**
 * Uses custom layouts for preference items because they customizing them + having custom design & controls is easier.
 */
public class PreferencesFragment extends DankFragment {

  private static final String KEY_ACTIVE_PREFERENCE_GROUP = "activePreferenceGroup";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_scrollview) ScrollView contentScrollView;
  @BindView(R.id.userpreferences_viewswitcher) ViewFlipper layoutFlipper;

  private ExpandablePageLayout contentPageLayout;
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

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ACTIVE_PREFERENCE_GROUP)) {
      populatePreferences((UserPreferenceGroup) savedInstanceState.getSerializable(KEY_ACTIVE_PREFERENCE_GROUP));
    }

    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    toolbar.setNavigationOnClickListener(v -> ((UserPreferencesActivity) getActivity()).onClickPreferencesToolbarUp());
    contentPageLayout = ((ExpandablePageLayout) view.getParent());
    contentPageLayout.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return Views.touchLiesOn(contentScrollView, downX, downY) && contentScrollView.canScrollVertically(upwardPagePull ? 1 : -1);
    });
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
        groupLayoutRes = R.id.userpreferences_container_look_and_feel;
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
    contentScrollView.scrollTo(0, 0);
  }
}
