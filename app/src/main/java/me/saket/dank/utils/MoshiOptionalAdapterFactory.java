package me.saket.dank.utils;

import static me.saket.dank.utils.Preconditions.checkNotNull;

import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

public class MoshiOptionalAdapterFactory implements JsonAdapter.Factory {

  @Nullable
  @Override
  public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations, Moshi moshi) {
    if (type instanceof ParameterizedType && Optional.class == ((ParameterizedType) type).getRawType()) {
      return new OptionalAdapter(moshi, ((ParameterizedType) type).getActualTypeArguments()[0]);
    }
    return null;
  }

  private static class OptionalAdapter extends JsonAdapter<Optional<?>> {
    private JsonAdapter<Object> adapter;
    private final Moshi moshi;
    private final Type type;

    public OptionalAdapter(Moshi moshi, Type type) {
      this.moshi = moshi;
      this.type = type;
    }

    @Override
    public void toJson(JsonWriter out, @Nullable Optional<?> value) throws IOException {
      checkNotNull(value, "optional value == null");
      if (value.isPresent()) {
        adapter().toJson(out, value.get());
      } else {
        out.nullValue();
      }
    }

    @Override
    public Optional<?> fromJson(JsonReader in) throws IOException {
      //noinspection ConstantConditions
      return in.peek() == JsonReader.Token.NULL
          ? Optional.empty()
          : Optional.of(adapter().fromJson(in));
    }

    private JsonAdapter<Object> adapter() {
      if (adapter == null) {
        adapter = moshi.adapter(type);
      }
      return adapter;
    }
  }
}
