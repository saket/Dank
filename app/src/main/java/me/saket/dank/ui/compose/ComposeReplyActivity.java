package me.saket.dank.ui.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

/**
 * For composing comments and message replies. Handles saving drafts.
 * <p>
 * Formatting buttons to support:
 * - Bold, italic
 * - Strikethrough
 * - Superscript
 * - Quote
 * - List (ordered, unordered)
 * - Image
 * - Headers
 * - Link
 * - Code
 * - HR
 * <p>
 * Complex stuff:
 * - Nested subscripts and superscripts
 * - Nested quotes
 * - Multiple words in subscript/superscript. E.g., "This sentence^ (has a superscript with multiple words)"
 * - Nested list
 * - Table
 */
public class ComposeReplyActivity extends DankPullCollapsibleActivity implements AddLinkDialog.Callbacks {

  private static final String KEY_START_OPTIONS = "startOptions";

  @BindView(R.id.composereply_root) IndependentExpandablePageLayout pageLayout;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.composereply_progress) View progressView;
  @BindView(R.id.composereply_compose_field_scrollview) ScrollView replyScrollView;
  @BindView(R.id.composereply_compose_field) EditText replyField;
  @BindView(R.id.composereply_format_toolbar) TextFormatToolbarView formatToolbarView;

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

    pageLayout.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return Views.touchLiesOn(replyScrollView, downX, downY) && replyScrollView.canScrollVertically(upwardPagePull ? 1 : -1);
    });
  }

  @Override
  public void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    formatToolbarView.setActionClickListener((markdownAction, markdownBlock) -> {
      Timber.d("--------------------------");
      Timber.i("%s: %s", markdownAction, markdownBlock);

      switch (markdownAction) {
        case INSERT_TEXT_EMOJI:
          Timber.w("TODO: Emojis");
          break;

        case INSERT_LINK:
          CharSequence preFilledTitle = replyField.getText().subSequence(replyField.getSelectionStart(), replyField.getSelectionEnd());
          AddLinkDialog.showPreFilled(getSupportFragmentManager(), preFilledTitle.toString());
          break;

        case INSERT_IMAGE:
          Timber.w("TODO: Image picker");
          break;

        default:
          if (replyField.getSelectionStart() != replyField.getSelectionEnd()) {
            // Some text is selected.
            int selectionStart = replyField.getSelectionStart();
            int selectionEnd = replyField.getSelectionEnd();

            //noinspection ConstantConditions
            replyField.getText().insert(selectionStart, markdownBlock.prefix());
            replyField.getText().insert(selectionEnd + markdownBlock.prefix().length(), markdownBlock.prefix());
            replyField.setSelection(selectionStart + markdownBlock.prefix().length(), selectionEnd + markdownBlock.prefix().length());

          } else {
            //noinspection ConstantConditions
            replyField.getText().insert(replyField.getSelectionStart(), markdownBlock.prefix() + markdownBlock.suffix());
            replyField.setSelection(replyField.getSelectionStart() - markdownBlock.suffix().length());
          }
          break;
      }
    });
  }

  @Override
  public void onLinkInsert(String title, String url) {
    int selectionStart = replyField.getSelectionStart();
    int selectionEnd = replyField.getSelectionEnd();
    String linkMarkdown = String.format("[%s](%s)", title, url);
    replyField.getText().replace(selectionStart, selectionEnd, linkMarkdown);
  }

  @OnClick(R.id.composereply_send)
  void onClickSend() {
    progressView.setVisibility(View.VISIBLE);
  }

// ======== TOOLBAR MENU ======== //

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_compose_reply, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
