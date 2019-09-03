package me.saket.dank.cache;

import androidx.annotation.NonNull;

import com.nytimes.android.external.fs3.FSReader;
import com.nytimes.android.external.fs3.FSWriter;
import com.nytimes.android.external.fs3.FileSystemPersister;
import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.Clearable;
import com.nytimes.android.external.store3.base.Parser;
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

/**
 * Store provides {@link FileSystemPersister}, but it assumes that fetchers will return a {@link BufferedSource}
 * which can be stored directly into the file-system.
 * <p>
 * For usecases where a fetcher returns parsed Objects instead of BufferedSource, this persister can be used.
 */
public class StoreFilePersister<KEY, VALUE> implements Persister<VALUE, KEY>, Clearable<KEY> {

  private final FSReader<KEY> fileReader;
  private final FSWriter<KEY> fileWriter;
  private final FileSystem fileSystem;
  private final PathResolver<KEY> pathResolver;
  private final JsonParser<VALUE> jsonParser;

  /**
   * This exists because {@link Parser} only provides reading from JSON and not writing.
   */
  public interface JsonParser<VALUE> {
    VALUE fromJson(BufferedSource jsonBufferedSource) throws IOException;

    String toJson(VALUE raw);
  }

  public StoreFilePersister(FileSystem fileSystem, DiskLruCachePathResolver<KEY> pathResolver, JsonParser<VALUE> jsonParser) {
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;

    this.jsonParser = jsonParser;
    this.fileReader = new FSReader<>(fileSystem, pathResolver);
    this.fileWriter = new FSWriter<>(fileSystem, pathResolver);
  }

  @Nonnull
  @Override
  public Maybe<VALUE> read(@Nonnull KEY key) {
    return fileReader
        .read(key)
        .map(bufferedSource -> jsonParser.fromJson(bufferedSource));
  }

  @Nonnull
  @Override
  public Single<Boolean> write(@Nonnull KEY key, @Nonnull VALUE value) {
    String rawJson = jsonParser.toJson(value);
    InputStream stream = new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8));
    BufferedSource jsonBufferedSource = Okio.buffer(Okio.source(stream));

    return fileWriter.write(key, jsonBufferedSource);
  }

  @Override
  public void clear(@Nonnull KEY key) {
    try {
      fileSystem.delete(pathResolver.resolve(key));
    } catch (IOException e) {
      Timber.e(e, "Error deleting item with key %s", key.toString());
    } catch (IllegalStateException e) {
      if (!e.getMessage().contains("unable to delete")) {
        throw e;
      }
      // Else, file isn't present. Probably got removed by Android.
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
