package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;

import net.dean.jraw.models.Message;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.NotificationActionReceiver;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.OpenUrlActivity;
import me.saket.dank.utils.Collections;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class InboxActivity extends DankPullCollapsibleActivity implements InboxFolderFragment.Callbacks {

  private static final String KEY_SEEN_UNREAD_MESSAGES = "seenUnreadMessages";
  private static final String KEY_INITIAL_FOLDER = "initialFolder";

  @BindView(R.id.inbox_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.inbox_tablayout) TabLayout tabLayout;
  @BindView(R.id.inbox_viewpager) ViewPager viewPager;

  private Set<InboxFolder> firstRefreshDoneForFolders = new HashSet<>(InboxFolder.ALL.length);
  private DankLinkMovementMethod commentLinkMovementMethod;
  private InboxPagerAdapter inboxPagerAdapter;
  private Set<Message> seenUnreadMessages = new HashSet<>();

  /**
   * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
   */
  public static void start(Context context, @Nullable Rect expandFromShape) {
    context.startActivity(createStartIntent(context, InboxFolder.UNREAD, expandFromShape));
  }

  public static Intent createStartIntent(Context context, InboxFolder initialFolder, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, InboxActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    intent.putExtra(KEY_INITIAL_FOLDER, initialFolder);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_messages);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(contentPage);
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));

    inboxPagerAdapter = new InboxPagerAdapter(getResources(), getSupportFragmentManager());
    viewPager.setAdapter(inboxPagerAdapter);
    tabLayout.setupWithViewPager(viewPager, true);

    contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return touchLiesOn(viewPager, downX, downY) && inboxPagerAdapter.getActiveFragment().shouldInterceptPullToCollapse(upwardPagePull);
    });

    // We're only using the initial value to move to the Unread tab if any other page was active in onNewIntent().
    if (getIntent().getSerializableExtra(KEY_INITIAL_FOLDER) != InboxFolder.ALL[0]) {
      throw new UnsupportedOperationException("Hey, when did we write this code?");
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    InboxFolder folderToShow = (InboxFolder) intent.getSerializableExtra(KEY_INITIAL_FOLDER);
    viewPager.setCurrentItem(inboxPagerAdapter.getPosition(folderToShow));
  }

  @Override
  protected void onStart() {
    super.onStart();

    // Dismiss any active message notifications when the unread page is active.
    unsubscribeOnStop(RxViewPager.pageSelections(viewPager)
        .map(pageIndex -> inboxPagerAdapter.getFolder(pageIndex) == InboxFolder.UNREAD)
        .doOnNext(isUnreadActive -> Dank.sharedPrefs().setUnreadMessagesFolderActive(isUnreadActive))
        .doOnDispose(() -> Dank.sharedPrefs().setUnreadMessagesFolderActive(false))
        .flatMapCompletable(isUnreadActive -> isUnreadActive
            ? Dank.messagesNotifManager().dismissAllNotifications(this)
            : Completable.complete())
        .subscribe());
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    JsonAdapter<Set<Message>> jsonAdapter = Dank.moshi().adapter(Types.newParameterizedType(Set.class, Message.class));
    outState.putString(KEY_SEEN_UNREAD_MESSAGES, jsonAdapter.toJson(seenUnreadMessages));
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onRestoreInstanceState(Bundle inState) {
    if (inState != null) {
      final long startTime = System.currentTimeMillis();
      JsonAdapter<Set<Message>> jsonAdapter = Dank.moshi().adapter(Types.newParameterizedType(Set.class, Message.class));
      String seenUnreadMessagesJson = inState.getString(KEY_SEEN_UNREAD_MESSAGES);
      try {
        //noinspection ConstantConditions
        seenUnreadMessages = jsonAdapter.fromJson(seenUnreadMessagesJson);
        Timber.i("Deserialized in: %sms", System.currentTimeMillis() - startTime);

      } catch (IOException e) {
        Timber.e(e, "Couldn't deserialize seen unread messages json: %s", seenUnreadMessagesJson);
      }
    }
  }

  @Override
  public void finish() {
    if (!isChangingConfigurations()) {
      markSeenMessagesAsRead();
    }
    super.finish();
  }

  @Override
  public void setFirstRefreshDone(InboxFolder forFolder) {
    firstRefreshDoneForFolders.add(forFolder);
  }

  @Override
  public boolean isFirstRefreshDone(InboxFolder forFolder) {
    return firstRefreshDoneForFolders.contains(forFolder);
  }

  @Override
  public BetterLinkMovementMethod getMessageLinkMovementMethod() {
    if (commentLinkMovementMethod == null) {
      commentLinkMovementMethod = DankLinkMovementMethod.newInstance();
      commentLinkMovementMethod.setOnLinkClickListener((textView, url) -> {
        // TODO: 18/03/17 Remove try/catch block
        try {
          Link parsedLink = UrlParser.parse(url);
          Point clickedUrlCoordinates = commentLinkMovementMethod.getLastUrlClickCoordinates();
          int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
          Rect clickedUrlCoordinatesRect = new Rect(0, clickedUrlCoordinates.y, deviceDisplayWidth, clickedUrlCoordinates.y);
          OpenUrlActivity.handle(this, parsedLink, clickedUrlCoordinatesRect);
          return true;

        } catch (Exception e) {
          Timber.i(e, "Couldn't parse URL: %s", url);
          return false;
        }
      });
    }
    return commentLinkMovementMethod;
  }

  @Override
  public void markUnreadMessageAsRead(Message unreadMessage) {
    if (!seenUnreadMessages.contains(unreadMessage)) {
      seenUnreadMessages.add(unreadMessage);
    }
  }

  @Override
  public void markAllUnreadMessagesAsReadAndExit(List<Message> unreadMessages) {
    sendBroadcast(NotificationActionReceiver.createMarkAllAsReadIntent(this, unreadMessages));
    finish();
  }

  private void markSeenMessagesAsRead() {
    if (seenUnreadMessages.isEmpty()) {
      return;
    }

    Message[] seenMessagesArray = Collections.toArray(seenUnreadMessages, Message.class);
    sendBroadcast(NotificationActionReceiver.createMarkAsReadIntent(this, Dank.moshi(), seenMessagesArray));

    Dank.inbox().refreshMessages(InboxFolder.UNREAD)
        .compose(applySchedulersSingle())
        .subscribe(doNothing(), doNothing());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_inbox, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_refresh_messages) {
      InboxFolderFragment activeFragment = inboxPagerAdapter.getActiveFragment();
      activeFragment.handleOnClickRefreshMenuItem();
      return true;

    } else {
      return super.onOptionsItemSelected(item);
    }
  }
}
