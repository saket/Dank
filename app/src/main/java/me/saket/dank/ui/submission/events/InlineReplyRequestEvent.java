package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

@AutoValue
public abstract class InlineReplyRequestEvent {

  public abstract Contribution parentContribution();

  public static InlineReplyRequestEvent create(Contribution parentContribution) {
    return new AutoValue_InlineReplyRequestEvent(parentContribution);
  }
}
