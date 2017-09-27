package me.saket.dank.ui.compose;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MarkdownBlock {

  public abstract String prefix();

  public abstract String suffix();

  public static MarkdownBlock create(String prefix, String suffix) {
    return new AutoValue_MarkdownBlock(prefix, suffix);
  }

  public static MarkdownBlock create(String prefix) {
    return new AutoValue_MarkdownBlock(prefix, "");
  }
}
