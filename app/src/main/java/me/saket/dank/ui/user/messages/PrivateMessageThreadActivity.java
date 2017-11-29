package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.InboxManager;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.widgets.ImageButtonWithDisabledTint;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class PrivateMessageThreadActivity extends DankPullCollapsibleActivity {

  private static final String KEY_MESSAGE_FULLNAME = "messageId";
  private static final String KEY_THREAD_SECOND_PARTY_NAME = "threadSecondPartyName";
  private static final int REQUEST_CODE_FULLSCREEN_REPLY = 99;

  @BindView(R.id.privatemessagethread_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.privatemessagethread_subject) TextView threadSubjectView;
  @BindView(R.id.privatemessagethread_message_list) RecyclerView messageRecyclerView;
  @BindView(R.id.privatemessagethread_reply) EditText replyField;
  @BindView(R.id.privatemessagethread_send) ImageButtonWithDisabledTint sendButton;

  @Inject InboxManager inboxManager;
  @Inject DankLinkMovementMethod linkMovementMethod;

  private ThreadedMessagesAdapter messagesAdapter;

  public static void start(Context context, Message message, String threadSecondPartyName, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, PrivateMessageThreadActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    intent.putExtra(KEY_MESSAGE_FULLNAME, message.getFullName());
    intent.putExtra(KEY_THREAD_SECOND_PARTY_NAME, threadSecondPartyName);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_private_message_thread);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setTitle(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME));

    setupContentExpandablePage(contentPage);
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));

    contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) ->
        touchLiesOn(messageRecyclerView, downX, downY) && messageRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1)
    );
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messageRecyclerView.setLayoutManager(layoutManager);

    // TODO: Empty state
    messagesAdapter = new ThreadedMessagesAdapter(linkMovementMethod);
    messageRecyclerView.setAdapter(messagesAdapter);

    inboxManager.message(getIntent().getStringExtra(KEY_MESSAGE_FULLNAME), InboxFolder.PRIVATE_MESSAGES)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(message -> threadSubjectView.setText(message.getSubject()))
        .map(parentMessage -> {
          List<Message> messageReplies = JrawUtils.messageReplies(parentMessage);
          List<Message> messages = new ArrayList<>(messageReplies.size() + 1);
          messages.add(parentMessage);
          messages.addAll(messageReplies);
          return messages;
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe(messagesAdapter);

    Observable<CharSequence> fullscreenReplyStream = lifecycle().onActivityResults()
        .filter(activityResult -> activityResult.requestCode() == REQUEST_CODE_FULLSCREEN_REPLY && activityResult.isResultOk())
        .map(activityResult -> ComposeReplyActivity.extractActivityResult(activityResult.data()))
        .map(composeResult -> composeResult.reply());

    Observable<CharSequence> inlineReplyStream = RxView.clicks(sendButton)
        .map(o -> (CharSequence) replyField.getText())
        .filter(inlineReply -> inlineReply.length() > 0)
        .doOnNext(o -> replyField.setText(null));

    inlineReplyStream.mergeWith(fullscreenReplyStream)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(reply -> Timber.w("[TODO] Send reply: %s", reply));

    // Enable send button only if there's some text.
    RxTextView.textChanges(replyField)
        .map(text -> text.length() > 0)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(RxView.enabled(sendButton));
  }

  @OnClick(R.id.privatemessagethread_fullscreen)
  void onClickFullscreen() {
    List<Message> messages = messagesAdapter.getData();
    Message latestMessage = messages.get(messages.size() - 1);

    Intent fullscreenComposeIntent = ComposeReplyActivity.intent(this, ComposeStartOptions.builder()
        .preFilledText(replyField.getText())
        .secondPartyName(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME))
        .parentContribution(latestMessage)
        .build());
    startActivityForResult(fullscreenComposeIntent, REQUEST_CODE_FULLSCREEN_REPLY);
  }
}
