package me.saket.dank.data;

import net.dean.jraw.models.PublicContribution;

public abstract class StubPublicContribution extends PublicContribution {

  public StubPublicContribution() {
    super(null);
  }

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
