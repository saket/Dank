package me.saket.dank.cache;

import com.jakewharton.disklrucache.DiskLruCache;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.store3.base.RecordState;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.reactivex.exceptions.Exceptions;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import timber.log.Timber;

public class StoreLruFileSystem implements FileSystem {

  private final DiskLruCache lruCache;

  @Inject
  public StoreLruFileSystem(DiskLruCache lruCache) {
    this.lruCache = lruCache;
  }

  @Nonnull
  @Override
  public BufferedSource read(String path) {
    try {
      DiskLruCache.Snapshot snapshot = lruCache.get(path);
      if (snapshot == null) {
        throw new FileNotFoundException(path);
      }
      return Okio.buffer(Okio.source(snapshot.getInputStream(0)));

    } catch (IOException e) {
      throw Exceptions.propagate(e);
    }
  }

  @Override
  public void write(String path, BufferedSource source) throws IOException {
    DiskLruCache.Editor editor = lruCache.edit(path);
    try {
      if (editor == null) {
        Timber.i("Another edit to the same path is already in progress: %s", path);
        return;
      }

      OutputStream outputStream = editor.newOutputStream(0);
      try (BufferedSink sink = Okio.buffer(Okio.sink(outputStream))) {
        sink.writeAll(source);
      }
      editor.commit();

    } catch (Exception e) {
      //noinspection ConstantConditions
      editor.abortUnlessCommitted();
    }
  }

  @Override
  public void delete(String path) throws IOException {
    lruCache.remove(path);
  }

  @Override
  public void deleteAll(String path) throws IOException {
    lruCache.delete();
  }

  @Override
  public boolean exists(String path) {
    try {
      return lruCache.get(path) != null;
    } catch (IOException e) {
      throw Exceptions.propagate(e);
    }
  }

  @Nonnull
  @Override
  public Collection<String> list(String path) throws FileNotFoundException {
    throw new UnsupportedOperationException();
  }

  @Override
  public RecordState getRecordState(@Nonnull TimeUnit expirationUnit, long expirationDuration, @Nonnull String path) {
    throw new UnsupportedOperationException();
  }
}
