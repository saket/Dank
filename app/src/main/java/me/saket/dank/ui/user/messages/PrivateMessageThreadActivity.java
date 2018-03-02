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
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.widgets.ImageButtonWithDisabledTint;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class PrivateMessageThreadActivity extends DankPullCollapsibleActivity {

  private static final String KEY_MESSAGE_FULLNAME = "messageFullname";
  private static final String KEY_THREAD_SECOND_PARTY_NAME = "threadSecondPartyName";
  private static final int REQUEST_CODE_FULLSCREEN_REPLY = 99;

  @BindView(R.id.privatemessagethread_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.privatemessagethread_subject) TextView threadSubjectView;
  @BindView(R.id.privatemessagethread_message_list) RecyclerView messageRecyclerView;
  @BindView(R.id.privatemessagethread_reply) EditText replyField;
  @BindView(R.id.privatemessagethread_send) ImageButtonWithDisabledTint sendButton;
  @BindColor(R.color.submission_comment_byline_failed_to_post) int messageBylineForFailedReply;

  @Inject InboxRepository inboxRepository;
  @Inject DankLinkMovementMethod linkMovementMethod;
  @Inject DraftStore draftStore;
  @Inject ReplyRepository replyRepository;
  @Inject Markdown markdown;
  @Inject ErrorResolver errorResolver;
  @Inject UserSessionRepository userSessionRepository;

  private ThreadedMessagesAdapter messagesAdapter;
  private Relay<Message> latestMessageStream = BehaviorRelay.create();
  private ContributionFullNameWrapper privateMessageFullName;

  public static Intent intent(Context context, PrivateMessage privateMessage, String threadSecondPartyName, @Nullable Rect expandFromShape) {
    String firstMessageFullName = privateMessage.getFirstMessage();

    if (firstMessageFullName == null) {
      // This message is the root message.
      firstMessageFullName = privateMessage.getFullName();
    }

    //noinspection ConstantConditions
    if (TextUtils.isEmpty(threadSecondPartyName)) {
      throw new AssertionError();
    }

    Intent intent = new Intent(context, PrivateMessageThreadActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    //noinspection ConstantConditions
    intent.putExtra(KEY_MESSAGE_FULLNAME, ContributionFullNameWrapper.create(firstMessageFullName));
    intent.putExtra(KEY_THREAD_SECOND_PARTY_NAME, threadSecondPartyName);
    return intent;
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

    privateMessageFullName = getIntent().getParcelableExtra(KEY_MESSAGE_FULLNAME);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messageRecyclerView.setLayoutManager(layoutManager);

    messagesAdapter = new ThreadedMessagesAdapter(linkMovementMethod);
    messageRecyclerView.setAdapter(messagesAdapter);

    messageRecyclerView.setItemAnimator(new SlideUpAlphaAnimator()
        .withInterpolator(Animations.INTERPOLATOR)
        .withRemoveDuration(250)
        .withAddDuration(250));

    ParentThread privateMessageThread = ParentThread.createPrivateMessage(privateMessageFullName.getFullName());

    Observable<List<PendingSyncReply>> pendingSyncRepliesStream = replyRepository.streamPendingSyncReplies(privateMessageThread);

    Observable<PrivateMessage> messageThread = inboxRepository.message(privateMessageFullName.getFullName(), InboxFolder.PRIVATE_MESSAGES)
        .cast(PrivateMessage.class)
        .replay(1)
        .refCount();

    messageThread
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(message -> {
          threadSubjectView.setText(message.getSubject());

          List<Message> messageReplies = JrawUtils.messageReplies(message);
          if (messageReplies.isEmpty()) {
            latestMessageStream.accept(message);
          } else {
            Message latestMessage = messageReplies.get(messageReplies.size() - 1);
            latestMessageStream.accept(latestMessage);
          }
        });

    // Adapter data-set.
    Observable.combineLatest(messageThread, pendingSyncRepliesStream, Pair::create)
        .subscribeOn(io())
        .map(pair -> {
          PrivateMessage parentMessage = pair.first();
          List<Message> messageReplies = JrawUtils.messageReplies(parentMessage);
          List<PendingSyncReply> pendingSyncReplies = pair.second();
          String loggedInUserName = userSessionRepository.loggedInUserName();
          return constructUiModels(parentMessage, messageReplies, pendingSyncReplies, loggedInUserName);
        })
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(PrivateMessageItemDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyFlowable())
        .subscribe(
            itemsAndDiff -> {
              List<PrivateMessageUiModel> newComments = itemsAndDiff.first();
              messagesAdapter.updateData(newComments);
              messageRecyclerView.post(() -> messageRecyclerView.scrollToPosition(messagesAdapter.getItemCount() - 1));

              DiffUtil.DiffResult diffResult = itemsAndDiff.second();
              //noinspection ConstantConditions
              diffResult.dispatchUpdatesTo(messagesAdapter);
            },
            logError("Error while diff-ing messages")
        );

    //noinspection ConstantConditions
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
        .observeOn(io())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(
            pair -> {
              CharSequence reply = pair.first();
              Message latestMessage = pair.second();

              // Message sending is not a part of the chain so that it does not get unsubscribed on destroy.
              //noinspection ConstantConditions
              replyRepository.removeDraft(privateMessageFullName)
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
    draftStore.streamDrafts(privateMessageFullName)
        .subscribeOn(io())
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(draft -> {
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
        .map(uiModel -> uiModel.originalModel())
        .ofType(PendingSyncReply.class)
        .filter(pendingSyncReply -> pendingSyncReply.state() == PendingSyncReply.State.FAILED)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(failedPendingSyncReply ->
            Dank.reddit().withAuth(replyRepository.reSendReply(failedPendingSyncReply))
                .subscribeOn(io())
                .subscribe()
        );
  }

  @Override
  protected void onStop() {
    super.onStop();

    // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
    draftStore.saveDraft(privateMessageFullName, replyField.getText().toString())
        .subscribeOn(io())
        .subscribe();
  }

  @OnClick(R.id.privatemessagethread_fullscreen)
  void onClickFullscreen() {
    ComposeStartOptions composeStartOptions = ComposeStartOptions.builder()
        .preFilledText(replyField.getText())
        .secondPartyName(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME))
        .parentContribution(Optional.empty())
        .draftKey(privateMessageFullName)
        .build();
    startActivityForResult(ComposeReplyActivity.intent(this, composeStartOptions), REQUEST_CODE_FULLSCREEN_REPLY);
  }

  private List<PrivateMessageUiModel> constructUiModels(Message parentMessage, List<Message> threadedReplies,
      List<PendingSyncReply> pendingSyncReplies, String loggedInUserName)
  {
    List<PrivateMessageUiModel> uiModels = new ArrayList<>(1 + threadedReplies.size() + pendingSyncReplies.size());

    // 1. Parent message.
    long parentCreatedTimeMillis = JrawUtils.createdTimeUtc(parentMessage);
    PrivateMessageUiModel.Direction parentDirection = loggedInUserName.equals(parentMessage.getAuthor())
        ? PrivateMessageUiModel.Direction.SENT
        : PrivateMessageUiModel.Direction.RECEIVED;

    uiModels.add(
        PrivateMessageUiModel.builder()
            .senderName(parentMessage.getAuthor() == null
                ? getString(R.string.subreddit_name_r_prefix, parentMessage.getSubreddit())
                : parentMessage.getAuthor())
            .messageBody(markdown.parse(parentMessage))
            .byline(Dates.createTimestamp(getResources(), parentCreatedTimeMillis))
            .sentTimeMillis(parentCreatedTimeMillis)
            .adapterId((long) parentMessage.getFullName().hashCode())
            .originalModel(parentMessage)
            .isClickable(false)
            .senderType(parentDirection)
            .build()
    );

    // 2. Replies.
    for (Message threadedReply : threadedReplies) {
      long createdTimeMillis = JrawUtils.createdTimeUtc(threadedReply);
      PrivateMessageUiModel.Direction direction = loggedInUserName.equals(threadedReply.getAuthor())
          ? PrivateMessageUiModel.Direction.SENT
          : PrivateMessageUiModel.Direction.RECEIVED;

      uiModels.add(
          PrivateMessageUiModel.builder()
              .senderName(threadedReply.getAuthor() == null
                  ? getString(R.string.subreddit_name_r_prefix, threadedReply.getSubreddit())
                  : threadedReply.getAuthor())
              .messageBody(markdown.parse(threadedReply))
              .byline(Dates.createTimestamp(getResources(), createdTimeMillis))
              .sentTimeMillis(createdTimeMillis)
              .adapterId((long) threadedReply.getFullName().hashCode())
              .originalModel(threadedReply)
              .isClickable(false)
              .senderType(direction)
              .build()
      );
    }

    // 3. Pending-sync replies.
    for (PendingSyncReply pendingSyncReply : pendingSyncReplies) {
      long sentTimeMillis = pendingSyncReply.sentTimeMillis();
      long adapterId = (pendingSyncReply.parentContributionFullName() + "_reply_ " + sentTimeMillis).hashCode();

      Truss bylineBuilder = new Truss();
      if (pendingSyncReply.state() == PendingSyncReply.State.POSTING) {
        bylineBuilder.append(getString(R.string.submission_comment_reply_byline_posting_status));
      } else if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
        bylineBuilder.pushSpan(new ForegroundColorSpan(messageBylineForFailedReply));
        bylineBuilder.append(getString(R.string.submission_comment_reply_byline_failed_status));
        bylineBuilder.popSpan();
      } else {
        bylineBuilder.append(Dates.createTimestamp(getResources(), sentTimeMillis));
      }

      uiModels.add(
          PrivateMessageUiModel.builder()
              .senderName(pendingSyncReply.author())
              .messageBody(markdown.parse(pendingSyncReply))
              .byline(bylineBuilder.build())
              .sentTimeMillis(sentTimeMillis)
              .adapterId(adapterId)
              .originalModel(pendingSyncReply)
              .isClickable(pendingSyncReply.state() == PendingSyncReply.State.FAILED)
              .senderType(PrivateMessageUiModel.Direction.SENT)
              .build()
      );
    }

    // Finally, sort the messages so that pending-sync replies are at the correct positions.
    Collections.sort(uiModels, (first, second) -> {
      // Wow such stupid. How about you just do subtraction?!?.
      if (first.sentTimeMillis() < second.sentTimeMillis()) {
        return -1;
      } else if (first.sentTimeMillis() > second.sentTimeMillis()) {
        return +1;
      } else {
        return 0;
      }
    });

    return uiModels;
  }
}
