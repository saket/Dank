package me.saket.dank.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class UserPreferencesActivity extends DankActivity {

    @BindView(R.id.userpreferences_root_page) IndependentExpandablePageLayout activityContentPage;
    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.userpreferences_list) InboxRecyclerView preferenceList;
    @BindView(R.id.userpreferences_preferences_page) ExpandablePageLayout preferencesPage;

    public static void start(Context context) {
        context.startActivity(new Intent(context, UserPreferencesActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_user_preferences);
        ButterKnife.bind(this);
        findAndSetupToolbar(true);

        setupActivityExpandablePage();

        preferenceList.setLayoutManager(preferenceList.createLayoutManager());
        preferenceList.setItemAnimator(new DefaultItemAnimator());
        preferenceList.setExpandablePage(preferencesPage, toolbar);
        preferenceList.setHasFixedSize(true);

        List<DankPreferenceGroup> preferenceGroups = new ArrayList<>();
        preferenceGroups.add(DankPreferenceGroup.create(
                R.drawable.ic_style_24dp,
                R.string.userpreferences_look_and_feel,
                R.string.userpreferences_look_and_feel_subtitle)
        );

        PreferencesAdapter preferencesAdapter = new PreferencesAdapter(preferenceGroups);
        preferencesAdapter.setOnPreferenceGroupClickListener((preferenceGroup, submissionItemView, submissionId) -> {
            preferenceList.expandItem(preferenceList.indexOfChild(submissionItemView), submissionId);

            // TODO: 04/03/17
            preferencesPage.post(() -> {
            });
        });
        preferenceList.setAdapter(preferencesAdapter);
    }

    private void setupActivityExpandablePage() {
        Views.executeOnMeasure(toolbar, () -> {
            activityContentPage.setParentActivityToolbarHeight(toolbar.getHeight());
            activityContentPage.expandFromBelowToolbar();
        });
        activityContentPage.setCallbacks(new IndependentExpandablePageLayout.Callbacks() {
            @Override
            public void onPageFullyCollapsed() {
                UserPreferencesActivity.super.finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onPageRelease(boolean collapseEligible) {
                if (collapseEligible) {
                    finish();
                }
            }
        });
        activityContentPage.setNestedExpandablePage(preferencesPage);
    }

    @Override
    public void finish() {
        activityContentPage.collapseBelowToolbar();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
