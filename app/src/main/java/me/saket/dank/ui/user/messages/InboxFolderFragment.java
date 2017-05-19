package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doOnSingleStartAndEnd;
import static me.saket.dank.utils.RxUtils.doOnceAfterNext;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.InfiniteScrollListener;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter;
import me.saket.dank.widgets.EmptyStateView;
import me.saket.dank.widgets.ErrorStateView;
import timber.log.Timber;

/**
 * Displays messages under one folder. e.g., "Unread", "Messages", "Comment replies", etc.
 */
public class InboxFolderFragment extends DankFragment {

  private static final String KEY_FOLDER = "folder";

  @BindView(R.id.messagefolder_message_list) RecyclerView messageList;
  @BindView(R.id.messagefolder_first_load_progress) View firstLoadProgressView;
  @BindView(R.id.messagefolder_empty_state) EmptyStateView emptyStateView;
  @BindView(R.id.messagefolder_error_state) ErrorStateView firstLoadErrorStateView;

  private InboxFolder folder;
  private MessagesAdapter messagesAdapter;
  private InfiniteScrollRecyclerAdapter messagesAdapterWithProgress;

  interface Callbacks {
    void setFirstRefreshDone(InboxFolder forFolder);

    boolean isFirstRefreshDone(InboxFolder forFolder);

    BetterLinkMovementMethod getMessageLinkMovementMethod();
  }

  public static InboxFolderFragment create(InboxFolder folder) {
    InboxFolderFragment fragment = new InboxFolderFragment();
    Bundle arguments = new Bundle(1);
    arguments.putSerializable(KEY_FOLDER, folder);
    fragment.setArguments(arguments);
    return fragment;
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

    messagesAdapter = new MessagesAdapter(((Callbacks) getActivity()).getMessageLinkMovementMethod());
    messagesAdapterWithProgress = InfiniteScrollRecyclerAdapter.wrap(messagesAdapter);
    messageList.setAdapter(messagesAdapterWithProgress);
    messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
    messageList.setItemAnimator(new DefaultItemAnimator());

    unsubscribeOnDestroy(Dank.inbox().messages(folder)
        .compose(applySchedulers())
        .compose(doOnceAfterNext(o -> {
          // Refresh messages once we've received the messages from database for the first time.
          if (!((Callbacks) getActivity()).isFirstRefreshDone(folder)) {
            refreshMessages();
          }
        }))
        .subscribe(messagesAdapter));

    startInfiniteScroll();

    populateEmptyStateView();
  }

  protected InboxFolder getFolder() {
    return folder;
  }

  protected void refreshMessages() {
    unsubscribeOnDestroy(Dank.inbox().refreshMessages(folder)
        .compose(applySchedulersSingle())
        .compose(doOnSingleStartAndEnd(ongoing -> setProgressVisibleForRefresh(ongoing)))
        .doOnSubscribe(o -> emptyStateView.setVisibility(View.GONE))
        .subscribe(
            result -> {
              ((Callbacks) getActivity()).setFirstRefreshDone(folder);
              emptyStateView.setVisibility(result.wasEmpty() ? View.VISIBLE : View.GONE);
              firstLoadErrorStateView.setOnRetryClickListener(o -> refreshMessages());
            },
            error -> Timber.e(error, "Couldn't refresh %s", folder)
        ));
  }

  private void startInfiniteScroll() {
    InfiniteScrollListener scrollListener = InfiniteScrollListener.create(messageList, 0.75f /* loadThreshold */);
    scrollListener.setEmitInitialEvent(false);

    unsubscribeOnDestroy(scrollListener
        .emitWhenLoadNeeded()
        .flatMapSingle(o -> Dank.inbox().fetchMoreMessages(folder)
            .compose(applySchedulersSingle())
            .compose(doOnSingleStartAndEnd(ongoing -> {
              scrollListener.setLoadOngoing(ongoing);
              setProgressVisibleForFetchingMoreItems(ongoing);
            }))
        )
        .takeUntil(fetchMoreResult -> (boolean) fetchMoreResult.wasEmpty())
        .subscribe(doNothing(), error -> Timber.w("%s %s", folder, error.getMessage())));
  }

  private void setProgressVisibleForFetchingMoreItems(boolean visible) {
    messageList.post(() ->
        messagesAdapterWithProgress.setProgressVisible(InfiniteScrollRecyclerAdapter.ProgressType.MORE_DATA_LOAD_PROGRESS, visible)
    );
  }

  private void setProgressVisibleForRefresh(boolean visible) {
    if (visible) {
      if (messagesAdapter.getItemCount() == 0) {
        firstLoadProgressView.setVisibility(View.VISIBLE);
      } else {
        messageList.post(() ->
            messagesAdapterWithProgress.setProgressVisible(InfiniteScrollRecyclerAdapter.ProgressType.REFRESH_DATA_LOAD_PROGRESS, true)
        );
      }

    } else {
      firstLoadProgressView.setVisibility(View.GONE);
      messageList.post(() ->
          messagesAdapterWithProgress.setProgressVisible(InfiniteScrollRecyclerAdapter.ProgressType.REFRESH_DATA_LOAD_PROGRESS, false)
      );
    }
  }

  public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
    return messageList.canScrollVertically(upwardPagePull ? +1 : -1);
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

}
