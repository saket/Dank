package me.saket.dank.ui.preferences;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.squareup.sqlbrite2.BriteDatabase;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.user.messages.CachedMessage;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

@SuppressLint("SetTextI18n")
public class HiddenPreferencesActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.hiddenpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.hiddenpreferences_content) ViewGroup contentContainer;

  @Inject BriteDatabase briteDatabase;

  public static void start(Context context) {
    context.startActivity(new Intent(context, HiddenPreferencesActivity.class));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hidden_preferences);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(activityContentPage);
    expandFromBelowToolbar();
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    addButton("Clear \"seen\" message notifs", o -> {
      Dank.messagesNotifManager()
          .removeAllMessageNotifSeenStatuses()
          .andThen(Completable.fromAction(() -> CheckUnreadMessagesJobService.syncImmediately(this)))
          .subscribe();
    });

    addButton("Drop messages table", v -> {
      Completable
          .fromAction(() -> {
            briteDatabase.executeAndTrigger(CachedMessage.TABLE_NAME, "DROP TABLE " + CachedMessage.TABLE_NAME);
            briteDatabase.executeAndTrigger(CachedMessage.TABLE_NAME, CachedMessage.QUERY_CREATE_TABLE);
          })
          .compose(RxUtils.applySchedulersCompletable())
          .subscribe(() -> {
            Snackbar.make(v, "Messages dropped", Snackbar.LENGTH_SHORT).show();
          });
    });

    addButton("Clear cached submissions", o -> {
      Dank.submissions().removeAllCached().subscribeOn(Schedulers.io()).subscribe();
    });

    addButton("Clear subreddit subscriptions", o -> {
      Dank.subscriptions().removeAll()
          .subscribeOn(Schedulers.io())
          .subscribe();
    });

    addButton("Clear pending votes", o -> {
      Dank.voting().removeAll()
          .subscribeOn(Schedulers.io())
          .subscribe();
    });

    addButton("Clear pending sync replies", o -> {
      Dank.comments().removeAllPendingSyncReplies()
          .subscribeOn(Schedulers.io())
          .subscribe();
    });
  }

  private void addButton(String label, View.OnClickListener clickListener) {
    Button button = new Button(this);
    contentContainer.addView(button, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    button.setText(label);
    button.setOnClickListener(clickListener);
  }
}
