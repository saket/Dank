package me.saket.dank.ui.preferences;

import static me.saket.dank.utils.Views.setMarginTop;
import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.statusBarHeight;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class UserPreferencesActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.userpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_list) InboxRecyclerView preferenceList;
  @BindView(R.id.userpreferences_preferences_page) ExpandablePageLayout preferencesPage;
  @BindView(R.id.userpreferences_hiddenoptions) Button hiddenOptionsButton;

  private PreferencesFragment preferencesFragment;

  public static void start(Context context) {
    context.startActivity(new Intent(context, UserPreferencesActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_preferences);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    int statusBarHeight = statusBarHeight(getResources());
    setPaddingTop(toolbar, statusBarHeight);
    setMarginTop(preferenceList, statusBarHeight);

    setupContentExpandablePage(activityContentPage);
    activityContentPage.setNestedExpandablePage(preferencesPage);
    expandFromBelowToolbar();
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    setupPreferencesFragment();
    setupPreferencesGroupList(savedInstanceState);

    unsubscribeOnDestroy(Dank.reddit()
        .isUserLoggedIn()
        .subscribe(loggedIn -> {
          if (loggedIn) {
            String loggedInUserName = Dank.reddit().loggedInUserName();
            boolean canAccessHiddenOptions = loggedInUserName.equalsIgnoreCase("saketme");
            hiddenOptionsButton.setVisibility(canAccessHiddenOptions ? View.VISIBLE : View.GONE);
          }
        }));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    preferenceList.handleOnSaveInstance(outState);
    super.onSaveInstanceState(outState);
  }

  public void onClickPreferencesToolbarUp() {
    preferenceList.collapse();
  }

  private void setupPreferencesFragment() {
    preferencesFragment = (PreferencesFragment) getSupportFragmentManager().findFragmentById(preferencesPage.getId());
    if (preferencesFragment == null) {
      preferencesFragment = PreferencesFragment.create();
    }
    getSupportFragmentManager()
        .beginTransaction()
        .replace(preferencesPage.getId(), preferencesFragment)
        .commitNow();
  }

  private void setupPreferencesGroupList(@Nullable Bundle savedInstanceState) {
    preferenceList.setLayoutManager(preferenceList.createLayoutManager());
    preferenceList.setExpandablePage(preferencesPage, toolbar);
    preferenceList.setHasFixedSize(true);
    if (savedInstanceState != null) {
      preferenceList.handleOnRestoreInstanceState(savedInstanceState);
    }

    PreferencesAdapter preferencesAdapter = new PreferencesAdapter(constructPreferenceGroups());
    preferencesAdapter.setOnPreferenceGroupClickListener((preferenceGroup, itemView, groupId) -> {
      preferenceList.expandItem(preferenceList.indexOfChild(itemView), groupId);
      preferencesPage.post(() -> preferencesFragment.populatePreferences(preferenceGroup));
    });
    preferenceList.setAdapter(preferencesAdapter);
  }

  private List<DankPreferenceGroup> constructPreferenceGroups() {
    List<DankPreferenceGroup> preferenceGroups = new ArrayList<>();
    preferenceGroups.add(DankPreferenceGroup.LOOK_AND_FEEL);
    preferenceGroups.add(DankPreferenceGroup.MANAGE_SUBREDDITS);
    preferenceGroups.add(DankPreferenceGroup.FILTERS);
    preferenceGroups.add(DankPreferenceGroup.DATA_USAGE);
    preferenceGroups.add(DankPreferenceGroup.MISCELLANEOUS);
    preferenceGroups.add(DankPreferenceGroup.ABOUT_DANK);
    return preferenceGroups;
  }

  @OnClick(R.id.userpreferences_hiddenoptions)
  void onClickHiddenOptions() {
    HiddenPreferencesActivity.start(this);
  }

  @Override
  public void onBackPressed() {
    if (preferencesPage.isExpandedOrExpanding()) {
      preferenceList.collapse();
    } else {
      super.onBackPressed();
    }
  }

}
