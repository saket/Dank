package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

@AutoValue
public abstract class ReplyDiscardClickEvent {

  public abstract Identifiable parent();

  public static ReplyDiscardClickEvent create(Identifiable parentContribution) {
    return new AutoValue_ReplyDiscardClickEvent(parentContribution);
  }
}
