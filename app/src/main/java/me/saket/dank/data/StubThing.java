package me.saket.dank.data;

import net.dean.jraw.models.Thing;

public abstract class StubThing extends Thing {

  public StubThing() {
    super(null);
  }

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
