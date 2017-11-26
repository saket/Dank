package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

@AutoValue
public abstract class ReplyDiscardClickEvent {

  public abstract PublicContribution parentContribution();

  public static ReplyDiscardClickEvent create(PublicContribution parentContribution) {
    return new AutoValue_ReplyDiscardClickEvent(parentContribution);
  }
}
