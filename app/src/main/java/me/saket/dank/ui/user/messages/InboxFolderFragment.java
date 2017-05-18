package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doNothing;

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
import io.reactivex.schedulers.Schedulers;
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
    BetterLinkMovementMethod getMessageLinkMovementMethod();
  }

  public static InboxFolderFragment create(InboxFolder folder) {
    InboxFolderFragment fragment = new InboxFolderFragment();
    Bundle arguments = new Bundle(1);
    arguments.putSerializable(KEY_FOLDER, folder);
    fragment.setArguments(arguments);
    return fragment;
  }

  @Nullable
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

    // TODO: Remove DataStores.
    // TODO: Remove MessageCacheKey.
    // TODO: Remove onFragmentHiddenInPager().
    // TODO: Notifications?

    unsubscribeOnDestroy(Dank.inbox().message(folder)
        .doOnNext(messages -> {
          if (messages.isEmpty()) {
            Timber.i("%s Empty state", folder);
          }
        })
        .compose(applySchedulers())
        .subscribe(messagesAdapter));
    startInfiniteScroll();

    populateEmptyStateView();
    firstLoadErrorStateView.setOnRetryClickListener(o -> startInfiniteScroll());
  }

  /**
   * Called when this fragment is no longer visible in the fragment.
   */
  public void onFragmentHiddenInPager() {
  }

  private void startInfiniteScroll() {
    InfiniteScrollListener scrollListener = InfiniteScrollListener.create(messageList, 0.75f /* loadThreshold */);
    scrollListener.setEmitInitialEvent(true);

    unsubscribeOnDestroy(scrollListener
        .emitWhenLoadNeeded()
        .doOnNext(o -> Timber.i("%s Progress: load start", folder))
        .observeOn(Schedulers.io())
        .flatMapSingle(o -> Dank.inbox().fetchMoreMessages(folder)
            .doOnSubscribe(oo -> scrollListener.setLoadOngoing(true))
            .doOnSuccess(oo -> scrollListener.setLoadOngoing(false))
        )
        .doOnNext(o -> Timber.i("%s Progress: load stop", folder))
        .takeUntil(fetchMoreResult -> (boolean) fetchMoreResult.wasEmpty())
        .subscribe(doNothing(), error -> Timber.w("%s %s", folder, error.getMessage())));
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
