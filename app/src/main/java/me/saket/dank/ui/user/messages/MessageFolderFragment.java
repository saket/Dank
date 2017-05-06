package me.saket.dank.ui.user.messages;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

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
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.PaginationAnchor;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.exceptions.PaginationCompleteException;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankFragment;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter;
import me.saket.dank.utils.InfiniteScrollRecyclerAdapter.ProgressType;
import me.saket.dank.utils.InfiniteScrollListener;
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
    @BindView(R.id.messagefolder_error_state) ErrorStateView errorStateView;

    private InboxFolder folder;
    private MessagesAdapter messagesAdapter;
    private InfiniteScrollRecyclerAdapter messagesAdapterWithProgress;
    private Disposable infiniteScrollDisposable;
    private Snackbar loadMoreErrorSnackbar;     // TODO: Rxify this.

    private enum ScreenState {
        FRESH_LOAD_IN_FLIGHT,
        MORE_LOAD_IN_FLIGHT,
        MESSAGES_VISIBLE,
        EMPTY_STATE,
        ERROR_ON_FRESH_LOAD,
        ERROR_ON_MORE_LOAD,
    }

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
        messagesAdapterWithProgress = InfiniteScrollRecyclerAdapter.wrap(messagesAdapter);
        messageList.setAdapter(messagesAdapterWithProgress);

        messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageList.setItemAnimator(new DefaultItemAnimator());

        folder = (InboxFolder) getArguments().getSerializable(KEY_FOLDER);
        subscribeToMessageLoads();
    }

    /**
     * Called when the ViewPager is swiped and this page goes out of the view.
     */
    public void onFragmentHiddenInPager() {
        if (loadMoreErrorSnackbar != null) {
            loadMoreErrorSnackbar.dismiss();
        }
    }

    private void showScreenState(ScreenState state) {
        switch (state) {
            case FRESH_LOAD_IN_FLIGHT:
                emptyStateView.setVisibility(View.GONE);
                errorStateView.setVisibility(View.GONE);
                if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }

                boolean isFirstLoad = messagesAdapter.getItemCount() == 0;
                if (isFirstLoad) {
                    firstLoadProgressView.setVisibility(View.VISIBLE);
                } else {
                    messageList.post(() -> {
                        messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                        messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, false);
                    });
                }
                break;

            case MORE_LOAD_IN_FLIGHT:
                firstLoadProgressView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                errorStateView.setVisibility(View.GONE);
                messageList.post(() -> {
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, true);
                });
                if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }
                break;

            case MESSAGES_VISIBLE:
                firstLoadProgressView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                errorStateView.setVisibility(View.GONE);
                messageList.post(() -> {
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, false);
                });
                if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }
                break;

            case ERROR_ON_FRESH_LOAD:
                firstLoadProgressView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                errorStateView.setVisibility(View.VISIBLE);
                messageList.post(() -> {
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, false);
                });
                if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }
                break;

            case ERROR_ON_MORE_LOAD:
                firstLoadProgressView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
                errorStateView.setVisibility(View.GONE);
                messageList.post(() -> {
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, false);
                });

                if (isVisible()) {
                    loadMoreErrorSnackbar = Snackbar.make(messageList, "Failed to load more messages", Snackbar.LENGTH_INDEFINITE);
                    loadMoreErrorSnackbar.setAction(R.string.common_error_retry, v -> subscribeToMessageLoads());
                    loadMoreErrorSnackbar.show();
                }
                break;

            case EMPTY_STATE:
                firstLoadProgressView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.VISIBLE);
                errorStateView.setVisibility(View.GONE);
                messageList.post(() -> {
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.FRESH_DATA_LOAD_PROGRESS, false);
                    messagesAdapterWithProgress.setProgressVisible(ProgressType.MORE_DATA_LOAD_PROGRESS, false);
                });
                if (loadMoreErrorSnackbar != null) {
                    loadMoreErrorSnackbar.dismiss();
                }
                break;

            default:
                throw new AssertionError("Unknown state: " + state);
        }
    }

    private void subscribeToMessageLoads() {
        // Listen to load-more requests.
        InfiniteScrollListener infiniteScrollListener = InfiniteScrollListener.create(messageList, 0.75f /* loadThreshold */);
        infiniteScrollListener.setEmitInitialEvent(true);
        infiniteScrollDisposable = infiniteScrollListener.emitWhenLoadNeeded()
                .flatMapSingle(o -> calculatePaginationAnchor())
                .scan((oldAnchor, newAnchor) -> {
                    if (oldAnchor.equals(newAnchor)) {
                        // No new pagination anchor was found. This means that
                        // no new items were fetched. Pagination can stop.
                        throw new PaginationCompleteException();
                    }
                    return newAnchor;
                })
                .doOnNext(o -> showScreenState(messagesAdapter.getItemCount() == 0 ? ScreenState.FRESH_LOAD_IN_FLIGHT : ScreenState.MORE_LOAD_IN_FLIGHT))
                .doOnNext(o -> infiniteScrollListener.setLoadOngoing(true))
                .observeOn(io())
                .flatMapSingle(((Callbacks) getActivity()).fetchMoreMessages(folder))
                .observeOn(mainThread())
                .doOnNext(messages -> {
                    infiniteScrollListener.setLoadOngoing(false);

                    boolean noMessagesFoundInFolder = messages.isEmpty() && messagesAdapter.getItemCount() == 0;
                    if (noMessagesFoundInFolder) {
                        // First load, no messages received. Inbox folder is empty.
                        showScreenState(ScreenState.EMPTY_STATE);
                        showEmptyStateView(folder);
                        throw new PaginationCompleteException();

                    } else {
                        showScreenState(ScreenState.MESSAGES_VISIBLE);
                    }
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

    private void showEmptyStateView(InboxFolder forFolder) {
        emptyStateView.setEmoji(R.string.inbox_empty_state_title);

        switch (forFolder) {
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

    private Consumer<Throwable> handleMessageLoadError(InboxFolder folder) {
        return error -> {
            if ((error instanceof PaginationCompleteException)) {
                // Expected. Ignore.
                return;
            }

            ResolvedError resolvedError = Dank.errors().resolve(error);

            if (messagesAdapter.getItemCount() == 0) {
                showScreenState(ScreenState.ERROR_ON_FRESH_LOAD);
                errorStateView.applyFrom(resolvedError);
                errorStateView.setOnRetryClickListener(o -> subscribeToMessageLoads());
            } else {
                showScreenState(ScreenState.ERROR_ON_MORE_LOAD);
            }

            if (resolvedError.isUnknown()) {
                Timber.e(error, "Couldn't fetch more messages under %s", folder);
            }
        };
    }

}
