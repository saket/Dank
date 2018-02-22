package me.saket.dank.data;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

/**
 * See {@link ThingFullNameWrapper}. Calling any method other than {@link #getFullName()} will result in an NPE.
 */
@AutoValue
public abstract class VotableThingFullNameWrapper extends StubThing implements Votable {

  public abstract String fullName();

  public static VotableThingFullNameWrapper create(String fullName) {
    return new AutoValue_VotableThingFullNameWrapper(fullName);
  }

  @Override
  public Integer getScore() {
    throw new UnsupportedOperationException();
  }

  @Override
  public VoteDirection getVote() {
    throw new UnsupportedOperationException();
  }
}
