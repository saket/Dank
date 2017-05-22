package me.saket.dank.ui.user;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class UserProfileActivity extends DankPullCollapsibleActivity {

  private static final String KEY_USERNAME = "usernameLink";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userprofile_root) IndependentExpandablePageLayout contentPage;

  /**
   * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
   */
  public static void start(Context context, String userName, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, UserProfileActivity.class);
    intent.putExtra(KEY_USERNAME, userName);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_user_profile);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(contentPage);
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));

    String username = getIntent().getStringExtra(KEY_USERNAME);
    setTitle(getString(R.string.user_name_u_prefix, username));
  }

}
