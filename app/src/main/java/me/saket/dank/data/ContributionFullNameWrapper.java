package me.saket.dank.data;

import net.dean.jraw.models.Contribution;

/**
 * See {@link ThingFullNameWrapper}.
 */
public class ContributionFullNameWrapper extends Contribution {

  private String fullName;

  public static ContributionFullNameWrapper create(String fullName) {
    return new ContributionFullNameWrapper(fullName);
  }

  public ContributionFullNameWrapper(String fullName) {
    super(null);
    this.fullName = fullName;
  }

  @Override
  public String getFullName() {
    return fullName;
  }
}
