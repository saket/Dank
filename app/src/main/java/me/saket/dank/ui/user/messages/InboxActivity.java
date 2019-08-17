package me.saket.dank.ui.user.messages;

import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Units.dpToPx;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ViewFlipper;

import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Types;

import net.dean.jraw.models.Message;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.MoshiAdapter;
import me.saket.dank.di.Dank;
import me.saket.dank.notifs.MessageNotifActionReceiver;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.submission.SubmissionPageLayoutActivity;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class InboxActivity extends DankPullCollapsibleActivity implements InboxFolderFragment.Callbacks {

  private static final String KEY_SEEN_UNREAD_MESSAGES = "seenUnreadMessages";
  private static final String KEY_INITIAL_FOLDER = "initialFolder";
  private static final String KEY_ACTIVE_FOLDER_INDEX = "activeTabPosition";
  private static final int REQUESTCODE_REFRESH_MESSAGES_THREAD_ON_EXIT = 99;

  @BindView(R.id.inbox_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.inbox_folder_spinner) Spinner folderNamesSpinner;
  @BindView(R.id.inbox_message_refresh_status_viewflipper) ViewFlipper refreshStatusViewFlipper;
  @BindView(R.id.inbox_fragment_container) ViewGroup fragmentContainer;

  @Inject UserSessionRepository userSessionRepository;
  @Inject InboxRepository inboxRepository;
  @Inject Lazy<MoshiAdapter> moshiAdapter;
  @Inject Lazy<UrlParser> urlParser;

  private Set<InboxFolder> firstRefreshDoneForFolders = new HashSet<>(InboxFolder.getALL().length);
  private InboxPagerAdapter inboxPagerAdapter;
  private Set<Message> seenUnreadMessages = new HashSet<>();
  private Relay<MessagesRefreshState> messagesRefreshStateStream = BehaviorRelay.create();

  public static void start(Context context) {
    context.startActivity(intent(context, InboxFolder.UNREAD));
  }

  public static Intent intent(Context context, InboxFolder initialFolder) {
    Intent intent = new Intent(context, InboxActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, (Parcelable) null);
    intent.putExtra(KEY_INITIAL_FOLDER, initialFolder);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_inbox);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    // Inbox uses a spinner on top of the toolbar.
    setTitle(null);

    setupContentExpandablePage(contentPage);
    expandFromBelowToolbar();

    contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return touchLiesOn(fragmentContainer, downX, downY) && inboxPagerAdapter.getActiveFragment().shouldInterceptPullToCollapse(upwardPagePull);
    });

    // Only Unread is supported as the initial tab right now.
    if (getIntent().getSerializableExtra(KEY_INITIAL_FOLDER) != InboxFolder.getALL()[0]) {
      throw new UnsupportedOperationException("Hey, when did we start supporting a non-unread folder: "
          + getIntent().getSerializableExtra(KEY_INITIAL_FOLDER) + "?");
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    inboxPagerAdapter = new InboxPagerAdapter(getResources(), getSupportFragmentManager());

    final CharSequence[] folderNames = getResources().getTextArray(R.array.inbox_folder_names);
    ArrayAdapter<CharSequence> folderNamesAdapter = new ArrayAdapter<>(
        this,
        R.layout.spinner_inbox_folder_selected_item,
        android.R.id.text1,
        folderNames
    );
    folderNamesAdapter.setDropDownViewResource(R.layout.list_item_inbox_folder);
    folderNamesSpinner.setAdapter(folderNamesAdapter);
    folderNamesSpinner.setDropDownVerticalOffset(dpToPx(8, this));

    if (savedInstanceState != null) {
      int retainedFolderIndex = savedInstanceState.getInt(KEY_ACTIVE_FOLDER_INDEX);
      //noinspection ConstantConditions
      folderNamesSpinner.setSelection(retainedFolderIndex);
    }

    RxAdapterView.itemSelections(folderNamesSpinner)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(selectedIndex -> {
          InboxFolderFragment currentFragment = (InboxFolderFragment) inboxPagerAdapter.instantiateItem(fragmentContainer, selectedIndex);
          inboxPagerAdapter.setPrimaryItem(fragmentContainer, selectedIndex, currentFragment);

          getSupportFragmentManager()
              .beginTransaction()
              .replace(R.id.inbox_fragment_container, currentFragment)
              .commitNowAllowingStateLoss();
        });

    messagesRefreshStateStream
        .distinctUntilChanged()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(state -> {
          @IdRes int targetChildId;
          switch (state) {
            case IN_FLIGHT:
              targetChildId = R.id.inbox_refresh_status_in_flight;
              break;
            case IDLE:
              targetChildId = R.id.inbox_refresh_status_idle;
              break;
            case ERROR:
              targetChildId = R.id.inbox_refresh_status_error;
              break;
            default:
              throw new UnsupportedOperationException();
          }
          refreshStatusViewFlipper.setDisplayedChild(refreshStatusViewFlipper.indexOfChild(refreshStatusViewFlipper.findViewById(targetChildId)));
        });
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);

    InboxFolder folderToShow = (InboxFolder) intent.getSerializableExtra(KEY_INITIAL_FOLDER);
    int folderPosition = inboxPagerAdapter.getPosition(folderToShow);
    folderNamesSpinner.setSelection(folderPosition);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    JsonAdapter<Set<Message>> jsonAdapter = moshiAdapter.get().create(Types.newParameterizedType(Set.class, Message.class));
    outState.putString(KEY_SEEN_UNREAD_MESSAGES, jsonAdapter.toJson(seenUnreadMessages));

    // ViewPager is supposed to handle restoring page index on its own, but that
    // is not working for some reason. And I don't have time to investigate why.
    outState.putInt(KEY_ACTIVE_FOLDER_INDEX, folderNamesSpinner.getSelectedItemPosition());
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onRestoreInstanceState(Bundle inState) {
    if (inState != null) {
      final long startTime = System.currentTimeMillis();
      JsonAdapter<Set<Message>> jsonAdapter = moshiAdapter.get().create(Types.newParameterizedType(Set.class, Message.class));
      String seenUnreadMessagesJson = inState.getString(KEY_SEEN_UNREAD_MESSAGES);
      try {
        //noinspection ConstantConditions
        seenUnreadMessages = jsonAdapter.fromJson(seenUnreadMessagesJson);
        //Timber.i("Deserialized in: %sms", System.currentTimeMillis() - startTime);

      } catch (IOException e) {
        Timber.e(e, "Couldn't deserialize seen unread messages json: %s", seenUnreadMessagesJson);
      }
    }
  }

  @Override
  public void finish() {
    super.finish();
    markSeenMessagesAsRead();
  }

  @Override
  public void setFirstRefreshDone(InboxFolder forFolder) {
    firstRefreshDoneForFolders.add(forFolder);
  }

  @Override
  public boolean isFirstRefreshDone(InboxFolder forFolder) {
    return firstRefreshDoneForFolders.contains(forFolder);
  }

  @OnClick({ R.id.inbox_refresh_status_idle, R.id.inbox_refresh_status_error })
  void onClickRefreshMessages() {
    inboxPagerAdapter.getActiveFragment().handleOnClickRefreshMenuItem();
  }

  @Override
  public void onClickMessage(Message message, View messageItemView) {
    // Play the expand entry animation from the bottom of the item.
    Rect messageItemViewRect = Views.globalVisibleRect(messageItemView);
    messageItemViewRect.offset(0, -Views.statusBarHeight(getResources()));
    messageItemViewRect.top = messageItemViewRect.bottom;

    if (message.isComment()) {
      String commentUrl = "https://reddit.com" + message.getContext();
      RedditSubmissionLink parsedLink = (RedditSubmissionLink) urlParser.get().parse(commentUrl);
      startActivity(SubmissionPageLayoutActivity.intent(this, parsedLink, null, message));

    } else {
      String secondPartyName = JrawUtils2.secondPartyName(getResources(), message, userSessionRepository.loggedInUserName());
      //noinspection ConstantConditions
      startActivityForResult(
          PrivateMessageThreadActivity.intent(this, message, secondPartyName, messageItemViewRect),
          REQUESTCODE_REFRESH_MESSAGES_THREAD_ON_EXIT
      );
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUESTCODE_REFRESH_MESSAGES_THREAD_ON_EXIT) {
      onClickRefreshMessages();
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public void markUnreadMessageAsSeen(Message unreadMessage) {
    if (!seenUnreadMessages.contains(unreadMessage)) {
      seenUnreadMessages.add(unreadMessage);
    }
  }

  private void markSeenMessagesAsRead() {
    if (seenUnreadMessages.isEmpty()) {
      return;
    }

    Message[] seenMessagesArray = Arrays2.toArray(seenUnreadMessages, Message.class);
    sendBroadcast(MessageNotifActionReceiver.createMarkAsReadIntent(this, moshiAdapter.get(), seenMessagesArray));

    // Marking messages as read happens on the UI thread so we can immediately refresh messages after that.
    // Though this is dangerous in case the implementation of MessageNotifActionReceiver is ever changed in the future.
    inboxRepository.refreshMessages(InboxFolder.UNREAD, false)
        .subscribeOn(io())
        .subscribe(doNothing(), logError("Couldn't refresh messages"));
  }

  @Override
  public void markAllUnreadMessagesAsReadAndExit(List<Message> unreadMessages) {
    sendBroadcast(MessageNotifActionReceiver.createMarkAllAsReadIntent(this, unreadMessages));
    finish();
  }

  @Override
  public Consumer<MessagesRefreshState> messagesRefreshStateConsumer() {
    return messagesRefreshStateStream;
  }
}
