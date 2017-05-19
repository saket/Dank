package me.saket.dank.ui.user.messages;

import static me.saket.dank.utils.Views.touchLiesOn;

import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.jakewharton.rxbinding2.support.v4.view.RxViewPager;

import java.util.HashSet;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.Link;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.OpenUrlActivity;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.UrlParser;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

public class InboxActivity extends DankPullCollapsibleActivity implements InboxFolderFragment.Callbacks {

  @BindView(R.id.inbox_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.inbox_tablayout) TabLayout tabLayout;
  @BindView(R.id.inbox_viewpager) ViewPager viewPager;

  private Set<InboxFolder> firstRefreshDoneForFolders = new HashSet<>(InboxFolder.ALL.length);
  private DankLinkMovementMethod commentLinkMovementMethod;
  private InboxPagerAdapter inboxPagerAdapter;

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

    inboxPagerAdapter = new InboxPagerAdapter(getResources(), getSupportFragmentManager());
    viewPager.setAdapter(inboxPagerAdapter);
    tabLayout.setupWithViewPager(viewPager, true);

    RxViewPager.pageSelections(viewPager).subscribe(o -> invalidateOptionsMenu());

    contentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return touchLiesOn(viewPager, downX, downY) && inboxPagerAdapter.getActiveFragment().shouldInterceptPullToCollapse(upwardPagePull);
    });
  }

  @Override
  public void setFirstRefreshDone(InboxFolder forFolder) {
    firstRefreshDoneForFolders.add(forFolder);
  }

  @Override
  public boolean isFirstRefreshDone(InboxFolder forFolder) {
    return firstRefreshDoneForFolders.contains(forFolder);
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_inbox, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_refresh_messages) {
      InboxFolderFragment activeFragment = inboxPagerAdapter.getActiveFragment();
      activeFragment.handleOnClickRefreshMenuItem();
      return true;

    } else {
      return super.onOptionsItemSelected(item);
    }
  }
}
