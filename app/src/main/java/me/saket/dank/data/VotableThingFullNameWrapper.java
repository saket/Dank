package me.saket.dank.data;

import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

/**
 * See {@link ThingFullNameWrapper}. Calling any method other than {@link #getFullName()} will result in an NPE.
 */
public class VotableThingFullNameWrapper extends ThingFullNameWrapper implements Votable {

  private String fullName;

  public static VotableThingFullNameWrapper create(String fullName) {
    return new VotableThingFullNameWrapper(fullName);
  }

  public VotableThingFullNameWrapper(String fullName) {
    super(null);
    this.fullName = fullName;
  }

  @Override
  public String getFullName() {
    return fullName;
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
