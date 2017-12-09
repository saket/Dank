package me.saket.dank.ui.user.messages;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PrivateMessageUiModel {

  public enum Direction {
    RECEIVED,
    SENT
  }

  public abstract String senderName();

  public abstract CharSequence messageBody();

  public abstract CharSequence byline();

  public abstract long sentTimeMillis();

  public abstract long adapterId();

  /** The original model from which this Ui model was created. */
  public abstract Object originalModel();

  public abstract boolean isClickable();

  public abstract Direction senderType();

  public static PrivateMessageUiModel.Builder builder() {
    return new AutoValue_PrivateMessageUiModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder senderName(String name);

    public abstract Builder messageBody(CharSequence body);

    public abstract Builder byline(CharSequence byline);

    public abstract Builder sentTimeMillis(long sentTimeMillis);

    public abstract Builder adapterId(long adapterId);

    public abstract Builder originalModel(Object originalModel);

    public abstract Builder isClickable(boolean isClickable);

    public abstract Builder senderType(Direction type);

    public abstract PrivateMessageUiModel build();
  }
}
