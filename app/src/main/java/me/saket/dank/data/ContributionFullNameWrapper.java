package me.saket.dank.data;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Thing;

import me.saket.dank.ui.subreddit.models.SubredditScreenUiModel;

/**
 * See {@link ThingFullNameWrapper}.
 * <p>
 * This class is also used in all {@link SubredditScreenUiModel.SubmissionRowUiModel} over
 * actual {@link Contribution} objects to speed up equals() calls.
 */
@AutoValue
public abstract class ContributionFullNameWrapper extends StubContribution implements Parcelable {

  public abstract String fullName();

  public static ContributionFullNameWrapper create(String fullName) {
    return new AutoValue_ContributionFullNameWrapper(fullName);
  }

  public static ContributionFullNameWrapper createFrom(Thing thing) {
    return create(thing.getFullName());
  }

  @Override
  public String getFullName() {
    return fullName();
  }

  public static JsonAdapter<ContributionFullNameWrapper> jsonAdapter(Moshi moshi) {
    return new AutoValue_ContributionFullNameWrapper.MoshiJsonAdapter(moshi);
  }
}
