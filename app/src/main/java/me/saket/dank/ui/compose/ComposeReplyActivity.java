package me.saket.dank.ui.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * For composing comments and message replies. Handles saving drafts.
 *
 * Compose buttons to support:
 * Bold, italic, underline
 * Strikethrough
 * Superscript, subscript,
 * quote
 * list (ordered, unordered)
 * image
 */
public class ComposeReplyActivity extends DankPullCollapsibleActivity {

  private static final String KEY_START_OPTIONS = "startOptions";

  @BindView(R.id.composereply_root) IndependentExpandablePageLayout pageLayout;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.composereply_progress) View progressView;

  public static void start(Context context, ComposeStartOptions startOptions) {
    Intent intent = new Intent(context, ComposeReplyActivity.class);
    intent.putExtra(KEY_START_OPTIONS, startOptions);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.compose_reply);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    ComposeStartOptions startOptions = getIntent().getParcelableExtra(KEY_START_OPTIONS);

    setTitle(getString(R.string.composereply_title_reply_to, startOptions.secondPartyName()));
    toolbar.setNavigationOnClickListener(o -> onBackPressed());

    setupContentExpandablePage(pageLayout);
    expandFromBelowToolbar();
  }

  @Override
  public void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    findViewById(R.id.composereply_editor_bold).setOnClickListener(o -> {

    });
    findViewById(R.id.composereply_editor_ul).setOnClickListener(o -> {

    });
    findViewById(R.id.composereply_editor_ol).setOnClickListener(o -> {

    });
    findViewById(R.id.composereply_editor_hr).setOnClickListener(o -> {

    });
  }

  @OnClick(R.id.composereply_send)
  void onClickSend() {
    progressView.setVisibility(View.VISIBLE);
  }
}
