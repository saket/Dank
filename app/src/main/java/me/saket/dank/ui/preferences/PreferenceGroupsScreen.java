package me.saket.dank.ui.preferences;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.ScreenSavedState;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;

/**
 * Uses custom layouts for preference items because customizing them + having custom design & controls is easier.
 */
public class PreferenceGroupsScreen extends ExpandablePageLayout {

  private static final String KEY_ACTIVE_PREFERENCE_GROUP = "activePreferenceGroup";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_preference_recyclerview) InboxRecyclerView preferenceRecyclerView;

  private UserPreferenceGroup activePreferenceGroup;

  public PreferenceGroupsScreen(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this, this);

    //noinspection ConstantConditions
    toolbar.setNavigationOnClickListener(v -> ((UserPreferencesActivity) getContext()).onClickPreferencesToolbarUp());
    setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return Views.touchLiesOn(preferenceRecyclerView, downX, downY) && preferenceRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1);
    });
  }

  @Nullable
  @Override
  protected Parcelable onSaveInstanceState() {
    Bundle values = new Bundle();
    values.putSerializable(KEY_ACTIVE_PREFERENCE_GROUP, activePreferenceGroup);
    return ScreenSavedState.combine(super.onSaveInstanceState(), values);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    ScreenSavedState savedState = (ScreenSavedState) state;
    super.onRestoreInstanceState(savedState.superSavedState());

    if (savedState.values().containsKey(KEY_ACTIVE_PREFERENCE_GROUP)) {
      //noinspection ConstantConditions
      populatePreferences((UserPreferenceGroup) savedState.values().getSerializable(KEY_ACTIVE_PREFERENCE_GROUP));
    }
  }

  public void populatePreferences(UserPreferenceGroup preferenceGroup) {
    activePreferenceGroup = preferenceGroup;

//    @IdRes int groupLayoutRes;
//    switch (preferenceGroup) {
//      case LOOK_AND_FEEL:
//        groupLayoutRes = R.id.userpreferences_container_look_and_feel;
//        break;
//
//      case FILTERS:
//        groupLayoutRes = R.id.userpreferences_container_filters;
//        break;
//
//      case DATA_USAGE:
//        groupLayoutRes = R.id.userpreferences_container_data_usage;
//        break;
//
//      case MISCELLANEOUS:
//        groupLayoutRes = R.id.userpreferences_container_misc;
//        break;
//
//      case ABOUT_DANK:
//        groupLayoutRes = R.id.userpreferences_container_empty;
//        break;
//
//      default:
//        throw new UnsupportedOperationException("Unknown preference" + preferenceGroup);
//    }
  }
}
