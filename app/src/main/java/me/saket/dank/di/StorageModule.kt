package me.saket.dank.di

import android.app.Application
import androidx.room.Room
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import me.saket.dank.data.AppDatabase
import me.saket.dank.utils.AutoValueMoshiAdapterFactory
import me.saket.dank.utils.MoshiOptionalAdapterFactory
import net.dean.jraw.JrawUtils
import javax.inject.Singleton

@Module
class StorageModule {

  @Provides
  @Singleton
  fun provideMoshi(): Moshi {
    return JrawUtils.moshi
        .newBuilder()
        .add(AutoValueMoshiAdapterFactory.create())
        .add(MoshiOptionalAdapterFactory())
        .build()
  }

  @Provides
  fun appDatabase(appContext: Application): AppDatabase {
    return Room.databaseBuilder(appContext, AppDatabase::class.java, "Dank-room")
        .fallbackToDestructiveMigrationFrom(1)
        .build()
  }
}
