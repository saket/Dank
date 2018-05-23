package me.saket.dank.data;

import net.dean.jraw.models.Contribution;

/**
 * AutoValue doesn't generate equals() and hashcode() if they're already present in the
 * super class. This class workarounds this issue by declaring them as abstract again.
 */
public abstract class StubContribution extends Contribution {

  public StubContribution() {
    super(null);
  }

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
