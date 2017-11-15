package me.saket.dank.ui.compose;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
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

    formatToolbarView.setActionClickListener((view, markdownAction, markdownBlock) -> {
      Timber.d("--------------------------");
      Timber.i("%s: %s", markdownAction, markdownBlock);

      switch (markdownAction) {
        case INSERT_TEXT_EMOJI:
          String[] unicodeEmojis = getResources().getStringArray(R.array.compose_reply_unicode_emojis);
          PopupMenu emojiMenu = new PopupMenu(this, view, Gravity.TOP);
          for (String unicodeEmoji : unicodeEmojis) {
            emojiMenu.getMenu().add(unicodeEmoji);
          }
          emojiMenu.show();
          emojiMenu.setOnMenuItemClickListener(item -> {
            replyField.getText().replace(replyField.getSelectionStart(), replyField.getSelectionEnd(), item.getTitle());
            return true;
          });
          break;

        case INSERT_LINK:
          // preFilledTitle will be empty when there's no text selected.
          CharSequence preFilledTitle = replyField.getText().subSequence(replyField.getSelectionStart(), replyField.getSelectionEnd());
          AddLinkDialog.showPreFilled(getSupportFragmentManager(), preFilledTitle.toString());
          break;

        case INSERT_IMAGE:
          // TODO.
          Timber.w("TODO: Image picker");
          break;

        case QUOTE:
        case HEADING:
          insertQuoteOrHeadingMarkdownSyntax(markdownBlock);
          break;

        default:
          insertMarkdownSyntax(markdownBlock);
          break;
      }
    });
  }

  /**
   * Inserts '>' or '#' at the starting of the line and deletes extra space when nesting.
   */
  private void insertQuoteOrHeadingMarkdownSyntax(MarkdownBlock markdownBlock) {
    char syntax = markdownBlock.prefix().charAt(0);   // '>' or '#'.

    // To keep things simple, we'll always insert the quote at the beginning.
    int currentLineIndex = replyField.getLayout().getLineForOffset(replyField.getSelectionStart());
    int currentLineStartIndex = replyField.getLayout().getLineStart(currentLineIndex);
    boolean isNestingQuotes = replyField.getText().length() > 0 && replyField.getText().charAt(currentLineStartIndex) == syntax;

    int selectionStartCopy = replyField.getSelectionStart();
    int selectionEndCopy = replyField.getSelectionEnd();

    replyField.setSelection(currentLineStartIndex);
    insertMarkdownSyntax(markdownBlock);
    //noinspection ConstantConditions
    int quoteSyntaxLength = markdownBlock.prefix().length();
    replyField.setSelection(selectionStartCopy + quoteSyntaxLength, selectionEndCopy + quoteSyntaxLength);

    // Next, delete extra spaces between nested quotes.
    if (isNestingQuotes) {
      replyField.getText().delete(currentLineStartIndex + 1, currentLineStartIndex + 2);
    }
  }

  private void insertMarkdownSyntax(MarkdownBlock markdownBlock) {
    boolean isSomeTextSelected = replyField.getSelectionStart() != replyField.getSelectionEnd();
    if (isSomeTextSelected) {
      int selectionStart = replyField.getSelectionStart();
      int selectionEnd = replyField.getSelectionEnd();

      Timber.i("selectionStart: %s", selectionStart);
      Timber.i("selectionEnd: %s", selectionEnd);

      replyField.getText().insert(selectionStart, markdownBlock.prefix());
      replyField.getText().insert(selectionEnd + markdownBlock.prefix().length(), markdownBlock.suffix());
      replyField.setSelection(selectionStart + markdownBlock.prefix().length(), selectionEnd + markdownBlock.prefix().length());

    } else {
      //noinspection ConstantConditions
      replyField.getText().insert(replyField.getSelectionStart(), markdownBlock.prefix() + markdownBlock.suffix());
      replyField.setSelection(replyField.getSelectionStart() - markdownBlock.suffix().length());
    }
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
    //getMenuInflater().inflate(R.menu.menu_compose_reply, menu);
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
