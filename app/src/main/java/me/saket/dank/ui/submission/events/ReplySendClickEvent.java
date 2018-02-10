package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.ContributionFullNameWrapper;

/**
 * Emitted when the send button is pressed in an inline comment reply.
 */
@AutoValue
public abstract class ReplySendClickEvent {

  public abstract ContributionFullNameWrapper parentContribution();

  public abstract String replyMessage();

  public static ReplySendClickEvent create(ContributionFullNameWrapper parentContribution, String replyMessage) {
    return new AutoValue_ReplySendClickEvent(parentContribution, replyMessage);
  }
}
