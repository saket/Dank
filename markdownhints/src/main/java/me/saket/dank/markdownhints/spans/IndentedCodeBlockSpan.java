package me.saket.dank.markdownhints.spans;

import androidx.annotation.NonNull;

import ru.noties.markwon.spans.CodeSpan;
import ru.noties.markwon.spans.SpannableTheme;

public class IndentedCodeBlockSpan extends CodeSpan {

  public IndentedCodeBlockSpan(@NonNull SpannableTheme theme) {
    super(theme, true);
  }
}
