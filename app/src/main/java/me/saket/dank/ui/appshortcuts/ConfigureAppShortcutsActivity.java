package me.saket.dank.ui.appshortcuts;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.airbnb.deeplinkdispatch.DeepLink;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

@DeepLink(ConfigureAppShortcutsActivity.DEEP_LINK)
public class ConfigureAppShortcutsActivity extends DankPullCollapsibleActivity {

  public static final String DEEP_LINK = "dank://configureAppShortcuts";

  @BindView(R.id.configureappshortcuts_root) IndependentExpandablePageLayout pageLayout;
  @BindView(R.id.toolbar) Toolbar toolbar;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setPullToCollapseEnabled(false);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configure_app_shortcuts);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setPullToCollapseEnabled(true);
    setupContentExpandablePage(pageLayout);
    expandFromBelowToolbar();

    pageLayout.setOnApplyWindowInsetsListener((o, insets) -> {
      Views.setPaddingTop(toolbar, insets.getSystemWindowInsetTop());
      return insets.consumeSystemWindowInsets();
    });
  }
}
