package me.saket.dank.ui.compose;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;

import androidx.annotation.Nullable;

import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;

public class TextFormatToolbarView extends HorizontalScrollView {

  private ActionClickListener actionClickListener;

  public interface ActionClickListener {
    /**
     * @param markdownBlock Nullable for insert-link, insert-text-emoji and insert-image.
     */
    void onClickAction(View buttonView, MarkdownAction markdownAction, @Nullable MarkdownBlock markdownBlock);
  }

  public TextFormatToolbarView(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.custom_text_formatting_toolbar, this, true);
    ButterKnife.bind(this);
  }

  public void setActionClickListener(ActionClickListener listener) {
    actionClickListener = listener;
  }

  @OnClick(R.id.textformattoolbar_insert_text_emoji)
  void onClickInsertTextEmoji(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.INSERT_TEXT_EMOJI, null);
  }

  @OnClick(R.id.textformattoolbar_insert_link)
  void onClickInsertLink(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.INSERT_LINK, null);
  }

  @OnClick(R.id.textformattoolbar_insert_image)
  void onClickInsertImage(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.INSERT_IMAGE, null);
  }

  @OnClick(R.id.textformattoolbar_insert_gif)
  void onClickInsertGif(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.INSERT_GIF, null);
  }

  @OnClick(R.id.textformattoolbar_bold)
  void onClickBold(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.BOLD, MarkdownBlock.BOLD);
  }

  @OnClick(R.id.textformattoolbar_italic)
  void onClickItalic(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.ITALIC, MarkdownBlock.ITALIC);
  }

  @OnClick(R.id.textformattoolbar_strikethrough)
  void onClickStrikeThrough(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.STRIKE_THROUGH, MarkdownBlock.STRIKE_THROUGH);
  }

  @OnClick(R.id.textformattoolbar_quote)
  void onClickQuote(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.QUOTE, MarkdownBlock.QUOTE);
  }

  @OnClick(R.id.textformattoolbar_inline_code)
  void onClickInlineCode(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.INLINE_CODE, MarkdownBlock.INLINE_CODE);
  }

  @OnClick(R.id.textformattoolbar_header)
  void onClickHeader(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.HEADING, MarkdownBlock.HEADING);
  }

  @OnClick(R.id.textformattoolbar_spoiler)
  void onClickSpoiler(View view) {
    actionClickListener.onClickAction(view, MarkdownAction.SPOILER, MarkdownBlock.SPOILER);
  }
}
