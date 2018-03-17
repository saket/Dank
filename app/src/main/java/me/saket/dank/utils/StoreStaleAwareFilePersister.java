package me.saket.dank.utils;

import com.nytimes.android.external.fs3.PathResolver;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.RecordProvider;
import com.nytimes.android.external.store3.base.RecordState;

import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

// TODO.
public class StoreStaleAwareFilePersister<Key, Value> extends StoreFilePersister<Key, Value> implements RecordProvider<Key> {

  private final FileSystem fileSystem;
  private final PathResolver<Key> pathResolver;
  private final long expirationDuration;
  private final TimeUnit expirationUnit;

  public StoreStaleAwareFilePersister(FileSystem fileSystem, PathResolver<Key> pathResolver, JsonParser<Value> jsonParser,
      long expirationDuration, TimeUnit expirationUnit)
  {
    super(fileSystem, pathResolver, jsonParser);
    this.fileSystem = fileSystem;
    this.pathResolver = pathResolver;
    this.expirationDuration = expirationDuration;
    this.expirationUnit = expirationUnit;
  }

  @Nonnull
  @Override
  public RecordState getRecordState(Key key) {
    return fileSystem.getRecordState(expirationUnit, expirationDuration, pathResolver.resolve(key));
  }
}
