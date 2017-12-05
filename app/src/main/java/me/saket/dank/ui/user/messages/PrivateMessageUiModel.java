package me.saket.dank.ui.user.messages;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PrivateMessageUiModel {

  public abstract String senderName();

  public abstract CharSequence messageBody();

  public abstract CharSequence byline();

  public abstract long createdTimeMillis();

  public abstract long adapterId();

  /** The original model from which this Ui model was created. */
  public abstract Object parentModel();

  public static PrivateMessageUiModel create(
      String senderName,
      CharSequence messageBody,
      CharSequence byline,
      long createdTimeMillis,
      long adapterId,
      Object parentModel)
  {
    return new AutoValue_PrivateMessageUiModel(senderName, messageBody, byline, createdTimeMillis, adapterId, parentModel);
  }
}
