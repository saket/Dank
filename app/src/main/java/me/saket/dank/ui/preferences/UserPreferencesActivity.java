package me.saket.dank.ui.preferences;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.DankActivity;
import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class UserPreferencesActivity extends DankActivity {

    @BindView(R.id.userpreferences_root_page) IndependentExpandablePageLayout activityContentPage;
    @BindView(R.id.toolbar) Toolbar toolbar;

    public static void start(Context context) {
        context.startActivity(new Intent(context, UserPreferencesActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_preferences);
        ButterKnife.bind(this);
        findAndSetupToolbar(true);

        Views.executeOnMeasure(toolbar, () -> {
            activityContentPage.setParentActivityToolbarHeight(toolbar.getHeight());
        });

        activityContentPage.setCallbacks(new IndependentExpandablePageLayout.Callbacks() {
            @Override
            public void onPageFullyCollapsed() {
                finish();
            }

            @Override
            public void onPageRelease(boolean collapseEligible) {
                if (collapseEligible) {
                    activityContentPage.collapse();
                }
            }
        });
    }

}
