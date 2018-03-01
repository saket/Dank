package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.FullNameType;

@AutoValue
public abstract class FocusedComment {

  public abstract String fullname();

  public static FocusedComment create(String id) {
    if (id.startsWith(FullNameType.COMMENT.prefix())) {
      throw new AssertionError();
    }
    return new AutoValue_FocusedComment(FullNameType.COMMENT.prefix() + id);
  }
}
