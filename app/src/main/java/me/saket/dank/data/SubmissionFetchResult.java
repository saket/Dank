package me.saket.dank.data;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import java.util.List;

@AutoValue
public abstract class SubmissionFetchResult {

  public abstract List<Submission> fetchedSubmissions();

  /**
   * Whether more submissions can be fetched after this.
   */
  public abstract boolean hasMoreItems();

  public static SubmissionFetchResult create(List<Submission> fetchedItems, boolean hasMoreItems) {
    return new AutoValue_SubmissionFetchResult(fetchedItems, hasMoreItems);
  }
}
