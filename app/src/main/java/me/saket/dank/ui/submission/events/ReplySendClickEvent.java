package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

/**
 * Emitted when the send button is pressed in an inline comment reply.
 */
@AutoValue
public abstract class ReplySendClickEvent {

  public abstract Contribution parentContribution();

  public abstract String replyMessage();

  public static ReplySendClickEvent create(Contribution parentContribution, String replyMessage) {
    return new AutoValue_ReplySendClickEvent(parentContribution, replyMessage);
  }
}
