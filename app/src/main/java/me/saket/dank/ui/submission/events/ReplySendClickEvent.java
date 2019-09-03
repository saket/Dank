package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

/**
 * Emitted when the send button is pressed in an inline comment reply.
 */
@AutoValue
public abstract class ReplySendClickEvent {

  public abstract Identifiable parentContribution();

  public abstract String replyBody();

  public static ReplySendClickEvent create(Identifiable parentContribution, String replyBody) {
    return new AutoValue_ReplySendClickEvent(parentContribution, replyBody);
  }
}
