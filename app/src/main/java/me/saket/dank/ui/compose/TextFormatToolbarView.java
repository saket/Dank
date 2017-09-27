package me.saket.dank.ui.compose;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.HorizontalScrollView;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;

public class TextFormatToolbarView extends HorizontalScrollView {

  private ActionClickListener actionClickListener;

  public interface ActionClickListener {
    /**
     * @param block Nullable for insert-link, insert-text-emoji and insert-image.
     */
    void onClickAction(MarkdownAction action, @Nullable MarkdownBlock block);
  }

  public TextFormatToolbarView(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.custom_text_formatting_toolbar, this, true);
    ButterKnife.bind(this);
  }

  public void setActionClickListener(ActionClickListener listener) {
    actionClickListener = listener;
  }

  @OnClick(R.id.textformattoolbar_bold)
  void onClickBold() {
    actionClickListener.onClickAction(MarkdownAction.BOLD, MarkdownBlock.create("**", "**"));
  }

  @OnClick(R.id.textformattoolbar_italic)
  void onClickItalic() {
    actionClickListener.onClickAction(MarkdownAction.ITALIC, MarkdownBlock.create("*", "*"));
  }

  @OnClick(R.id.textformattoolbar_insert_text_emoji)
  void onClickInsertTextEmoji() {
    actionClickListener.onClickAction(MarkdownAction.INSERT_TEXT_EMOJI, null);
  }

  @OnClick(R.id.textformattoolbar_insert_link)
  void onClickInsertLink() {
    actionClickListener.onClickAction(MarkdownAction.INSERT_LINK, null);
  }

  @OnClick(R.id.textformattoolbar_insert_image)
  void onClickInsertImage() {
    actionClickListener.onClickAction(MarkdownAction.INSERT_IMAGE, null);
  }

  @OnClick(R.id.textformattoolbar_strikethrough)
  void onClickStrikeThrough() {
    actionClickListener.onClickAction(MarkdownAction.STRIKE_THROUGH, MarkdownBlock.create("~~", "~~"));
  }

  @OnClick(R.id.textformattoolbar_quote)
  void onClickQuote() {
    actionClickListener.onClickAction(MarkdownAction.QUOTE, MarkdownBlock.create(">"));
  }

  @OnClick(R.id.textformattoolbar_superscript)
  void onClickSuperscript() {
    actionClickListener.onClickAction(MarkdownAction.SUPERSCRIPT, MarkdownBlock.create("^"));
  }

  @OnClick(R.id.textformattoolbar_inline_code)
  void onClickInlineCode() {
    actionClickListener.onClickAction(MarkdownAction.INLINE_CODE, MarkdownBlock.create("`", "`"));
  }

  @OnClick(R.id.textformattoolbar_header)
  void onClickHeader() {
    actionClickListener.onClickAction(MarkdownAction.HEADER, MarkdownBlock.create("#"));
  }
}
