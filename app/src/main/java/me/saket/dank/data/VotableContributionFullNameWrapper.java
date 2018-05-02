package me.saket.dank.data;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.models.attr.Votable;

/**
 * See {@link ThingFullNameWrapper}. Calling any method other than {@link #getFullName()} will result in an NPE.
 */
@AutoValue
public abstract class VotableContributionFullNameWrapper extends StubPublicContribution implements Votable {

  @Override
  public abstract String getFullName();

  @Override
  public abstract Integer getScore();

  @Override
  public abstract VoteDirection getVote();

  public static VotableContributionFullNameWrapper createFrom(PublicContribution contribution) {
    return new AutoValue_VotableContributionFullNameWrapper(contribution.getFullName(), contribution.getScore(), contribution.getVote());
  }

  public static JsonAdapter<VotableContributionFullNameWrapper> jsonAdapter(Moshi moshi) {
    return new AutoValue_VotableContributionFullNameWrapper.MoshiJsonAdapter(moshi);
  }
}
