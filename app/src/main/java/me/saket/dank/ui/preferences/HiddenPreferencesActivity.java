package me.saket.dank.ui.preferences;

import static me.saket.dank.utils.Views.setPaddingTop;
import static me.saket.dank.utils.Views.statusBarHeight;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

@SuppressLint("SetTextI18n")
public class HiddenPreferencesActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.hiddenpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.hiddenpreferences_content) ViewGroup contentContainer;

  public static void start(Context context) {
      context.startActivity(new Intent(context, HiddenPreferencesActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hidden_preferences);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setPaddingTop(toolbar, statusBarHeight(getResources()));

    setupContentExpandablePage(activityContentPage);
    expandFromBelowToolbar();
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    Button clearSeenMessageNotifsButton = new Button(this);
    clearSeenMessageNotifsButton.setText("Clear \"seen\" message notifs");
    clearSeenMessageNotifsButton.setOnClickListener(o -> {
      Dank.messagesNotifManager()
          .removeAllMessageNotifSeenStatuses()
          .andThen(Completable.fromAction(() -> CheckUnreadMessagesJobService.syncImmediately(this)))
          .subscribe();
    });
    contentContainer.addView(clearSeenMessageNotifsButton, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

}
