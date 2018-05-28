package me.saket.dank.data;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;


/**
 * Reddit uses a {@link PublicContribution}'s full name as the anchor for paginating listings. On every page load, it returns the last
 * Thing's name in the list that can be used as the anchor for getting the next page.
 * <p>
 * This class holds the name of that anchor Thing.
 */
@AutoValue
public abstract class PaginationAnchor {

  public abstract String fullName();

  public static PaginationAnchor create(String fullName) {
    return new AutoValue_PaginationAnchor(fullName);
  }

  public static PaginationAnchor createEmpty() {
    return new AutoValue_PaginationAnchor("");
  }

  public boolean isEmpty() {
    return fullName().length() == 0;
  }
}
