package me.saket.dank.ui.user.messages;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.InfiniteScrollListener;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter.ProgressType;
import me.saket.dank.utils.InfiniteScroller;
import me.saket.dank.widgets.EmptyStateView;
import me.saket.dank.widgets.ErrorStateView;
import timber.log.Timber;

/**
 * Displays messages under one folder. e.g., "Unread", "Messages", "Comment replies", etc.
 */
public class MessageFolderFragment extends DankFragment {

    private static final String KEY_FOLDER = "folder";

    @BindView(R.id.messagefolder_message_list) RecyclerView messageList;
    @BindView(R.id.messagefolder_first_load_progress) View firstLoadProgressView;
    @BindView(R.id.messagefolder_empty_state) EmptyStateView emptyStateView;
    @BindView(R.id.messagefolder_error_state) ErrorStateView firstLoadErrorStateView;

    private InboxFolder folder;
    private MessagesAdapter messagesAdapter;
    private InfiniteScrollRecyclerAdapter messagesAdapterWithProgress;
    private Snackbar loadMoreErrorSnackbar;     // TODO: Rxify this.

    interface Callbacks {
        InfiniteScroller.InfiniteDataStreamFunction<Message> fetchMoreMessages(InboxFolder folder);

        BetterLinkMovementMethod getMessageLinkMovementMethod();
    }

    public static MessageFolderFragment create(InboxFolder folder) {
        MessageFolderFragment fragment = new MessageFolderFragment();
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

        messagesAdapter = new MessagesAdapter(((Callbacks) getActivity()).getMessageLinkMovementMethod());
        messagesAdapterWithProgress = InfiniteScrollRecyclerAdapter.wrap(messagesAdapter);
        messageList.setAdapter(messagesAdapterWithProgress);

        messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageList.setItemAnimator(new DefaultItemAnimator());

        folder = (InboxFolder) getArguments().getSerializable(KEY_FOLDER);
        subscribeToMessageLoads();

        populateEmptyStateView();
        firstLoadErrorStateView.setOnRetryClickListener(o -> subscribeToMessageLoads());
    }

    /**
     * Called when the ViewPager is swiped and this page goes out of the view.
     */
    public void onFragmentHiddenInPager() {
        if (loadMoreErrorSnackbar != null) {
            loadMoreErrorSnackbar.dismiss();
        }
    }

    private void subscribeToMessageLoads() {
        InfiniteScroller.ViewStateCallbacks<Message> viewStateCallbacks = new InfiniteScroller.ViewStateCallbacks<Message>() {
            @Override
            public void setFirstLoadProgressVisible(boolean visible) {
                firstLoadProgressView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            @Override
            public void setMoreLoadProgressVisible(boolean visible) {
                messageList.post(() -> messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, visible));
            }

            @Override
            public void setRefreshProgressVisible(boolean visible) {
                //Timber.i("setRefreshProgressVisible() -> visible: %s", visible);
                messageList.post(() -> messagesAdapterWithProgress.setProgressVisible(ProgressType.REFRESH_DATA_LOAD_PROGRESS, visible));
            }

            @Override
            public void setEmptyStateVisible(boolean visible) {
                emptyStateView.setVisibility(visible ? View.VISIBLE : View.GONE);
            }

            @Override
            public void updateItemsDataset(List<Message> items) {
                messagesAdapter.updateData(items);
            }

            @Override
            public void setErrorOnFirstLoadVisible(@Nullable Throwable error, boolean errorVisible) {
                firstLoadErrorStateView.setVisibility(errorVisible ? View.VISIBLE : View.GONE);

                if (errorVisible) {
                    ResolvedError resolvedError = Dank.errors().resolve(error);
                    if (resolvedError.isUnknown()) {
                        Timber.e(error, "Couldn't fetch more messages under %s", folder);
                    }
                    firstLoadErrorStateView.applyFrom(resolvedError);
                }
            }

            @Override
            public void setErrorOnMoreLoadVisible(@Nullable Throwable error, boolean errorVisible) {
                if (!isVisible()) {
                    // Ignore showing the snackbar if this fragment isn't visible,
                    // because the Snackbar is appearing in the Activity's layout.
                    return;
                }

                if (errorVisible) {
                    ResolvedError resolvedError = Dank.errors().resolve(error);
                    if (resolvedError.isUnknown()) {
                        Timber.e(error, "Couldn't fetch more messages under %s", folder);
                    }

                    loadMoreErrorSnackbar = Snackbar.make(messageList, resolvedError.errorMessageRes(), Snackbar.LENGTH_INDEFINITE);
                    loadMoreErrorSnackbar.setAction(R.string.common_error_retry, v -> subscribeToMessageLoads());
                    loadMoreErrorSnackbar.show();

                } else if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }
            }
        };

        InfiniteScrollListener scrollListener = InfiniteScrollListener.create(messageList, 0.75f /* loadThreshold */);
        scrollListener.setEmitInitialEvent(true);

        unsubscribeOnDestroy(
                InfiniteScroller.<Message>create(scrollListener)
                        .withViewStateCallbacks(viewStateCallbacks)
                        .withStreams(paginationAnchorStream(), ((Callbacks) getActivity()).fetchMoreMessages(folder))
                        .subscribe()
        );
    }

    public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
        return messageList.canScrollVertically(upwardPagePull ? +1 : -1);
    }

    /**
     * Find the full-name of the last message in the list, that can be used as the anchor for
     * fetching the next set of pages.
     */
    private Single<PaginationAnchor> paginationAnchorStream() {
        return Single.fromCallable(() -> {
            if (messagesAdapter.getItemCount() > 0) {
                List<Message> existingMessages = messagesAdapter.getData();
                Message lastMessage = existingMessages.get(existingMessages.size() - 1);

                // Private messages can have nested replies. Go through them and find the last one.
                if (lastMessage instanceof PrivateMessage) {
                    JsonNode repliesNode = lastMessage.getDataNode().get("replies");
                    if (repliesNode.isObject()) {
                        // Replies are present.
                        //noinspection MismatchedQueryAndUpdateOfCollection
                        List<Message> lastMessageReplies = new Listing<>(repliesNode.get("data"), Message.class);
                        Message lastMessageLastReply = lastMessageReplies.get(lastMessageReplies.size() - 1);
                        return PaginationAnchor.create(lastMessageLastReply.getFullName());
                    }
                }

                return PaginationAnchor.create(lastMessage.getFullName());

            } else {
                return PaginationAnchor.createEmpty();
            }
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

}
