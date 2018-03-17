package me.saket.dank.utils;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import okio.BufferedSource;

public class MoshiStoreJsonParser<KEY, VALUE> implements StoreFilePersister.JsonParser<VALUE> {
  private final Moshi moshi;
  private final Class<VALUE> valueType;

  public MoshiStoreJsonParser(Moshi moshi, Class<VALUE> valueType) {
    this.moshi = moshi;
    this.valueType = valueType;
  }

  @Override
  public VALUE fromJson(BufferedSource jsonBufferedSource) throws IOException {
    JsonAdapter<VALUE> adapter = moshi.adapter(valueType);
    return adapter.fromJson(jsonBufferedSource);
  }

  @Override
  public String toJson(VALUE raw) {
    return moshi.adapter(valueType).toJson(raw);
  }
}
