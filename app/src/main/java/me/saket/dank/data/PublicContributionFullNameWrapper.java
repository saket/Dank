package me.saket.dank.data;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.VoteDirection;

import java.io.Serializable;

/**
 * See {@link ThingFullNameWrapper}.
 */
public class PublicContributionFullNameWrapper extends PublicContribution implements Serializable {

  private final String fullName;
  private final Integer score;
  private final VoteDirection voteDirection;

  public static PublicContributionFullNameWrapper createFrom(PublicContribution contribution) {
    return new PublicContributionFullNameWrapper(contribution.getFullName(), contribution.getScore(), contribution.getVote());
  }

  private PublicContributionFullNameWrapper(String fullName, Integer score, VoteDirection voteDirection) {
    super(null);
    this.fullName = fullName;
    this.score = score;
    this.voteDirection = voteDirection;
  }

  @Override
  public String getFullName() {
    return fullName;
  }

  @Override
  public Integer getScore() {
    return score;
  }

  @Override
  public VoteDirection getVote() {
    return voteDirection;
  }
}
