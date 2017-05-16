package me.saket.dank.ui.user.messages;

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
import android.util.SparseArray;
import android.view.ViewGroup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;
import com.jakewharton.rxrelay2.BehaviorRelay;

import net.dean.jraw.models.Message;

import java.util.Collections;
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
import me.saket.dank.utils.InfiniteScroller;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class InboxActivity extends DankPullCollapsibleActivity implements InboxFolderFragment.Callbacks {

  @BindView(R.id.inbox_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.inbox_tablayout) TabLayout tabLayout;
  @BindView(R.id.inbox_viewpager) ViewPager viewPager;

  private BehaviorRelay<List<Message>> unreadMessageStream = BehaviorRelay.create();
  private BehaviorRelay<List<Message>> privateMessageStream = BehaviorRelay.create();
  private BehaviorRelay<List<Message>> commentRepliesStream = BehaviorRelay.create();
  private BehaviorRelay<List<Message>> postRepliesStream = BehaviorRelay.create();
  private BehaviorRelay<List<Message>> usernameMentionStream = BehaviorRelay.create();

  private DankLinkMovementMethod commentLinkMovementMethod;

  /**
   * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
   */
  public static void start(Context context, @Nullable Rect expandFromShape) {
    context.startActivity(createStartIntent(context, expandFromShape));
  }

  public static Intent createStartIntent(Context context, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, InboxActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    return intent;
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
        InboxFolderFragment activeFragment = messagesPagerAdapter.getFragment(viewPager.getCurrentItem());
        return activeFragment.shouldInterceptPullToCollapse(upwardPagePull);
      } else {
        return false;
      }
    });

    unsubscribeOnDestroy(RxViewPager.pageSelections(viewPager)
        .scan((prevPage, newPage) -> {
          InboxFolderFragment previousFragment = messagesPagerAdapter.getFragment(prevPage);
          if (previousFragment != null) {
            previousFragment.onFragmentHiddenInPager();
          }
          return newPage;
        })
        .subscribe()
    );
  }

  /**
   * These local streams are used as a cache of messages loaded so far, so that we can
   * combine all message pages loaded so far and emit them to the fragment subscribers.
   */
  @Override
  public BehaviorRelay<List<Message>> messageStream(InboxFolder folder) {
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
  public InfiniteScroller.InfiniteDataStreamFunction<Message> fetchMoreMessages(InboxFolder folder) {
    return (paginationAnchor, skipCache) -> {
      BehaviorRelay<List<Message>> folderStream = messageStream(folder);
      MessageCacheKey cacheKey = MessageCacheKey.create(folder, paginationAnchor);

      Single<List<Message>> storeStream;
      if (skipCache) {
        storeStream = Dank.stores().messageStore().fetch(cacheKey);
        //.map(m -> m.subList(m.size() - Math.min(m.size(), 9), m.size() - Math.min(m.size(), 1)))
      } else {
        storeStream = Dank.stores().messageStore().get(cacheKey)
            // Using a set here to avoid duplicates. Duplicates can creep in when force-loading messages.
            .map(nextPageMessages -> ImmutableList.copyOf(new ImmutableSet.Builder<Message>()
                .addAll(folderStream.hasValue() ? folderStream.getValue() : Collections.emptyList())
                .addAll(nextPageMessages)
                .build()));
      }

      return storeStream.doOnSuccess(messages -> folderStream.accept(messages));
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
    private Resources resources;
    private SparseArray<InboxFolderFragment> fragmentMap;

    public MessagesPagerAdapter(Resources resources, FragmentManager manager) {
      super(manager);
      this.resources = resources;
      this.fragmentMap = new SparseArray<>();
    }

    @Override
    public Fragment getItem(int position) {
      return InboxFolderFragment.create(InboxFolder.ALL[position]);
    }

    @Override
    public int getCount() {
      return InboxFolder.ALL.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return resources.getString(InboxFolder.ALL[position].titleRes());
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      InboxFolderFragment fragment = (InboxFolderFragment) super.instantiateItem(container, position);
      fragmentMap.put(position, fragment);
      return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      fragmentMap.remove(position);
      super.destroyItem(container, position, object);
    }

    public InboxFolderFragment getFragment(int position) {
      return fragmentMap.get(position);
    }
  }

}
