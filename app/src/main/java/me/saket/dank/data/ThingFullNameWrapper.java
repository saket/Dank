package me.saket.dank.data;

import net.dean.jraw.models.Thing;

/**
 * Most of the actionable APIs exposed by JRAW like voting, marking-as-read, etc. only accept
 * {@link Thing} objects instead of their IDs even when only their IDs are supplied for
 * making the API calls to Reddit. This wrapper exists to workaround this problem.
 *
 * Calling any method other than {@link #getFullName()} will result in an NPE.
 */
public class ThingFullNameWrapper extends Thing {

  private final String fullName;

  public static ThingFullNameWrapper create(String fullName) {
    return new ThingFullNameWrapper(fullName);
  }

  public ThingFullNameWrapper(String fullName) {
    super(null);
    this.fullName = fullName;
  }

  @Override
  public String getFullName() {
    return fullName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ThingFullNameWrapper)) {
      return false;
    }
    ThingFullNameWrapper that = (ThingFullNameWrapper) o;
    return fullName.equals(that.fullName);
  }

  @Override
  public int hashCode() {
    return fullName.hashCode();
  }
}
