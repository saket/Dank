package me.saket.dank.data;

import static me.saket.dank.utils.Preconditions.checkNotNull;

import android.support.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Thing;

import java.io.IOException;
import java.io.Serializable;

import me.saket.dank.ui.subreddit.models.SubredditScreenUiModel;

/**
 * See {@link ThingFullNameWrapper}.
 * <p>
 * This class is also used in all {@link SubredditScreenUiModel.SubmissionRowUiModel} over
 * actual {@link Contribution} objects to speed up equals() calls.
 */
public class ContributionFullNameWrapper extends Contribution implements Serializable {

  private final String fullName;

  public static ContributionFullNameWrapper create(String fullName) {
    return new ContributionFullNameWrapper(fullName);
  }

  public static ContributionFullNameWrapper createFrom(Thing thing) {
    return create(thing.getFullName());
  }

  private ContributionFullNameWrapper(String fullName) {
    super(null);
    this.fullName = fullName;
  }

  @Override
  public String getFullName() {
    return fullName;
  }

  public static class MoshiJsonAdapter extends JsonAdapter<ContributionFullNameWrapper> {
    @Override
    public ContributionFullNameWrapper fromJson(JsonReader reader) throws IOException {
      reader.beginObject();
      String fullname = reader.nextString();
      reader.endObject();
      return create(fullname);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable ContributionFullNameWrapper value) throws IOException {
      checkNotNull(value, "contribution");
      writer.beginObject();
      writer.name("fullname");
      writer.value(value.fullName);
      writer.endObject();
    }
  }
}
