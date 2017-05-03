package me.saket.dank.ui.user.messages;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.models.PrivateMessage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.InfiniteScroller;
import timber.log.Timber;

/**
 * Displays messages under one folder. e.g., "Unread", "Messages", "Comment replies", etc.
 */
public class MessageFolderFragment extends DankFragment {

    private static final String KEY_FOLDER = "folder";

    @BindView(R.id.messagefolder_message_list) RecyclerView messageList;
    @BindView(R.id.messagefolder_first_load_progress) View firstLoadProgressView;
    @BindView(R.id.messagefolder_empty_state) ViewGroup emptyStateContainer;
    @BindView(R.id.messagefolder_empty_state_message) TextView emptyStateMessageView;

    private MessagesAdapter messagesAdapter;
    private Disposable infiniteScrollDisposable;

    interface Callbacks {
        Function<PaginationAnchor, Single<List<Message>>> fetchMoreMessages(InboxFolder folder);

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
        messageList.setAdapter(messagesAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageList.setItemAnimator(new DefaultItemAnimator());

        InboxFolder folder = (InboxFolder) getArguments().getSerializable(KEY_FOLDER);

        // Listen to load-more requests.
        InfiniteScroller infiniteScroller = new InfiniteScroller(messageList, 0.75f /* loadThreshold */);
        infiniteScroller.setEmitInitialEvent(true);
        infiniteScrollDisposable = infiniteScroller
                .emitWhenLoadNeeded()
                .doOnNext(o -> infiniteScroller.setLoadOngoing(true))
                .observeOn(io())
                .flatMapSingle(o -> calculatePaginationAnchor())
                .scan((oldAnchor, newAnchor) -> {
                    if (oldAnchor.equals(newAnchor)) {
                        // No new pagination anchor was found. This means that
                        // no new items were fetched. Pagination can stop.
                        infiniteScrollDisposable.dispose();
                    }
                    return newAnchor;
                })
                .flatMapSingle(((Callbacks) getActivity()).fetchMoreMessages(folder))
                .observeOn(mainThread())
                .doOnNext(messages -> {
                    infiniteScroller.setLoadOngoing(false);
                    firstLoadProgressView.setVisibility(View.GONE);

                    boolean isFolderEmpty = messages.isEmpty() && messagesAdapter.getItemCount() == 0;

                    if (isFolderEmpty) {
                        // First load, no messages received. Inbox folder is empty.
                        infiniteScrollDisposable.dispose();
                    }
                    setEmptyStateVisible(isFolderEmpty, folder);
                })
                .subscribe(messagesAdapter, handleMessageLoadError(folder));
        unsubscribeOnDestroy(infiniteScrollDisposable);
    }

    public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
        return messageList.canScrollVertically(upwardPagePull ? +1 : -1);
    }

    /**
     * Find the full-name of the last message in the list, that can be used as the anchor for
     * fetching the next set of pages.
     */
    private Single<PaginationAnchor> calculatePaginationAnchor() {
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
                    return Single.just(PaginationAnchor.create(lastMessageLastReply.getFullName()));
                }
            }

            return Single.just(PaginationAnchor.create(lastMessage.getFullName()));

        } else {
            return Single.just(PaginationAnchor.createEmpty());
        }
    }

    private void setEmptyStateVisible(boolean visible, InboxFolder forFolder) {
        emptyStateContainer.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            switch (forFolder) {
                case UNREAD:
                    emptyStateMessageView.setText(R.string.inbox_empty_state_message_for_unread);
                    break;

                case PRIVATE_MESSAGES:
                    emptyStateMessageView.setText(R.string.inbox_empty_state_message_for_private_messages);
                    break;

                case COMMENT_REPLIES:
                    emptyStateMessageView.setText(R.string.inbox_empty_state_message_for_comment_replies);
                    break;

                case POST_REPLIES:
                    emptyStateMessageView.setText(R.string.inbox_empty_state_message_for_post_replies);
                    break;

                case USERNAME_MENTIONS:
                    emptyStateMessageView.setText(R.string.inbox_empty_state_message_for_username_mentions);
                    break;
            }
        }
    }

    private Consumer<Throwable> handleMessageLoadError(InboxFolder folder) {
        return error -> {
            Timber.e(error, "Couldn't fetch more messages under %s", folder);
        };
    }

}
