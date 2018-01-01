package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Contribution;

@AutoValue
public abstract class ReplyDiscardClickEvent {

  public abstract Contribution parentContribution();

  public static ReplyDiscardClickEvent create(Contribution parentContribution) {
    return new AutoValue_ReplyDiscardClickEvent(parentContribution);
  }
}
