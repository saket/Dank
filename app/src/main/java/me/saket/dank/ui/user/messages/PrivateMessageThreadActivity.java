package me.saket.dank.ui.user.messages;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

public class PrivateMessageThreadActivity extends DankPullCollapsibleActivity {

  @BindView(R.id.privatemessagethread_root) IndependentExpandablePageLayout contentPage;

  public static void start(Context context, @Nullable Rect expandFromShape) {
    Intent intent = new Intent(context, PrivateMessageThreadActivity.class);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_private_message_thread);
    ButterKnife.bind(this);

    setupContentExpandablePage(contentPage);
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
  }
}
