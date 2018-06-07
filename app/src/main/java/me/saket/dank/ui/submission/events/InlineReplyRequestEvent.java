package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

import me.saket.dank.data.SwipeEvent;

@AutoValue
public abstract class InlineReplyRequestEvent implements SwipeEvent {

  public abstract Identifiable parentContribution();

  public static InlineReplyRequestEvent create(Identifiable parentContribution) {
    return new AutoValue_InlineReplyRequestEvent(parentContribution);
  }
}
