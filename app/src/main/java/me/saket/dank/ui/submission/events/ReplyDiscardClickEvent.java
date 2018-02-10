package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.ContributionFullNameWrapper;

@AutoValue
public abstract class ReplyDiscardClickEvent {

  public abstract ContributionFullNameWrapper parentContribution();

  public static ReplyDiscardClickEvent create(ContributionFullNameWrapper parentContribution) {
    return new AutoValue_ReplyDiscardClickEvent(parentContribution);
  }
}
