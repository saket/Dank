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

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
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

    interface Callbacks {
        BehaviorRelay<List<Message>> getMessages(MessageFolder folder);

        BetterLinkMovementMethod getMessageLinkMovementMethod();
    }

    public static MessageFolderFragment create(MessageFolder folder) {
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

    public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
        return messageList.canScrollVertically(upwardPagePull ? +1 : -1);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageList.setItemAnimator(new DefaultItemAnimator());

        MessagesAdapter messagesAdapter = new MessagesAdapter(((Callbacks) getActivity()).getMessageLinkMovementMethod());
        messageList.setAdapter(messagesAdapter);

        MessageFolder folder = (MessageFolder) getArguments().getSerializable(KEY_FOLDER);

        // Subscribe to messages.
        if (folder == MessageFolder.POST_REPLIES) {
            InfiniteScroller infiniteScroller = new InfiniteScroller(messageList, 0.75f);
            infiniteScroller
                    .emitWhenLoadNeeded()
                    .observeOn(io())
                    .doOnNext(o -> infiniteScroller.setLoadOngoing(true))
                    .concatMap(o -> loadNewItems())
                    .scan((existingMessages, newMessages) -> {
                        ArrayList<Message> mergedMessages = new ArrayList<>(existingMessages.size() + newMessages.size());
                        mergedMessages.addAll(existingMessages);
                        mergedMessages.addAll(newMessages);
                        return mergedMessages;
                    })
                    .observeOn(mainThread())
                    .doOnNext(m -> Timber.i("Fetched %d messages", m.size()))
                    .doOnNext(o -> infiniteScroller.setLoadOngoing(false))
                    .doOnNext(o -> firstLoadProgressView.setVisibility(View.GONE))
                    .doOnError(o -> firstLoadProgressView.setVisibility(View.GONE))
                    .subscribe(messages -> messagesAdapter.updateData(messages));

        } else {
            unsubscribeOnDestroy(((Callbacks) getActivity())
                    .getMessages(folder)
                    .doOnNext(o -> firstLoadProgressView.setVisibility(View.GONE))
                    .doOnError(o -> firstLoadProgressView.setVisibility(View.GONE))
                    .subscribe(messagesAdapter)
            );
        }
    }

    // Post replies.
    InboxPaginator postRepliesPaginator = Dank.reddit().userMessages(MessageFolder.POST_REPLIES);
    Observable<List<Message>> postRepliesStream;

    {
        postRepliesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);
    }

    private Observable<List<Message>> loadNewItems() {
        if (postRepliesPaginator.hasNext()) {
            if (postRepliesStream == null) {
                postRepliesStream = Observable.fromCallable(() -> {
                    Timber.i("Loading more…");
                    List<Message> postReplies = new ArrayList<>();

                    // TODO: Auth.
                    // Fetch a minimum of 10 post replies.
                    for (Listing<Message> messages : postRepliesPaginator) {
                        for (Message message : messages) {
                            if ("post reply".equals(message.getSubject())) {
                                postReplies.add(message);
                            }
                        }
                        if (postReplies.size() > 10) {
                            break;
                        }
                    }
                    return postReplies;
                });
            }
            return postRepliesStream;

        } else {
            return Observable.never();
        }
    }

}
