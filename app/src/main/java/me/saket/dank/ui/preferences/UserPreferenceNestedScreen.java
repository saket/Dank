package me.saket.dank.ui.preferences;

import android.view.View;

import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;

public interface UserPreferenceNestedScreen extends ExpandablePageLayout.OnPullToCollapseIntercepter {

  void setNavigationOnClickListener(View.OnClickListener listener);
}
