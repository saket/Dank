package me.saket.dank.ui.user.messages;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxManager;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Truss;
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
  @BindColor(R.color.submission_comment_byline_failed_to_post) int messageBylineForFailedReply;

  @Inject InboxManager inboxManager;
  @Inject DankLinkMovementMethod linkMovementMethod;
  @Inject DraftStore draftStore;
  @Inject ReplyRepository replyRepository;
  @Inject Markdown markdown;
  @Inject ErrorResolver errorResolver;

  private ThreadedMessagesAdapter messagesAdapter;
  private Relay<Message> latestMessageStream = BehaviorRelay.create();

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

    // windowSoftInputMode is set to hidden in manifest so that the keyboard
    // doesn't show up on start, but we do want the reply field to be focused
    // so that if full-screen reply is closed, the keyboard continues to show.
    replyField.requestFocus();
  }

  // TODO: Tap to retry sending message.
  // TODO: Remove pending sync replies once we refresh.
  // TODO: Refresh all messages on start and on exit if any replies were made (to remove pending-sync replies).
  // TODO: DiffUtils.
  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messageRecyclerView.setLayoutManager(layoutManager);

    messagesAdapter = new ThreadedMessagesAdapter(linkMovementMethod);
    messageRecyclerView.setAdapter(messagesAdapter);

    String privateMessageThreadFullName = getIntent().getStringExtra(KEY_MESSAGE_FULLNAME);
    ParentThread privateMessageThread = ParentThread.createPrivateMessage(privateMessageThreadFullName);

    Observable<List<PendingSyncReply>> pendingSyncRepliesStream = replyRepository.streamPendingSyncReplies(privateMessageThread);

    Observable<Message> messageStream = inboxManager.message(privateMessageThreadFullName, InboxFolder.PRIVATE_MESSAGES)
        .doOnNext(message -> threadSubjectView.setText(message.getSubject()))
        .doOnNext(message -> {
          List<Message> messageReplies = JrawUtils.messageReplies(message);
          if (messageReplies.isEmpty()) {
            latestMessageStream.accept(message);
          } else {
            Message latestMessage = messageReplies.get(messageReplies.size() - 1);
            latestMessageStream.accept(latestMessage);
          }
        });

    // Adapter data-set.
    Observable.combineLatest(messageStream, pendingSyncRepliesStream, Pair::create)
        .subscribeOn(io())
        .map(pair -> {
          Message parentMessage = pair.first;
          List<Message> messageReplies = JrawUtils.messageReplies(parentMessage);
          List<PendingSyncReply> pendingSyncReplies = pair.second;
          return constructUiModels(parentMessage, messageReplies, pendingSyncReplies);
        })
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .doAfterNext(o -> messageRecyclerView.post(() -> messageRecyclerView.smoothScrollToPosition(messagesAdapter.getItemCount() - 1)))
        .subscribe(messagesAdapter);

    Observable<CharSequence> fullscreenReplyStream = lifecycle().onActivityResults()
        .filter(activityResult -> activityResult.requestCode() == REQUEST_CODE_FULLSCREEN_REPLY && activityResult.isResultOk())
        .map(activityResult -> ComposeReplyActivity.extractActivityResult(activityResult.data()))
        .map(composeResult -> composeResult.reply());

    Observable<CharSequence> inlineReplyStream = RxView.clicks(sendButton)
        .map(o -> (CharSequence) replyField.getText())
        .filter(inlineReply -> inlineReply.length() > 0);

    // Replies.
    inlineReplyStream.mergeWith(fullscreenReplyStream)
        .withLatestFrom(latestMessageStream, Pair::create)
        .doOnNext(o -> replyField.setText(null))
        .observeOn(Schedulers.io())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(
            pair -> {
              CharSequence reply = pair.first;
              Message latestMessage = pair.second;

              // Message sending is not a part of the chain so that it does not get unsubscribed on destroy.
              replyRepository.removeDraft(ContributionFullNameWrapper.create(privateMessageThreadFullName))
                  .andThen(replyRepository.sendReply(latestMessage, privateMessageThread, reply.toString()).toObservable())
                  .subscribe(
                      doNothing(),
                      error -> {
                        ResolvedError resolvedError = errorResolver.resolve(error);
                        if (resolvedError.isUnknown()) {
                          Timber.e(error);
                        }
                        // Error is stored in the DB, so we don't need to show anything else to the user.
                      }
                  );
            },
            logError("Failed to send message")
        );

    // Enable send button only if there's some text.
    RxTextView.textChanges(replyField)
        .map(text -> text.length() > 0)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(RxView.enabled(sendButton));

    // Drafts.
    draftStore.streamDrafts(ContributionFullNameWrapper.create(privateMessageThreadFullName))
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(draft -> {
          Timber.i("Draft received: %s", draft);
          boolean isReplyCurrentlyEmpty = replyField.getText().length() == 0;

          // Using replace() instead of setText() to preserve cursor position.
          replyField.getText().replace(0, replyField.getText().length(), draft);

          // Avoid moving the cursor around unless the text was empty.
          if (isReplyCurrentlyEmpty) {
            replyField.setSelection(draft.length());
          }
        });

    // Retry failed messages.
    messagesAdapter.streamMessageClicks()
        .map(uiModel -> uiModel.parentModel())
        .ofType(PendingSyncReply.class)
        .filter(pendingSyncReply -> pendingSyncReply.state() == PendingSyncReply.State.FAILED)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(failedPendingSyncReply ->
            Dank.reddit().withAuth(replyRepository.reSendReply(failedPendingSyncReply))
                .subscribeOn(Schedulers.io())
                .subscribe()
        );
  }

  @Override
  protected void onStop() {
    super.onStop();

    // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
    String privateMessageFullName = getIntent().getStringExtra(KEY_MESSAGE_FULLNAME);
    Contribution fakePrivateMessage = ContributionFullNameWrapper.create(privateMessageFullName);
    draftStore.saveDraft(fakePrivateMessage, replyField.getText().toString())
        .subscribeOn(Schedulers.io())
        .subscribe();
  }

  @OnClick(R.id.privatemessagethread_fullscreen)
  void onClickFullscreen() {
    String privateMessageFullName = getIntent().getStringExtra(KEY_MESSAGE_FULLNAME);
    Contribution fakePrivateMessage = ContributionFullNameWrapper.create(privateMessageFullName);
    ComposeStartOptions composeStartOptions = ComposeStartOptions.builder()
        .preFilledText(replyField.getText())
        .secondPartyName(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME))
        .draftKey(fakePrivateMessage)
        .build();
    startActivityForResult(ComposeReplyActivity.intent(this, composeStartOptions), REQUEST_CODE_FULLSCREEN_REPLY);
  }

  private List<PrivateMessageUiModel> constructUiModels(
      Message parentMessage,
      List<Message> threadedReplies,
      List<PendingSyncReply> pendingSyncReplies)
  {
    List<PrivateMessageUiModel> uiModels = new ArrayList<>(1 + threadedReplies.size() + pendingSyncReplies.size());

    // 1. Parent message.
    CharSequence parentBody = markdown.parse(parentMessage);
    String parentSender = parentMessage.getAuthor() == null
        ? getString(R.string.subreddit_name_r_prefix, parentMessage.getSubreddit())
        : parentMessage.getAuthor();
    long parentCreatedTimeMillis = JrawUtils.createdTimeUtc(parentMessage);
    CharSequence parentByline = Dates.createTimestamp(getResources(), parentCreatedTimeMillis);
    long parentAdapterId = parentMessage.getFullName().hashCode();
    uiModels.add(PrivateMessageUiModel.create(parentSender, parentBody, parentByline, parentCreatedTimeMillis, parentAdapterId, parentMessage));

    // 2. Replies.
    for (Message threadedReply : threadedReplies) {
      String senderName = threadedReply.getAuthor() == null
          ? getString(R.string.subreddit_name_r_prefix, threadedReply.getSubreddit())
          : threadedReply.getAuthor();
      CharSequence body = markdown.parse(threadedReply);
      long createdTimeMillis = JrawUtils.createdTimeUtc(threadedReply);
      CharSequence byline = Dates.createTimestamp(getResources(), createdTimeMillis);
      long adapterId = threadedReply.getFullName().hashCode();

      uiModels.add(PrivateMessageUiModel.create(senderName, body, byline, createdTimeMillis, adapterId, threadedReply));
    }

    // 3. Pending-sync replies.
    for (PendingSyncReply pendingSyncReply : pendingSyncReplies) {
      long createdTimeMillis = pendingSyncReply.createdTimeMillis();

      Truss bylineBuilder = new Truss();
      if (pendingSyncReply.state() == PendingSyncReply.State.POSTING) {
        bylineBuilder.append(getString(R.string.submission_comment_reply_byline_posting_status));
      } else if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
        bylineBuilder.pushSpan(new ForegroundColorSpan(messageBylineForFailedReply));
        bylineBuilder.append(getString(R.string.submission_comment_reply_byline_failed_status));
        bylineBuilder.popSpan();
      } else {
        bylineBuilder.append(Dates.createTimestamp(getResources(), createdTimeMillis));
      }

      CharSequence byline = bylineBuilder.build();
      CharSequence body = markdown.parse(pendingSyncReply);
      long adapterId = (pendingSyncReply.parentContributionFullName() + "_reply_ " + createdTimeMillis).hashCode();

      uiModels.add(PrivateMessageUiModel.create(pendingSyncReply.author(), body, byline, createdTimeMillis, adapterId, pendingSyncReply));
    }

    // Finally, sort the messages so that pending-sync replies are at the correct positions.
    Collections.sort(uiModels, (first, second) -> {
      if (first.createdTimeMillis() < second.createdTimeMillis()) {
        return -1;
      } else if (first.createdTimeMillis() > second.createdTimeMillis()) {
        return +1;
      } else {
        return 0;
      }
    });

    return uiModels;
  }
}
