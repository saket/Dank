package me.saket.dank.data;

import net.dean.jraw.models.PublicContribution;

@Deprecated
public abstract class StubPublicContribution implements PublicContribution {

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
