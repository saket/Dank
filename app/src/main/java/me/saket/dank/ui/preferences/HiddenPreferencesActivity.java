package me.saket.dank.ui.preferences;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

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

import com.bumptech.glide.Glide;
import com.squareup.sqlbrite2.BriteDatabase;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.Completable;
import me.saket.dank.R;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.VotingManager;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.CheckUnreadMessagesJobService;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.ui.user.messages.CachedMessage;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

@SuppressLint("SetTextI18n")
public class HiddenPreferencesActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.hiddenpreferences_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.hiddenpreferences_content) ViewGroup contentContainer;

  @Inject BriteDatabase briteDatabase;
  @Inject SubmissionRepository submissionRepository;
  @Inject SubredditSubscriptionManager subscriptionManager;
  @Inject ReplyRepository replyRepository;
  @Inject LinkMetadataRepository linkMetadataRepository;
  @Inject VotingManager votingManager;
  @Inject UrlParser urlParser;
  @Inject Lazy<Markdown> markdown;
  @Inject Lazy<MediaHostRepository> mediaHostRepository;

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

    addButton("Clear cached submission list and comments", o -> {
      submissionRepository.clearCachedSubmissionLists()
          .andThen(submissionRepository.clearCachedSubmissionWithComments())
          .andThen(replyRepository.removeAllPendingSyncReplies())
          .subscribeOn(io()).subscribe();
    });

    addButton("Clear subreddit subscriptions", o -> {
      subscriptionManager.removeAll()
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

    addButton("Clear Glide cache", o -> {
      Completable
          .fromAction(() -> Glide.get(this).clearDiskCache())
          .subscribeOn(io())
          .observeOn(mainThread())
          .andThen(Completable.fromAction(() -> Glide.get(this).clearMemory()))
          .subscribe();
    });

    addButton("Clear markdown cache", o-> {
      markdown.get().clearCache();
    });

    addButton("Clear media-repo cache", o -> {
      mediaHostRepository.get().clearCache();
    });
  }

  private void addButton(String label, View.OnClickListener clickListener) {
    Button button = new Button(this);
    contentContainer.addView(button, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    button.setText(label);
    button.setOnClickListener(clickListener);
  }
}
