package me.saket.dank.ui.user.messages;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.dean.jraw.models.Message;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.Subject;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
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

    private Disposable infiniteScrollDisposable;

    interface Callbacks {
        /**
         * We couldn't combine the message stream and the fetchNextMessagePage() because the
         * fragments can get recreated. So the messages fetched so far have to be cached by
         * the Activity.
         */
        Subject<List<Message>> messageStream(InboxFolder folder);

        Completable fetchNextMessagePage(InboxFolder folder);

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

        MessagesAdapter messagesAdapter = new MessagesAdapter(((Callbacks) getActivity()).getMessageLinkMovementMethod());
        messageList.setAdapter(messagesAdapter);
        messageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        messageList.setItemAnimator(new DefaultItemAnimator());

        InboxFolder folder = (InboxFolder) getArguments().getSerializable(KEY_FOLDER);

        // Listen to paginated messages.
        unsubscribeOnDestroy(((Callbacks) getActivity())
                .messageStream(folder)
                .observeOn(mainThread())
                .doOnComplete(() -> {
                    infiniteScrollDisposable.dispose();
                    firstLoadProgressView.setVisibility(View.GONE);
                })
                .doOnNext(o -> firstLoadProgressView.setVisibility(View.GONE))
                .subscribe(messagesAdapter));

        // Listen to load-more requests.
        InfiniteScroller infiniteScroller = new InfiniteScroller(messageList, 0.75f /* loadThreshold */);

        // InfiniteScroller will emit an empty item to load the initial set of items when this
        // fragment is created for the first time.
        infiniteScroller.setEmitInitialEvent(savedInstanceState == null);

        infiniteScrollDisposable = infiniteScroller
                .emitWhenLoadNeeded()
                .doOnNext(o -> infiniteScroller.setLoadOngoing(true))
                .observeOn(io())
                .flatMapCompletable(o -> ((Callbacks) getActivity()).fetchNextMessagePage(folder))
                .observeOn(mainThread())
                .doOnComplete(() -> infiniteScroller.setLoadOngoing(false))
                .subscribe(doNothingCompletable(), error -> Timber.e(error, "Couldn't fetch more messages under %s", folder));

        unsubscribeOnDestroy(infiniteScrollDisposable);
    }

    public boolean shouldInterceptPullToCollapse(boolean upwardPagePull) {
        return messageList.canScrollVertically(upwardPagePull ? +1 : -1);
    }

}
