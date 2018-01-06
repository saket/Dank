package me.saket.dank.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import javax.annotation.Nullable;

import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.PostedOrInFlightContribution.ContributionFetchedFromRemote;
import me.saket.dank.data.PostedOrInFlightContribution.LocallyPostedContribution;

/**
 * Because moshi.adapter(PostedOrInFlightContribution.class) will not work for classes
 * that implement {@link PostedOrInFlightContribution}, we do it manually here.
 */
public class MoshiPostedOrInFlightContributionAdapterFactory implements JsonAdapter.Factory {
  boolean called;

  @Nullable
  @Override
  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
    if (type.equals(PostedOrInFlightContribution.class)) {
      if (called) {
        throw new AssertionError();
      }
      called = true;
      return new Adapter(moshi);
    } else {
      return null;
    }
  }

  private class Adapter extends JsonAdapter<PostedOrInFlightContribution> {
    private static final String ID_REMOTE = "ContributionFetchedFromRemote";
    private static final String ID_LOCAL = "LocallyPostedContribution";

    private final JsonAdapter<ContributionFetchedFromRemote> remoteItemAdapter;
    private final JsonAdapter<LocallyPostedContribution> localItemAdapter;

    Adapter(Moshi moshi) {
      remoteItemAdapter = moshi.adapter(ContributionFetchedFromRemote.class);
      localItemAdapter = moshi.adapter(LocallyPostedContribution.class);
    }

    @Override
    public void toJson(JsonWriter writer, @Nullable PostedOrInFlightContribution contribution) throws IOException {
      if (contribution == null) {
        writer.nullValue();
        return;
      }

      writer.beginObject();
      writer.name("id");
      if (contribution instanceof ContributionFetchedFromRemote) {
        writer.value(ID_REMOTE);
        writer.name("object");
        remoteItemAdapter.toJson(writer, (ContributionFetchedFromRemote) contribution);

      } else if (contribution instanceof LocallyPostedContribution) {
        writer.value(ID_LOCAL);
        writer.name("object");
        localItemAdapter.toJson(writer, (LocallyPostedContribution) contribution);
      }
      writer.endObject();
    }

    @Nullable
    @Override
    public PostedOrInFlightContribution fromJson(JsonReader reader) throws IOException {
      reader.beginObject();
      reader.nextName();
      String id = reader.nextString();
      reader.nextName();

      PostedOrInFlightContribution value;
      if (ID_REMOTE.equals(id)) {
        value = remoteItemAdapter.fromJson(reader);

      } else if (ID_LOCAL.equals(id)) {
        value = localItemAdapter.fromJson(reader);

      } else {
        throw new AssertionError("Unknown type: " + id);
      }

      reader.endObject();
      return value;
    }
  }
}
