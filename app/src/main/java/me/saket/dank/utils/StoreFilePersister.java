package me.saket.dank.utils;

import android.support.annotation.NonNull;

import com.nytimes.android.external.fs3.FSReader;
import com.nytimes.android.external.fs3.FSWriter;
import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.Clearable;
import com.nytimes.android.external.store3.base.Persister;
import com.nytimes.android.external.store3.util.ParserException;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.reactivex.Maybe;
import io.reactivex.Single;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

public class StoreFilePersister<Key, Value> implements Persister<Value, Key>, Clearable<Key> {

  private final FSReader<Key> fileReader;
  private final FSWriter<Key> fileWriter;
  private final FileSystem fileSystem;
  private final PathResolver<Key> pathResolver;
  private final JsonParser<Key, Value> jsonParser;

  public interface JsonParser<Key, Value> {
    Value fromJson(BufferedSource jsonBufferedSource) throws IOException;

    String toJson(Value raw);
  }

  public StoreFilePersister(FileSystem fileSystem, PathResolver<Key> pathResolver, JsonParser<Key, Value> jsonParser) {
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;

    this.jsonParser = jsonParser;
    this.fileReader = new FSReader<>(fileSystem, pathResolver);
    this.fileWriter = new FSWriter<>(fileSystem, pathResolver);
  }

  @Nonnull
  @Override
  public Maybe<Value> read(@Nonnull Key key) {
    return fileReader
        .read(key)
        .map(bufferedSource -> jsonParser.fromJson(bufferedSource));
  }

  @Nonnull
  @Override
  public Single<Boolean> write(@Nonnull Key key, @Nonnull Value value) {
    String rawJson = jsonParser.toJson(value);
    InputStream stream = new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8));
    BufferedSource jsonBufferedSource = Okio.buffer(Okio.source(stream));

    return fileWriter.write(key, jsonBufferedSource);
  }

  @Override
  public void clear(@Nonnull Key key) {
    try {
      fileSystem.delete(pathResolver.resolve(key));
    } catch (IOException e) {
      Timber.e(e, "Error deleting item with key %s", key.toString());
    }
  }

  public static class MoshiParser<Raw> {
    private final JsonAdapter<Raw> jsonAdapter;

    @Inject
    public MoshiParser(@Nonnull Moshi moshi, @Nonnull Type type) {
      jsonAdapter = moshi.adapter(type);
    }

    public Raw fromJson(@NonNull BufferedSource bufferedSource) throws ParserException {
      try {
        return jsonAdapter.fromJson(bufferedSource);
      } catch (IOException e) {
        throw new ParserException(e.getMessage(), e);
      }
    }

    public String toJson(Raw raw) {
      return jsonAdapter.toJson(raw);
    }
  }
}
