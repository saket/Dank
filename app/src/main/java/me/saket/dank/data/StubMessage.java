package me.saket.dank.data;

import net.dean.jraw.models.Message;

/** See {@link StubContribution's doc */
abstract class StubMessage extends Message {

  public StubMessage() {
    super(null);
  }

  @Override
  public abstract boolean equals(Object otherObject);

  @Override
  public abstract int hashCode();
}
