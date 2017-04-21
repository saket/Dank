package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.jakewharton.rxrelay.BehaviorRelay;

import net.dean.jraw.models.Message;
import net.dean.jraw.paginators.InboxPaginator;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import rx.Single;

public class MessagesActivity extends DankPullCollapsibleActivity implements MessageFolderFragment.Callbacks {

    @BindView(R.id.messages_root) IndependentExpandablePageLayout contentPage;
    @BindView(R.id.messages_tablayout) TabLayout tabLayout;
    @BindView(R.id.messages_viewpager) ViewPager viewPager;

    private BehaviorRelay<List<Message>> unreadMessagesRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> privateMessagesRelay = BehaviorRelay.create();
    private BehaviorRelay<List<Message>> usernameMentionsRelay = BehaviorRelay.create();

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

        // Unread messages.
        InboxPaginator unreadMessagesPaginator = Dank.reddit().userMessages(MessageFolder.UNREAD);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> unreadMessagesPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        messages -> unreadMessagesRelay.call(messages),
                        logError("Couldn't load unread messages")
                )
        );

        // Private messages.
        InboxPaginator privateMessagesPaginator = Dank.reddit().userMessages(MessageFolder.PRIVATE_MESSAGES);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> privateMessagesPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        messages -> privateMessagesRelay.call(messages),
                        logError("Couldn't load private messages")
                )
        );

        // Username mentions.
        InboxPaginator usernameMentionsPaginator = Dank.reddit().userMessages(MessageFolder.USERNAME_MENTIONS);
        unsubscribeOnDestroy(Dank.reddit()
                .withAuth(Single.fromCallable(() -> usernameMentionsPaginator.next(true)))
                .compose(applySchedulersSingle())
                .subscribe(
                        messages -> usernameMentionsRelay.call(messages),
                        logError("Couldn't load username mentions")
                )
        );
    }

    @Override
    public BehaviorRelay<List<Message>> messages(MessageFolder folder) {
        switch (folder) {
            case UNREAD:
                return unreadMessagesRelay;

            case PRIVATE_MESSAGES:
                return privateMessagesRelay;

            case USERNAME_MENTIONS:
                return usernameMentionsRelay;

            default:
                throw new UnsupportedOperationException("Unknwon message folder: " + folder);
        }
    }

    public static class MessagesPagerAdapter extends FragmentStatePagerAdapter {
        private MessageFolder[] folders = { MessageFolder.UNREAD, MessageFolder.PRIVATE_MESSAGES, MessageFolder.USERNAME_MENTIONS };
        private MessageFolderFragment activeFragment;
        private Resources resources;

        public MessagesPagerAdapter(Resources resources, FragmentManager manager) {
            super(manager);
            this.resources = resources;
        }

        @Override
        public Fragment getItem(int position) {
            return MessageFolderFragment.create(folders[position]);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            activeFragment = ((MessageFolderFragment) object);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return resources.getString(folders[position].titleRes());
        }

        public MessageFolderFragment getActiveFragment() {
            return activeFragment;
        }
    }

}
