package me.saket.dank.markdownhints.spans;

import androidx.annotation.NonNull;

import ru.noties.markwon.spans.SpannableTheme;

public class HeadingSpanWithLevel extends ru.noties.markwon.spans.HeadingSpan {

  private final int level;

  public HeadingSpanWithLevel(@NonNull SpannableTheme theme, int level) {
    super(theme, level);
    this.level = level;
  }

  public int level() {
    return level;
  }
}
