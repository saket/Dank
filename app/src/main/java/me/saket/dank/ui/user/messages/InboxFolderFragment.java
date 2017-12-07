package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doOnSingleStartAndTerminate;
import static me.saket.dank.utils.RxUtils.doOnceAfterNext;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;

import net.dean.jraw.models.Message;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.data.InboxRepository;
import me.saket.dank.data.InfiniteScrollHeaderFooter;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.InfiniteScrollListener;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.EmptyStateView;
import me.saket.dank.widgets.ErrorStateView;
import timber.log.Timber;

/**
 * Displays messages under one folder. e.g., "Unread", "Messages", "Comment replies", etc.
 */
public class InboxFolderFragment extends DankFragment {

  private static final String KEY_FOLDER = "folder";

  @BindView(R.id.messagefolder_message_list) RecyclerView messageRecyclerView;
  @BindView(R.id.messagefolder_first_load_progress) View firstLoadProgressView;
  @BindView(R.id.messagefolder_empty_state) EmptyStateView emptyStateView;
  @BindView(R.id.messagefolder_error_state) ErrorStateView firstLoadErrorStateView;
  @BindView(R.id.messagefolder_mark_all_as_read) FloatingActionButton markAllAsReadButton;

  @Inject DankLinkMovementMethod linkMovementMethod;
  @Inject UserSession userSession;
  @Inject InboxRepository inboxRepository;

  private InboxFolder folder;
  private MessagesAdapter messagesAdapter;
  private InfiniteScrollRecyclerAdapter<Message, ?> messagesAdapterWithProgress;
  private boolean isRefreshOngoing;

  interface Callbacks extends MessagesAdapter.OnMessageClickListener {
    void setFirstRefreshDone(InboxFolder forFolder);

    boolean isFirstRefreshDone(InboxFolder forFolder);

    @Override
    void onClickMessage(Message message, View messageItemView);

    /**
     * Called as the user scrolls the unread message list. All the seen unread messages are
     * marked as read on Activity exit.
     */
    void markUnreadMessageAsSeen(Message unreadMessage);

    void markAllUnreadMessagesAsReadAndExit(List<Message> unreadMessages);

    Consumer<MessagesRefreshState> messagesRefreshStateConsumer();
  }

  public static InboxFolderFragment create(InboxFolder folder) {
    InboxFolderFragment fragment = new InboxFolderFragment();
    Bundle arguments = new Bundle(1);
    arguments.putSerializable(KEY_FOLDER, folder);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Override
  public void onAttach(Context context) {
    Dank.dependencyInjector().inject(this);
    super.onAttach(context);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    View layout = inflater.inflate(R.layout.fragment_message_folder, container, false);
    ButterKnife.bind(this, layout);
    return layout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    folder = (InboxFolder) getArguments().getSerializable(KEY_FOLDER);

    boolean showMessageThreads = folder == InboxFolder.PRIVATE_MESSAGES;
    messagesAdapter = new MessagesAdapter(linkMovementMethod, showMessageThreads, userSession.loggedInUserName());
    messagesAdapter.setOnMessageClickListener(((Callbacks) getActivity()));
    messagesAdapterWithProgress = InfiniteScrollRecyclerAdapter.wrap(messagesAdapter);

    messageRecyclerView.setAdapter(messagesAdapterWithProgress);
    messageRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    messageRecyclerView.setItemAnimator(new DefaultItemAnimator());

    populateEmptyStateView();
    trackSeenUnreadMessages();
  }

  @Override
  public void onStart() {
    super.onStart();
    Callbacks callbacks = (Callbacks) getActivity();

    inboxRepository.messages(folder)
        .compose(applySchedulers())
        .compose(doOnceAfterNext(o -> {
          startInfiniteScroll(false);

          // Refresh messages once we've received the messages from database for the first time.
          if (!callbacks.isFirstRefreshDone(folder)) {
            refreshMessages(false);
          }
        }))
        .doOnNext(messages -> {
          if (callbacks.isFirstRefreshDone(folder)) {
            emptyStateView.setVisibility(messages.isEmpty() ? View.VISIBLE : View.GONE);
          }
        })
        .doAfterNext(messages -> {
          // Show FAB in unread folder for marking all as read.
          if (folder == InboxFolder.UNREAD && !messages.isEmpty()) {
            markAllAsReadButton.show();
            Views.executeOnMeasure(markAllAsReadButton, () -> {
              MarginLayoutParams fabMarginLayoutParams = (MarginLayoutParams) markAllAsReadButton.getLayoutParams();
              int spaceForFab = markAllAsReadButton.getHeight() + fabMarginLayoutParams.topMargin + fabMarginLayoutParams.bottomMargin;
              Views.setPaddingBottom(messageRecyclerView, spaceForFab);
            });

          } else {
            markAllAsReadButton.hide();
            Views.setPaddingBottom(messageRecyclerView, 0);
          }
        })
        .takeUntil(lifecycle().onStop())
        .subscribe(messagesAdapter);
  }

  public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
    return messageRecyclerView.canScrollVertically(upwardPagePull ? +1 : -1);
  }

  /**
   * Refresh message (only if not already refreshing).
   */
  protected void handleOnClickRefreshMenuItem() {
    if (!isRefreshOngoing) {
      refreshMessages(true);
    }
  }

  protected void refreshMessages(boolean replaceAllExistingMessages) {
    inboxRepository.refreshMessages(folder, replaceAllExistingMessages)
        .compose(applySchedulersSingle())
        .compose(handleProgressAndErrorForFirstRefresh(replaceAllExistingMessages))
        .compose(handleProgressAndErrorForSubsequentRefresh())
        .compose(doOnSingleStartAndTerminate(ongoing -> isRefreshOngoing = ongoing))
        .doOnSubscribe(o -> emptyStateView.setVisibility(View.GONE))
        .takeUntil(lifecycle().onDestroy().ignoreElements())
        .subscribe(fetchedMessages -> {
          if (isAdded()) {
            ((Callbacks) getActivity()).setFirstRefreshDone(folder);
          }
          emptyStateView.setVisibility(fetchedMessages.isEmpty() ? View.VISIBLE : View.GONE);
        }, doNothing());
  }

  private <T> SingleTransformer<T, T> handleProgressAndErrorForFirstRefresh(boolean deleteAllMessagesInFolder) {
    if (messagesAdapter.getItemCount() > 0) {
      return upstream -> upstream;
    }

    return upstream -> upstream
        .doOnSubscribe(o -> {
          firstLoadProgressView.setVisibility(View.VISIBLE);
          firstLoadErrorStateView.setVisibility(View.GONE);
        })
        .doOnSuccess(o -> firstLoadProgressView.setVisibility(View.GONE))
        .doOnError(error -> {
          ResolvedError resolvedError = Dank.errors().resolve(error);
          if (resolvedError.isUnknown()) {
            Timber.e(error, "Unknown error while refreshing messages in %s folder.", folder);
          }
          firstLoadErrorStateView.applyFrom(resolvedError);
          firstLoadErrorStateView.setVisibility(View.VISIBLE);
          firstLoadErrorStateView.setOnRetryClickListener(o -> refreshMessages(deleteAllMessagesInFolder));

          firstLoadProgressView.setVisibility(View.GONE);
        });
  }

  private <T> SingleTransformer<T, T> handleProgressAndErrorForSubsequentRefresh() {
    Consumer<MessagesRefreshState> messagesRefreshStateConsumer = ((Callbacks) getActivity()).messagesRefreshStateConsumer();

    if (messagesAdapter.getItemCount() == 0) {
      return upstream -> upstream.doOnSubscribe(o -> messagesRefreshStateConsumer.accept(MessagesRefreshState.IDLE));
    }

    return upstream -> upstream
        .doOnSubscribe(o -> messagesRefreshStateConsumer.accept(MessagesRefreshState.IN_FLIGHT))
        .doOnSuccess(o -> messagesRefreshStateConsumer.accept(MessagesRefreshState.IDLE))
        .doOnError(error -> {
          messagesRefreshStateConsumer.accept(MessagesRefreshState.ERROR);

          ResolvedError resolvedError = Dank.errors().resolve(error);
          if (resolvedError.isUnknown()) {
            Timber.e(error, "Unknown error while refreshing messages in %s folder.", folder);
          }
        });
  }

  /**
   * @param isRetrying When true, more items are fetched right away. Otherwise, we wait for {@link InfiniteScrollListener} to emit.
   */
  private void startInfiniteScroll(boolean isRetrying) {
    InfiniteScrollListener scrollListener = InfiniteScrollListener.create(messageRecyclerView, InfiniteScrollListener.DEFAULT_LOAD_THRESHOLD);
    scrollListener.setEmitInitialEvent(isRetrying);

    scrollListener.emitWhenLoadNeeded()
        .flatMapSingle(o -> inboxRepository.fetchMoreMessages(folder)
            .compose(applySchedulersSingle())
            .compose(handleProgressAndErrorForLoadMore())
            .compose(doOnSingleStartAndTerminate(ongoing -> scrollListener.setLoadOngoing(ongoing)))
        )
        .takeUntil(fetchedMessages -> (boolean) fetchedMessages.isEmpty())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(doNothing(), doNothing());
  }

  private <T> SingleTransformer<T, T> handleProgressAndErrorForLoadMore() {
    return upstream -> upstream
        .doOnSubscribe(o -> messagesAdapterWithProgress.setFooter(InfiniteScrollHeaderFooter.createFooterProgress()))
        .doOnSuccess(o -> messagesAdapterWithProgress.setFooter(InfiniteScrollHeaderFooter.createHidden()))
        .doOnError(error -> {
          messagesAdapterWithProgress.setFooter(InfiniteScrollHeaderFooter.createError(
              R.string.inbox_error_failed_to_load_more_messages,
              o -> startInfiniteScroll(true)
          ));
        });
  }

  private void populateEmptyStateView() {
    emptyStateView.setEmoji(R.string.inbox_empty_state_title);
    switch (folder) {
      case UNREAD:
        emptyStateView.setMessage(R.string.inbox_empty_state_message_for_unread);
        break;

      case PRIVATE_MESSAGES:
        emptyStateView.setMessage(R.string.inbox_empty_state_message_for_private_messages);
        break;

      case COMMENT_REPLIES:
        emptyStateView.setMessage(R.string.inbox_empty_state_message_for_comment_replies);
        break;

      case POST_REPLIES:
        emptyStateView.setMessage(R.string.inbox_empty_state_message_for_post_replies);
        break;

      case USERNAME_MENTIONS:
        emptyStateView.setMessage(R.string.inbox_empty_state_message_for_username_mentions);
        break;
    }
  }

  private void trackSeenUnreadMessages() {
    if (folder != InboxFolder.UNREAD) {
      return;
    }

    RxRecyclerView.scrollEvents(messageRecyclerView)
        .map(scrollEvent -> {
          int firstVisiblePosition = ((LinearLayoutManager) messageRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
          int lastVisiblePosition = ((LinearLayoutManager) messageRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();

          if (firstVisiblePosition == -1 && lastVisiblePosition == -1) {
            // Currently displayed item is longer than the window, so there is no completely visible item.
            firstVisiblePosition = ((LinearLayoutManager) messageRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
            lastVisiblePosition = ((LinearLayoutManager) messageRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
          }

          if (firstVisiblePosition != -1) {
            for (int i = firstVisiblePosition; i <= lastVisiblePosition; i++) {
              if (messagesAdapterWithProgress.isWrappedAdapterItem(i)) {
                Message message = messagesAdapterWithProgress.getItemInWrappedAdapter(i);
                ((Callbacks) getActivity()).markUnreadMessageAsSeen(message);
              }
            }
          }

          return scrollEvent;
        })
        .takeUntil(lifecycle().onDestroy())
        .subscribe(doNothing(), error -> Timber.e(error, "Couldn't track seen unread messages"));
  }

  @OnClick(R.id.messagefolder_mark_all_as_read)
  void onClickMarkAllAsRead() {
    ((Callbacks) getActivity()).markAllUnreadMessagesAsReadAndExit(messagesAdapter.getData());
  }

}
