package me.saket.dank.ui.user.messages;

import static me.saket.dank.ui.user.messages.InboxFolder.COMMENT_REPLIES;
import static me.saket.dank.ui.user.messages.InboxFolder.POST_REPLIES;
import static me.saket.dank.ui.user.messages.InboxFolder.PRIVATE_MESSAGES;
import static me.saket.dank.ui.user.messages.InboxFolder.UNREAD;
import static me.saket.dank.ui.user.messages.InboxFolder.USERNAME_MENTIONS;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableList;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.subjects.ReplaySubject;
import io.reactivex.subjects.Subject;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.OpenUrlActivity;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class InboxActivity extends DankPullCollapsibleActivity implements MessageFolderFragment.Callbacks {

    @BindView(R.id.inbox_root) IndependentExpandablePageLayout contentPage;
    @BindView(R.id.inbox_tablayout) TabLayout tabLayout;
    @BindView(R.id.inbox_viewpager) ViewPager viewPager;

    private ReplaySubject<List<Message>> unreadMessageStream = ReplaySubject.create();
    private ReplaySubject<List<Message>> privateMessageStream = ReplaySubject.create();
    private ReplaySubject<List<Message>> commentRepliesStream = ReplaySubject.create();
    private ReplaySubject<List<Message>> postRepliesStream = ReplaySubject.create();
    private ReplaySubject<List<Message>> usernameMentionStream = ReplaySubject.create();

    private DankLinkMovementMethod commentLinkMovementMethod;
    private InboxPaginator unreadMessagesPaginator;
    private InboxPaginator privateMessagesPaginator;
    private InboxPaginator commentRepliesPaginator;
    private InboxPaginator postRepliesPaginator;
    private InboxPaginator usernameMentionsPaginator;

    /**
     * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
     */
    public static void start(Context context, @Nullable Rect expandFromShape) {
        Intent intent = new Intent(context, InboxActivity.class);
        intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        ButterKnife.bind(this);
        findAndSetupToolbar();

        setupContentExpandablePage(contentPage);
        expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));

        MessagesPagerAdapter messagesPagerAdapter = new MessagesPagerAdapter(getResources(), getSupportFragmentManager());
        viewPager.setAdapter(messagesPagerAdapter);
        tabLayout.setupWithViewPager(viewPager, true);

        contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
            //noinspection SimplifiableIfStatement
            if (touchLiesOn(viewPager, downX, downY)) {
                MessageFolderFragment activeFragment = messagesPagerAdapter.getActiveFragment();
                return activeFragment.shouldInterceptPullToCollapse(upwardPagePull);
            } else {
                return false;
            }
        });

        // Unread messages.
        unreadMessagesPaginator = Dank.reddit().userMessages(UNREAD);
        unreadMessagesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

        // Private messages.
        privateMessagesPaginator = Dank.reddit().userMessages(PRIVATE_MESSAGES);
        privateMessagesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

        // Comment replies.
        commentRepliesPaginator = Dank.reddit().userMessages(COMMENT_REPLIES);
        commentRepliesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

        // Post replies.
        postRepliesPaginator = Dank.reddit().userMessages(POST_REPLIES);
        postRepliesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

        // Username mentions.
        usernameMentionsPaginator = Dank.reddit().userMessages(USERNAME_MENTIONS);
        usernameMentionsPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);
    }

    @Override
    public Subject<List<Message>> messageStream(InboxFolder folder) {
        switch (folder) {
            case UNREAD:
                return unreadMessageStream;

            case PRIVATE_MESSAGES:
                return privateMessageStream;

            case COMMENT_REPLIES:
                return commentRepliesStream;

            case POST_REPLIES:
                return postRepliesStream;

            case USERNAME_MENTIONS:
                return usernameMentionStream;

            default:
                throw new UnsupportedOperationException("Unknown message folder: " + folder);
        }
    }

    @Override
    public Completable fetchNextMessagePage(InboxFolder folder) {
        switch (folder) {
            case UNREAD:
                return Dank.reddit()
                        .withAuth(Observable.fromCallable(() -> unreadMessagesPaginator.next(true)))
                        .compose(propagatePaginatedItemsToStream(unreadMessagesPaginator, unreadMessageStream))
                        .ignoreElements();

            case PRIVATE_MESSAGES:
                return Dank.reddit()
                        .withAuth(Observable.fromCallable(() -> privateMessagesPaginator.next(true)))
                        .compose(propagatePaginatedItemsToStream(privateMessagesPaginator, privateMessageStream))
                        .ignoreElements();

            case COMMENT_REPLIES:
                return Dank.reddit()
                        .withAuth(Observable.fromCallable(() -> {
                            // Fetch a minimum of 10 comment replies.
                            List<Message> commentReplies = new ArrayList<>();

                            // commentRepliesPaginator makes an API call on every iteration.
                            for (Listing<Message> messages : commentRepliesPaginator) {
                                for (Message message : messages) {
                                    if ("comment reply".equals(message.getSubject())) {
                                        commentReplies.add(message);
                                    }
                                }
                                if (commentReplies.size() > 10) {
                                    break;
                                }
                            }
                            return commentReplies;
                        }))
                        .compose(propagatePaginatedItemsToStream(commentRepliesPaginator, commentRepliesStream))
                        .ignoreElements();

            case POST_REPLIES:
                return Dank.reddit()
                        .withAuth(Observable.fromCallable(() -> {
                            // Fetch a minimum of 10 post replies.
                            List<Message> postReplies = new ArrayList<>();
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
                        }))
                        .compose(propagatePaginatedItemsToStream(postRepliesPaginator, postRepliesStream))
                        .ignoreElements();

            case USERNAME_MENTIONS:
                return Dank.reddit()
                        .withAuth(Observable.fromCallable(() -> usernameMentionsPaginator.next(true)))
                        .compose(propagatePaginatedItemsToStream(usernameMentionsPaginator, usernameMentionStream))
                        .ignoreElements();

            default:
                throw new UnsupportedOperationException("Unknown message folder: " + folder);
        }
    }

    private static ObservableTransformer<List<Message>, List<Message>> propagatePaginatedItemsToStream(InboxPaginator messagesPaginator,
            ReplaySubject<List<Message>> stream)
    {
        return newMessagesObservable -> {
            //noinspection unchecked
            return newMessagesObservable
                    .map(nextPage -> (List<Message>) new ImmutableList.Builder<Message>()
                            .addAll(stream.hasValue() ? stream.getValue() : Collections.emptyList())
                            .addAll(nextPage)
                            .build()
                    )
                    .doOnNext(messages -> stream.onNext(messages))
                    .doAfterNext(o -> {
                        if (!messagesPaginator.hasNext()) {
                            stream.onComplete();
                        }
                    });
        };
    }

    @Override
    public BetterLinkMovementMethod getMessageLinkMovementMethod() {
        if (commentLinkMovementMethod == null) {
            commentLinkMovementMethod = DankLinkMovementMethod.newInstance();
            commentLinkMovementMethod.setOnLinkClickListener((textView, url) -> {
                // TODO: 18/03/17 Remove try/catch block
                try {
                    Link parsedLink = UrlParser.parse(url);
                    Point clickedUrlCoordinates = commentLinkMovementMethod.getLastUrlClickCoordinates();
                    int deviceDisplayWidth = getResources().getDisplayMetrics().widthPixels;
                    Rect clickedUrlCoordinatesRect = new Rect(0, clickedUrlCoordinates.y, deviceDisplayWidth, clickedUrlCoordinates.y);
                    OpenUrlActivity.handle(this, parsedLink, clickedUrlCoordinatesRect);
                    return true;

                } catch (Exception e) {
                    Timber.i(e, "Couldn't parse URL: %s", url);
                    return false;
                }
            });
        }
        return commentLinkMovementMethod;
    }

    public static class MessagesPagerAdapter extends FragmentStatePagerAdapter {
        private MessageFolderFragment activeFragment;
        private Resources resources;

        public MessagesPagerAdapter(Resources resources, FragmentManager manager) {
            super(manager);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int position) {
            return MessageFolderFragment.create(InboxFolder.ALL[position]);
        }

        @Override
        public int getCount() {
            return InboxFolder.ALL.length;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            activeFragment = ((MessageFolderFragment) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return resources.getString(InboxFolder.ALL[position].titleRes());
        }

        public MessageFolderFragment getActiveFragment() {
            return activeFragment;
        }
    }

}
