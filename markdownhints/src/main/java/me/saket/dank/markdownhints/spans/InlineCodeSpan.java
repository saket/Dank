package me.saket.dank.markdownhints.spans;

import androidx.annotation.NonNull;

import ru.noties.markwon.spans.CodeSpan;
import ru.noties.markwon.spans.SpannableTheme;

public class InlineCodeSpan extends CodeSpan {

  public InlineCodeSpan(@NonNull SpannableTheme theme) {
    super(theme, false);
  }
}
