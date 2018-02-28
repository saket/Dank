package me.saket.dank.data;

import net.dean.jraw.models.Contribution;

public abstract class StubContribution extends Contribution {

  public StubContribution() {
    super(null);
  }

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
