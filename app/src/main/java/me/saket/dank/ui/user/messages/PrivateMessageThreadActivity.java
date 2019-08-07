package me.saket.dank.ui.user.messages;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.logError;

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
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.reply.PendingSyncReply;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;
import me.saket.dank.ui.compose.SimpleIdentifiable;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.ImageButtonWithDisabledTint;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class PrivateMessageThreadActivity extends DankPullCollapsibleActivity {

  //  private static final String KEY_MESSAGE_FULLNAME = "messageFullname";
  private static final String KEY_MESSAGE_IDENTIFIABLE = "messageIdentifiable";
  private static final String KEY_THREAD_SECOND_PARTY_NAME = "threadSecondPartyName";
  private static final int REQUEST_CODE_FULLSCREEN_REPLY = 99;

  @BindView(R.id.privatemessagethread_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.privatemessagethread_subject) TextView threadSubjectView;
  @BindView(R.id.privatemessagethread_message_list) RecyclerView messageRecyclerView;
  @BindView(R.id.privatemessagethread_reply) EditText replyField;
  @BindView(R.id.privatemessagethread_send) ImageButtonWithDisabledTint sendButton;
  @BindView(R.id.privatemessagethread_progress) ProgressBar firstLoadProgressView;
  @BindView(R.id.privatemessagethread_error_state) ErrorStateView firstLoadErrorStateView;

  @BindColor(R.color.submission_comment_byline_failed_to_post) int messageBylineForFailedReply;

  @Inject Lazy<InboxRepository> inboxRepository;
  @Inject DankLinkMovementMethod linkMovementMethod;
  @Inject DraftStore draftStore;
  @Inject ReplyRepository replyRepository;
  @Inject Markdown markdown;
  @Inject UserSessionRepository userSessionRepository;
  @Inject Lazy<ErrorResolver> errorResolver;

  private ThreadedMessagesAdapter messagesAdapter;
  private Relay<Message> latestMessageStream = BehaviorRelay.create();
  private Identifiable privateMessage;

  public static Intent intent(Context context, Message privateMessage, String threadSecondPartyName, @Nullable Rect expandFromShape) {
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
    intent.putExtra(KEY_MESSAGE_IDENTIFIABLE, SimpleIdentifiable.Companion.from(firstMessageFullName));
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
    contentPage.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(messageRecyclerView));

    // windowSoftInputMode is set to hidden in manifest so that the keyboard
    // doesn't show up on start, but we do want the reply field to be focused
    // so that if full-screen reply is closed, the keyboard continues to show.
    replyField.requestFocus();

    privateMessage = getIntent().getParcelableExtra(KEY_MESSAGE_IDENTIFIABLE);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    LinearLayoutManager layoutManager = new LinearLayoutManager(this);
    layoutManager.setStackFromEnd(true);
    messageRecyclerView.setLayoutManager(layoutManager);

    messagesAdapter = new ThreadedMessagesAdapter(linkMovementMethod);
    messageRecyclerView.setAdapter(messagesAdapter);
    messageRecyclerView.setItemAnimator(SlideUpAlphaAnimator.create());

    Observable<List<PendingSyncReply>> pendingSyncRepliesStream = replyRepository
        .streamPendingSyncReplies(ParentThread.createPrivateMessage(privateMessage.getFullName()))
        .subscribeOn(io());

    Observable<Optional<Message>> dbThread = inboxRepository.get()
        .messages(privateMessage.getFullName(), InboxFolder.PRIVATE_MESSAGES)
        .subscribeOn(io())
        .replay(1)
        .refCount();

    Observable<Message> messageThread = dbThread
        .filter(Optional::isPresent)
        .map(Optional::get)
        .cast(Message.class);

    downloadPrivateMessageIfNeeded(dbThread);

    // Subject and latest message stream.
    messageThread
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(message -> {
          threadSubjectView.setText(message.getSubject());

          List<Message> messageReplies = JrawUtils2.messageReplies(message);
          if (messageReplies.isEmpty()) {
            latestMessageStream.accept(message);
          } else {
            Message latestMessage = messageReplies.get(messageReplies.size() - 1);
            latestMessageStream.accept(latestMessage);
          }
        });

    // Adapter data-set.
    Observable.combineLatest(messageThread, pendingSyncRepliesStream, Pair::create)
        .map(pair -> {
          Message parentMessage = pair.first();
          List<Message> messageReplies = JrawUtils2.messageReplies(parentMessage);
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
              replyRepository.removeDraft(privateMessage)
                  .andThen(replyRepository.sendReply(
                      latestMessage,
                      ParentThread.createPrivateMessage(privateMessage.getFullName()), reply.toString()).toObservable())
                  .subscribe(
                      doNothing(),
                      error -> {
                        ResolvedError resolvedError = errorResolver.get().resolve(error);
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
    draftStore.streamDrafts(privateMessage)
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
            replyRepository.reSendReply(failedPendingSyncReply)
                .subscribeOn(io())
                .subscribe()
        );

    // Mark PM as read.
    messageThread
        .take(1)
        .map(thread -> {
          List<? extends Identifiable> replies = JrawUtils2.messageReplies(thread);
          List<Identifiable> messagesToMarkAsRead = new ArrayList<>(1 + replies.size());
          messagesToMarkAsRead.add(thread);
          messagesToMarkAsRead.addAll(replies);
          return Arrays2.toArray(messagesToMarkAsRead, Identifiable.class);
        })
        .doOnNext(ids -> {
          if (BuildConfig.DEBUG) {
            Timber.i("Marking %s messages as read", ids.length);
          }
        })
        .flatMapCompletable(replyIds -> inboxRepository.get().setRead(replyIds, true))
        .subscribeOn(io())
        .subscribe(doNothingCompletable(), error -> {
          ResolvedError resolvedError = errorResolver.get().resolve(error);
          resolvedError.ifUnknown(() -> Timber.e(error, "Couldn't mark PM as read"));
        });
  }

  @Override
  protected void onStop() {
    super.onStop();
    saveDraftAsynchronously();
  }

  private void downloadPrivateMessageIfNeeded(Observable<Optional<Message>> dbThread) {
    Completable downloadCompletable = dbThread
        .takeWhile(optional -> optional.isEmpty())
        .distinctUntilChanged()
        .switchMap(oo -> inboxRepository.get().fetchAndSaveMoreMessagesWithResult(InboxFolder.PRIVATE_MESSAGES)
            .toObservable()
            .subscribeOn(io()))
        .ignoreElements()
        .cache();

    Completable progressVisibilities = downloadCompletable
        .onErrorComplete()
        .andThen(Observable.just(View.GONE))
        .startWith(View.VISIBLE)
        .observeOn(mainThread())
        .flatMapCompletable(progressVisibility -> Completable.fromAction(() -> firstLoadProgressView.setVisibility(progressVisibility)));

    Completable errorStateVisibilities = downloadCompletable
        .startWith(Observable.just(Optional.<ResolvedError>empty()))
        .onErrorReturn(error -> Optional.of(errorResolver.get().resolve(error)))
        .observeOn(mainThread())
        .flatMapCompletable(optionalError -> Completable.fromAction(() -> {
          firstLoadErrorStateView.setVisibility(optionalError.isPresent() ? View.VISIBLE : View.GONE);
          optionalError.ifPresent(error -> firstLoadErrorStateView.applyFrom(error));
        }));

    Completable.mergeArrayDelayError(downloadCompletable.onErrorComplete(), progressVisibilities, errorStateVisibilities)
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    firstLoadErrorStateView.retryClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> downloadPrivateMessageIfNeeded(dbThread));
  }

  private void saveDraftAsynchronously() {
    // Fire-and-forget call. No need to dispose this since we're making no memory references to this Activity.
    draftStore.saveDraft(privateMessage, replyField.getText().toString())
        .subscribeOn(io())
        .observeOn(mainThread())
        .subscribe();
  }

  @OnClick(R.id.privatemessagethread_fullscreen)
  void onClickFullscreen() {
    saveDraftAsynchronously();

    ComposeStartOptions composeStartOptions = ComposeStartOptions.builder()
        .secondPartyName(getIntent().getStringExtra(KEY_THREAD_SECOND_PARTY_NAME))
        .parent(Optional.empty())
        .draftKey(privateMessage)
        .build();
    startActivityForResult(ComposeReplyActivity.intent(this, composeStartOptions), REQUEST_CODE_FULLSCREEN_REPLY);
  }

  private List<PrivateMessageUiModel> constructUiModels(
      Message parentMessage,
      List<Message> threadedReplies,
      List<PendingSyncReply> pendingSyncReplies,
      String loggedInUserName)
  {
    List<PrivateMessageUiModel> uiModels = new ArrayList<>(1 + threadedReplies.size() + pendingSyncReplies.size());

    // 1. Parent message.
    long parentCreatedTimeMillis = parentMessage.getCreated().getTime();
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
            .adapterId(JrawUtils2.generateAdapterId(parentMessage))
            .originalModel(parentMessage)
            .isClickable(false)
            .senderType(parentDirection)
            .build()
    );

    // 2. Replies.
    for (Message threadedReply : threadedReplies) {
      long createdTimeMillis = threadedReply.getCreated().getTime();
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
              .adapterId(JrawUtils2.generateAdapterId(threadedReply))
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
    Collections.sort(uiModels, (first, second) -> Long.compare(first.sentTimeMillis(), second.sentTimeMillis()));

    return uiModels;
  }
}
