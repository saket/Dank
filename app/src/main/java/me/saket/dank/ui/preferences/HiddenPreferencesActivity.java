package me.saket.dank.ui.preferences;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;

import com.bumptech.glide.Glide;
import com.squareup.sqlbrite2.BriteDatabase;

import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.appshortcuts.AppShortcutRepository;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionCommentTreeUiConstructor;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.ui.user.messages.CachedMessage;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

@SuppressLint("SetTextI18n")
public class HiddenPreferencesActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.hiddenpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.hiddenpreferences_content_scrollview) ScrollView contentScrollView;
  @BindView(R.id.hiddenpreferences_content) ViewGroup contentContainer;

  @Inject BriteDatabase briteDatabase;
  @Inject SubmissionRepository submissionRepository;
  @Inject SubscriptionRepository subscriptionRepository;
  @Inject ReplyRepository replyRepository;
  @Inject LinkMetadataRepository linkMetadataRepository;
  @Inject VotingManager votingManager;
  @Inject UrlParser urlParser;
  @Inject Lazy<Markdown> markdown;
  @Inject Lazy<MediaHostRepository> mediaHostRepository;
  @Inject Lazy<AppShortcutRepository> appShortcutRepository;
  @Inject @Named("walkthroughs") Lazy<SharedPreferences> sharedPreferences;

  public static void start(Context context) {
    context.startActivity(new Intent(context, HiddenPreferencesActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hidden_preferences);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(activityContentPage);
    expandFromBelowToolbar();

    activityContentPage.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(contentScrollView));
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

    addButton("Clear cached submission list", o -> {
      submissionRepository.clearCachedSubmissionLists()
          .subscribeOn(io()).subscribe();
    });

    addButton("Clear cached submission comments", o -> {
      submissionRepository.clearAllCachedSubmissionComments()
          .andThen(replyRepository.removeAllPendingSyncReplies())
          .subscribeOn(io()).subscribe();
    });

    addButton("Clear subreddit subscriptions", o -> {
      subscriptionRepository.removeAll()
          .subscribeOn(io())
          .subscribe();
    });

    addButton("Clear pending votes", o -> {
      votingManager.removeAll()
          .subscribeOn(io())
          .subscribe();
    });

    addButton("Clear pending sync replies", o -> {
      replyRepository.removeAllPendingSyncReplies()
          .subscribeOn(io())
          .subscribe();
    });

    addButton("Clear link meta-data store", o -> {
      linkMetadataRepository.clearAll()
          .subscribeOn(io())
          .subscribe();
    });

    addButton("Clear UrlParser cache", o -> {
      urlParser.clearCache();
    });
    addButton("Clear media-repo cache", o -> {
      mediaHostRepository.get().clearCache();
    });

    addButton("Clear Glide cache", o -> {
      Completable
          .fromAction(() -> Glide.get(this).clearDiskCache())
          .subscribeOn(io())
          .observeOn(mainThread())
          .andThen(Completable.fromAction(() -> Glide.get(this).clearMemory()))
          .subscribe();
    });

    addButton("Clear markdown cache", o -> {
      markdown.get().clearCache();
    });

    addButton("Recycle old DB rows", o -> {
      int durationFromNow = 0;
      TimeUnit durationTimeUnit = TimeUnit.DAYS;

      submissionRepository.recycleAllCachedBefore(durationFromNow, durationTimeUnit)
          .subscribeOn(Schedulers.io())
          .takeUntil(lifecycle().onDestroyCompletable())
          .subscribe(
              deletedRows -> Timber.i("Recycled %s database rows older than %s days", deletedRows, durationTimeUnit.toDays(durationFromNow)),
              error -> Timber.e(error, "Couldn't recycle database rows"));
    });

    addButton("Reset walkthroughs", o -> {
      sharedPreferences.get().edit()
          .clear()
          .apply();
    });

    addButton("Reset collapsed comments", o -> {
      SubmissionCommentTreeUiConstructor.COLLAPSED_COMMENT_IDS.clear();
    });
  }

  private void addButton(String label, View.OnClickListener clickListener) {
    Button button = new Button(this);
    contentContainer.addView(button, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    button.setText(label);
    button.setOnClickListener(clickListener);
  }
}
