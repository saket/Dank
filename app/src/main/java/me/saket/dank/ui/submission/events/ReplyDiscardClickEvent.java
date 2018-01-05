package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.PostedOrInFlightContribution;

@AutoValue
public abstract class ReplyDiscardClickEvent {

  public abstract PostedOrInFlightContribution parentContribution();

  public static ReplyDiscardClickEvent create(PostedOrInFlightContribution parentContribution) {
    return new AutoValue_ReplyDiscardClickEvent(parentContribution);
  }
}
