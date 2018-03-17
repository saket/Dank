package me.saket.dank.di;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.danikula.videocache.HttpProxyCacheServer;
import com.jakewharton.disklrucache.DiskLruCache;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;
import com.nytimes.android.external.fs3.filesystem.FileSystem;
import com.nytimes.android.external.fs3.filesystem.FileSystemFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.data.AppInfo;
import me.saket.dank.data.FileSize;
import me.saket.dank.data.links.Link;
import me.saket.dank.utils.FileSizeUnit;

@Module
public class CacheModule {

  @Provides
  @Singleton
  FileSystem provideCacheFileSystem(Application appContext) {
    try {
      return FileSystemFactory.create(appContext.getCacheDir());
    } catch (IOException e) {
      throw new RuntimeException("Couldn't create FileSystemFactory. Cache dir: " + appContext.getCacheDir());
    }
  }

  /**
   * Used for caching videos.
   */
  @Provides
  @Singleton
  HttpProxyCacheServer provideHttpProxyCacheServer(Application appContext) {
    return new HttpProxyCacheServer(appContext);
  }

  @Provides
  @Named("cache_pre_filling")
  Scheduler cachePreFillingScheduler() {
    return Schedulers.from(Executors.newCachedThreadPool());
  }

  @Provides
  BitmapPool provideBitmapPool(Application appContext) {
    // Not adding Glide to the dagger graph intentionally. Glide objects
    // should be created in Activity, Fragment and View contexts instead.
    return Glide.get(appContext).getBitmapPool();
  }

  @Provides
  @Singleton
  @Named("markdown")
  Cache<String, CharSequence> provideMarkdownCache() {
    return CacheBuilder.newBuilder()
        .expireAfterAccess(15, TimeUnit.MINUTES)
        .build();
  }

  @Provides
  @Singleton
  @Named("url_parser")
  Cache<String, Link> provideUrlParserCache() {
    return CacheBuilder.newBuilder()
        .maximumSize(100)
        .build();
  }

  @Provides
  DiskLruCache diskLruCache(Application appContext, AppInfo appInfo) {
    FileSize maxCacheSize = FileSize.create(100, FileSizeUnit.MB);
    File cacheDirectory = new File(appContext.getCacheDir(), "disk_lru_cache");
    try {
      return DiskLruCache.open(cacheDirectory, appInfo.appVersionCode(), 1, (long) maxCacheSize.bytes());
    } catch (IOException e) {
      throw Exceptions.propagate(e);
    }
  }
}
