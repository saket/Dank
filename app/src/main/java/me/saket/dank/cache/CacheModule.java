package me.saket.dank.cache;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.danikula.videocache.HttpProxyCacheServer;
import com.jakewharton.disklrucache.DiskLruCache;
import com.nytimes.android.external.cache3.Cache;
import com.nytimes.android.external.cache3.CacheBuilder;
import com.nytimes.android.external.fs3.filesystem.FileSystem;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.data.AppInfo;
import me.saket.dank.data.FileSize;
import me.saket.dank.urlparser.Link;
import me.saket.dank.utils.DeviceInfo;
import me.saket.dank.utils.FileSizeUnit;

@Module
public class CacheModule {

  @Provides
  @Singleton
  FileSystem provideCacheFileSystem(StoreLruFileSystem lruFileSystem) {
    return lruFileSystem;
  }

  @Provides
  DiskLruCache diskLruCache(Application appContext, AppInfo appInfo) {
    FileSize maxCacheSize = FileSize.create(100, FileSizeUnit.MB);
    File cacheDirectory = new File(appContext.getCacheDir(), "disk_lru_cache");
    try {
      int valuesPerCacheEntry = 1;  // No idea what this means. Glide uses 1.
      return DiskLruCache.open(cacheDirectory, appInfo.appVersionCode(), valuesPerCacheEntry, (long) maxCacheSize.bytes());
    } catch (IOException e) {
      throw Exceptions.propagate(e);
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
  @Named("url_parser")
  Cache<String, Link> provideUrlParserCache(DeviceInfo deviceInfo) {
    if (BuildConfig.DEBUG && deviceInfo.isRunningOnEmulator()) {
      return CacheBuilder.newBuilder()
          .maximumSize(0)
          .build();
    }
    return CacheBuilder.newBuilder()
        .maximumSize(100)
        .build();
  }
}
