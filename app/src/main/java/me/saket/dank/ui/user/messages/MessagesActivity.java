package me.saket.dank.ui.user.messages;

import static me.saket.dank.ui.user.messages.MessageFolder.UNREAD;
import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;
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

import com.jakewharton.rxrelay2.BehaviorRelay;

import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;
import net.dean.jraw.paginators.Paginator;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Single;
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

public class MessagesActivity extends DankPullCollapsibleActivity implements MessageFolderFragment.Callbacks {

    @BindView(R.id.messages_root) IndependentExpandablePageLayout contentPage;
    @BindView(R.id.messages_tablayout) TabLayout tabLayout;
    @BindView(R.id.messages_viewpager) ViewPager viewPager;

    private BehaviorRelay<List<Message>> unreadMessagesRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> privateMessagesRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> commentRepliesMessageRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> postRepliesMessageRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> usernameMentionsRelay = BehaviorRelay.create();
    private DankLinkMovementMethod commentLinkMovementMethod;

    /**
     * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
     */
    public static void start(Context context, @Nullable Rect expandFromShape) {
        Intent intent = new Intent(context, MessagesActivity.class);
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
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // TODO: These API calls happen in parallel and start automatically on start. Move them to on-demand loading instead.

        // Unread messages.
        InboxPaginator unreadMessagesPaginator = Dank.reddit().userMessages(UNREAD);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> unreadMessagesPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        unreads -> unreadMessagesRelay.accept(unreads),
                        logError("Couldn't load unread messages")
                )
        );

        // Private messages.
        InboxPaginator privateMessagesPaginator = Dank.reddit().userMessages(MessageFolder.PRIVATE_MESSAGES);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> privateMessagesPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        messages -> privateMessagesRelay.accept(messages),
                        logError("Couldn't load private messages")
                )
        );

        // Comment replies.
        InboxPaginator commentRepliesPaginator = Dank.reddit().userMessages(MessageFolder.COMMENT_REPLIES);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> commentRepliesPaginator.next(true)))
                //.map(RxUtils.filterItems(MessageFolder.isCommentReply()))
                .compose(applySchedulersSingle())
                .subscribe(
                        commentReplies -> commentRepliesMessageRelay.accept(commentReplies),
                        logError("Couldn't load username mentions")
                )
        );

        // Post replies.
        InboxPaginator postRepliesPaginator = Dank.reddit().userMessages(MessageFolder.POST_REPLIES);
        postRepliesPaginator.setLimit(Paginator.DEFAULT_LIMIT * 2);

        Single<List<Message>> postRepliesStream = Single.fromCallable(() -> {
            List<Message> postReplies = new ArrayList<>();

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

        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(postRepliesStream)
                .compose(applySchedulersSingle())
                .subscribe(
                        postReplies -> {
//                            postRepliesMessageRelay.accept(postReplies);
                        },
                        logError("Couldn't load username mentions")
                )
        );

        // Username mentions.
        InboxPaginator usernameMentionsPaginator = Dank.reddit().userMessages(MessageFolder.USERNAME_MENTIONS);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> usernameMentionsPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        mentions -> usernameMentionsRelay.accept(mentions),
                        logError("Couldn't load username mentions")
                )
        );
    }

    @Override
    public BehaviorRelay<List<Message>> getMessages(MessageFolder folder) {
        switch (folder) {
            case UNREAD:
                return unreadMessagesRelay;

            case PRIVATE_MESSAGES:
                return privateMessagesRelay;

            case COMMENT_REPLIES:
                return commentRepliesMessageRelay;

            case POST_REPLIES:
                return postRepliesMessageRelay;

            case USERNAME_MENTIONS:
                return usernameMentionsRelay;

            default:
                throw new UnsupportedOperationException("Unknown message folder: " + folder);
        }
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
            return MessageFolderFragment.create(MessageFolder.ALL[position]);
        }

        @Override
        public int getCount() {
            return MessageFolder.ALL.length;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            activeFragment = ((MessageFolderFragment) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return resources.getString(MessageFolder.ALL[position].titleRes());
        }

        public MessageFolderFragment getActiveFragment() {
            return activeFragment;
        }
    }

}
