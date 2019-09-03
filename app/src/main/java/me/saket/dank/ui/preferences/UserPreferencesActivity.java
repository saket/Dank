package me.saket.dank.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.BackPressCallback;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

public class UserPreferencesActivity extends DankPullCollapsibleActivity {

  private static final String KEY_INITIAL_PREF_GROUP = "initialPrefGroup";

  @BindView(R.id.userpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_list) InboxRecyclerView preferenceList;
  @BindView(R.id.userpreferences_preference_groups_page) PreferenceGroupsScreen preferencesGroupsPage;
  @BindView(R.id.userpreferences_hiddenoptions) Button hiddenOptionsButton;

  private List<UserPreferenceGroup> userPreferenceGroups;
  private UserPreferenceGroupAdapter userPreferenceGroupAdapter;

  public static Intent intent(Context context) {
    return new Intent(context, UserPreferencesActivity.class);
  }

  public static Intent intent(Context context, UserPreferenceGroup initialPreferenceGroup) {
    return new Intent(context, UserPreferencesActivity.class)
        .putExtra(KEY_INITIAL_PREF_GROUP, initialPreferenceGroup);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_preferences);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(activityContentPage);
    activityContentPage.setNestedExpandablePage(preferencesGroupsPage);
    expandFromBelowToolbar();

    activityContentPage.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(preferenceList));
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    userPreferenceGroups = constructPreferenceGroups();
    setupPreferencesGroupList(savedInstanceState);

    if (getIntent().hasExtra(KEY_INITIAL_PREF_GROUP)) {
      UserPreferenceGroup initialPreferenceGroup = (UserPreferenceGroup) getIntent().getSerializableExtra(KEY_INITIAL_PREF_GROUP);
      preferencesGroupsPage.populatePreferences(initialPreferenceGroup);

      // The deep-linked screen is delayed. By not opening it immediately, the user can understand that
      // an automatic navigation change is happening. This is important because opening this deep-linked
      // screen takes only one click, but going back to the calling screen will take two (one for the
      // linked screen and another for this Activity) -- and the user should understand this.
      Observable.timer(750, TimeUnit.MILLISECONDS, mainThread())
          .takeUntil(lifecycle().onDestroy())
          .subscribe(o -> preferencesGroupsPage.post(() -> {
            int prefPosition = userPreferenceGroups.indexOf(initialPreferenceGroup);
            preferenceList.expandItem(prefPosition, userPreferenceGroupAdapter.getItemId(prefPosition));
          }));
    }

    hiddenOptionsButton.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    preferenceList.saveExpandableState(outState);
    super.onSaveInstanceState(outState);
  }

  public void onClickPreferencesToolbarUp() {
    preferenceList.collapse();
  }

  private void setupPreferencesGroupList(@Nullable Bundle savedInstanceState) {
    preferenceList.setLayoutManager(preferenceList.createLayoutManager());
    preferenceList.setExpandablePage(preferencesGroupsPage, toolbar);
    preferenceList.setHasFixedSize(true);
    if (savedInstanceState != null) {
      preferenceList.restoreExpandableState(savedInstanceState);
    }

    userPreferenceGroupAdapter = new UserPreferenceGroupAdapter(userPreferenceGroups);
    userPreferenceGroupAdapter.setOnPreferenceGroupClickListener((preferenceGroup, itemView, groupId) -> {
      preferencesGroupsPage.populatePreferences(preferenceGroup);

      // A small delay allows the RecyclerView to inflate Views before the page fully expands.
      preferencesGroupsPage.postDelayed(
          () -> preferenceList.expandItem(preferenceList.indexOfChild(itemView), groupId),
          100);
    });
    preferenceList.setAdapter(userPreferenceGroupAdapter);
  }

  private List<UserPreferenceGroup> constructPreferenceGroups() {
    List<UserPreferenceGroup> preferenceGroups = new ArrayList<>();
    preferenceGroups.add(UserPreferenceGroup.LOOK_AND_FEEL);
    preferenceGroups.add(UserPreferenceGroup.FILTERS);
    preferenceGroups.add(UserPreferenceGroup.DATA_USAGE);
    preferenceGroups.add(UserPreferenceGroup.MISCELLANEOUS);
    preferenceGroups.add(UserPreferenceGroup.ABOUT_DANK);
    return preferenceGroups;
  }

  @OnClick(R.id.userpreferences_hiddenoptions)
  void onClickHiddenOptions() {
    HiddenPreferencesActivity.start(this);
  }

  @Override
  public void onBackPressed() {
    BackPressCallback callback = preferencesGroupsPage.onInterceptBackPress();
    if (callback.intercepted()) {
      return;
    }
    if (preferencesGroupsPage.isExpandedOrExpanding()) {
      preferenceList.collapse();
      return;
    }
    super.onBackPressed();
  }
}
