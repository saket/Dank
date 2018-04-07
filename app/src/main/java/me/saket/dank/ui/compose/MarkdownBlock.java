package me.saket.dank.ui.compose;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MarkdownBlock {

  public static final MarkdownBlock BOLD = MarkdownBlock.create("**", "**");
  public static final MarkdownBlock ITALIC = MarkdownBlock.create("*", "*");
  public static final MarkdownBlock STRIKE_THROUGH = MarkdownBlock.create("~~", "~~");
  public static final MarkdownBlock QUOTE = MarkdownBlock.create("> ");
  public static final MarkdownBlock SUPERSCRIPT = MarkdownBlock.create("^");
  public static final MarkdownBlock INLINE_CODE = MarkdownBlock.create("`", "`");
  public static final MarkdownBlock HEADING = MarkdownBlock.create("# ");
  public static final MarkdownBlock SPOILER = MarkdownBlock.create("[spoiler](/s \"", "\")");

  public abstract String prefix();

  public abstract String suffix();

  public static MarkdownBlock create(String prefix, String suffix) {
    return new AutoValue_MarkdownBlock(prefix, suffix);
  }

  public static MarkdownBlock create(String prefix) {
    return new AutoValue_MarkdownBlock(prefix, "");
  }
}
