package me.saket.dank.di

import android.app.Application
import android.arch.persistence.room.Room
import dagger.Module
import dagger.Provides
import me.saket.dank.data.AppDatabase

@Module
class StorageModule {

  @Provides
  fun appDatabase(appContext: Application): AppDatabase {
    return Room.databaseBuilder(appContext, AppDatabase::class.java, "Dank-room").build()
  }
}
