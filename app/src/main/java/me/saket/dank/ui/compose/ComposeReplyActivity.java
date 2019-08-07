package me.saket.dank.ui.compose;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Preconditions.checkNotNull;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ScrollView;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import com.werdpressed.partisan.rundo.RunDo;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownHints;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.giphy.GiphyPickerActivity;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.SimpleTextWatcher;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.ImageButtonWithDisabledTint;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * For composing comments and message replies. Handles saving and retaining drafts. Sends the composed message back to the caller.
 */
public class ComposeReplyActivity extends DankPullCollapsibleActivity
    implements OnLinkInsertListener, RunDo.TextLink, InsertGifDialog.OnGifInsertListener
{

  private static final String KEY_START_OPTIONS = "startOptions";
  private static final String KEY_COMPOSE_RESULT = "composeResult";
  private static final int REQUEST_CODE_PICK_IMAGE = 98;
  private static final int REQUEST_CODE_PICK_GIF = 99;

  @BindView(R.id.composereply_root) IndependentExpandablePageLayout pageLayout;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.composereply_compose_field_scrollview) ScrollView replyScrollView;
  @BindView(R.id.composereply_compose_field) EditText replyField;
  @BindView(R.id.composereply_format_toolbar) TextFormatToolbarView formatToolbarView;
  @BindView(R.id.composereply_undo) ImageButtonWithDisabledTint undoButton;
  @BindView(R.id.composereply_redo) ImageButtonWithDisabledTint redoButton;

  @Inject Lazy<ReplyRepository> replyRepository;
  @Inject Lazy<MarkdownSpanPool> markdownSpanPool;
  @Inject Lazy<MarkdownHintOptions> markdownHintOptions;

  private ComposeStartOptions startOptions;
  private RunDo runDo;
  private Relay<Object> successfulSendStream = PublishRelay.create();

  private EmojiPopup emojiMenu;

  @CheckResult
  public static Intent intent(Context context, ComposeStartOptions startOptions) {
    Intent intent = new Intent(context, ComposeReplyActivity.class);
    intent.putExtra(KEY_START_OPTIONS, startOptions);
    return intent;
  }

  public static ComposeResult extractActivityResult(Intent data) {
    return data.getParcelableExtra(KEY_COMPOSE_RESULT);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_compose_reply);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    startOptions = getIntent().getParcelableExtra(KEY_START_OPTIONS);

    if (startOptions.secondPartyName() != null) {
      setTitle(getString(R.string.composereply_title_reply_to, startOptions.secondPartyName()));
    } else {
      setTitle(R.string.composereply_empty_title_placeholder);
    }
    toolbar.setNavigationOnClickListener(o -> onBackPressed());

    setupContentExpandablePage(pageLayout);
    expandFromBelowToolbar();
    pageLayout.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(replyScrollView));
  }

  @Override
  public void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    // Highlight markdown syntax.
    // Note: We'll have to remove MarkdownHintOptions from Dagger graph when we introduce a light theme.
    replyField.addTextChangedListener(new MarkdownHints(replyField, markdownHintOptions.get(), markdownSpanPool.get()));

    // Callers never send any pre-filled text. Drafts are used as the single
    // source of truth for both inline and full-screen replies.
    replyRepository.get().streamDrafts(startOptions.draftKey())
        .firstElement()
        .subscribeOn(io())
        .observeOn(mainThread())
        .subscribe(draft -> replyField.getText().replace(0, replyField.getText().length(), draft));

    // Save draft before exiting, unless the message is being launched… into the space!
    lifecycle().onStop()
        .takeUntil(successfulSendStream)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> {
          String draft = replyField.getText().toString();
          replyRepository.get().saveDraft(startOptions.draftKey(), draft)
              .subscribeOn(io())
              .subscribe();
        });

    // Undo and redo.
    runDo = RunDo.Factory.getInstance(getSupportFragmentManager());
    runDo.setTimerLength(1);
    runDo.setQueueSize(50);

    // Show undo only when some text has been entered.
    // Show redo only when undo has been pressed once.
    undoButton.setEnabled(false);
    undoButton.setOnClickListener(o -> {
      runDo.undo();
      redoButton.setEnabled(true);
    });
    replyField.addTextChangedListener(new SimpleTextWatcher() {
      @Override
      public void afterTextChanged(Editable s) {
        undoButton.setEnabled(true);
        replyField.post(() -> replyField.removeTextChangedListener(this));
      }
    });
    redoButton.setEnabled(false);
    redoButton.setOnClickListener(o -> runDo.redo());

    formatToolbarView.setActionClickListener((view, markdownAction, markdownBlock) -> {
      //Timber.d("--------------------------");
      //Timber.i("%s: %s", markdownAction, markdownBlock);

      switch (markdownAction) {
        case INSERT_TEXT_EMOJI:
          emojiMenu = new EmojiPopup(this, view, R.array.compose_reply_unicode_emojis);
          emojiMenu.show();

          lifecycle().onStop()
              .take(1)
              .subscribe(o -> emojiMenu.dismiss());

          emojiMenu.streamEmojiSelections()
              .takeUntil(lifecycle().onDestroy())
              .subscribe(emoji -> {
                replyField.getText().replace(replyField.getSelectionStart(), replyField.getSelectionEnd(), emoji);
              });
          break;

        case INSERT_LINK:
          // preFilledTitle will be empty when there's no text selected.
          int selectionStart = Math.min(replyField.getSelectionStart(), replyField.getSelectionEnd());
          int selectionEnd = Math.max(replyField.getSelectionStart(), replyField.getSelectionEnd());
          CharSequence preFilledTitle = replyField.getText().subSequence(selectionStart, selectionEnd);
          AddLinkDialog.showPreFilled(getSupportFragmentManager(), preFilledTitle.toString());
          break;

        case INSERT_IMAGE:
          Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
          intent.setType("image/*");
          startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
          break;

        case INSERT_GIF:
          startActivityForResult(GiphyPickerActivity.intent(this), REQUEST_CODE_PICK_GIF);
          break;

        case QUOTE:
        case HEADING:
          checkNotNull(markdownBlock, "markdownBlock == null");
          insertQuoteOrHeadingMarkdownSyntax(markdownBlock);
          break;

        case SPOILER:
        default:
          if (markdownBlock == null) {
            throw new AssertionError();
          }
          insertMarkdownSyntax(markdownBlock);
          break;
      }
    });
  }

  @Override
  public EditText getEditTextForRunDo() {
    return replyField;
  }

  /**
   * Insert '>' or '#' at the starting of the line and delete extra space when nesting.
   */
  private void insertQuoteOrHeadingMarkdownSyntax(MarkdownBlock markdownBlock) {
    char syntax = markdownBlock.prefix().charAt(0);   // '>' or '#'.

    // To keep things simple, we'll always insert the quote at the beginning.
    Layout layout = replyField.getLayout();
    Editable text = replyField.getText();
    int currentLineIndex = layout.getLineForOffset(replyField.getSelectionStart());
    int textOffsetOfCurrentLine = layout.getLineStart(currentLineIndex);

    CharSequence currentLine = text.subSequence(textOffsetOfCurrentLine, text.length());
    boolean isCurrentLineNonEmpty = currentLine.length() > 0;
    boolean isNestingSyntax = isCurrentLineNonEmpty && currentLine.charAt(0) == syntax;

    int selectionStartCopy = replyField.getSelectionStart();
    int selectionEndCopy = replyField.getSelectionEnd();

    replyField.setSelection(textOffsetOfCurrentLine);
    insertMarkdownSyntax(markdownBlock);
    int quoteSyntaxLength = markdownBlock.prefix().length();
    replyField.setSelection(selectionStartCopy + quoteSyntaxLength, selectionEndCopy + quoteSyntaxLength);

    // Next, delete extra spaces between nested quotes/heading.
    if (isNestingSyntax) {
      text.delete(textOffsetOfCurrentLine + 1, textOffsetOfCurrentLine + 2);
    }
  }

  private void insertMarkdownSyntax(MarkdownBlock markdownBlock) {
    boolean isSomeTextSelected = replyField.getSelectionStart() != replyField.getSelectionEnd();
    Editable text = replyField.getText();
    if (isSomeTextSelected) {
      int selectionStart = replyField.getSelectionStart();
      int selectionEnd = replyField.getSelectionEnd();

      //Timber.i("selectionStart: %s", selectionStart);
      //Timber.i("selectionEnd: %s", selectionEnd);

      text.insert(selectionStart, markdownBlock.prefix());
      text.insert(selectionEnd + markdownBlock.prefix().length(), markdownBlock.suffix());
      replyField.setSelection(selectionStart + markdownBlock.prefix().length(), selectionEnd + markdownBlock.prefix().length());

    } else {
      //noinspection ConstantConditions
      text.insert(replyField.getSelectionStart(), markdownBlock.prefix() + markdownBlock.suffix());
      replyField.setSelection(replyField.getSelectionStart() - markdownBlock.suffix().length());
    }
  }

  private void onGifSelect(GiphyGif giphyGif) {
    InsertGifDialog.show(getSupportFragmentManager(), giphyGif);
  }

  @Override
  public void onGifInsert(String title, GiphyGif gif, @Nullable Parcelable payload) {
    onLinkInsert(title, gif.url());
  }

  @Override
  public void onLinkInsert(String title, String url) {
    int selectionStart = Math.min(replyField.getSelectionStart(), replyField.getSelectionEnd());
    int selectionEnd = Math.max(replyField.getSelectionStart(), replyField.getSelectionEnd());

    String linkMarkdown = title.isEmpty()
        ? url
        : String.format("[%s](%s)", title, url);
    replyField.getText().replace(selectionStart, selectionEnd, linkMarkdown);

    // Keyboard might have gotten dismissed while the GIF list was being scrolled.
    // Works only if called delayed. Posting to reply field's message queue works.
    replyField.post(() -> Keyboards.show(replyField));
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_PICK_IMAGE) {
      if (resultCode == Activity.RESULT_OK) {
        //noinspection ConstantConditions
        lifecycle().onResume()
            .take(1)
            .takeUntil(lifecycle().onPause())
            .subscribe(o -> UploadImageDialog.show(getSupportFragmentManager(), data.getData()));
      }

    } else if (requestCode == REQUEST_CODE_PICK_GIF) {
      if (resultCode == Activity.RESULT_OK) {
        GiphyGif selectedGif = GiphyPickerActivity.extractPickedGif(data);
        onGifSelect(selectedGif);
      }

    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @OnClick(R.id.composereply_send)
  void onClickSend() {
    if (!isReplyValid()) {
      return;
    }

    CharSequence reply = replyField.getText();
    ComposeResult composeResult = ComposeResult.create(startOptions.optionalParent(), reply, startOptions.extras());

    Intent resultData = new Intent();
    resultData.putExtra(KEY_COMPOSE_RESULT, composeResult);
    setResult(RESULT_OK, resultData);
    finish();

    successfulSendStream.accept(LifecycleStreams.NOTHING);
  }

  private boolean isReplyValid() {
    String reply = replyField.getText().toString();
    if (reply.isEmpty()) {
      replyField.setError(getString(R.string.composereply_error_empty_field));
      return false;

    } else {
      replyField.setError(null);
      return true;
    }
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
