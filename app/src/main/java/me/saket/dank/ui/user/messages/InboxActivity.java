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
import android.view.ViewGroup;

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
      //noinspection CodeBlock2Expr
      return touchLiesOn(viewPager, downX, downY) && messagesPagerAdapter.getActiveFragment().shouldInterceptPullToCollapse(upwardPagePull);
    });
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
    private InboxFolderFragment activeFragment;

    public MessagesPagerAdapter(Resources resources, FragmentManager manager) {
      super(manager);
      this.resources = resources;
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
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
      super.setPrimaryItem(container, position, object);
      activeFragment = (InboxFolderFragment) object;
    }

    public InboxFolderFragment getActiveFragment() {
      return activeFragment;
    }
  }
}
