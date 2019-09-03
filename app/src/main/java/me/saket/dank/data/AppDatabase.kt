package me.saket.dank.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.saket.dank.ui.submission.*

@Database(
    entities = [
      CachedSubmission::class,
      CachedSubmissionComments::class,
      CachedSubmissionId2::class],
    version = 2,
    exportSchema = false)
@TypeConverters(
    SubmissionRoomTypeConverter::class,
    RepliesRoomTypeConverter::class,
    DankSubmissionRequestRoomTypeConverter::class,
    SortingAndTimePeriodRoomTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

  abstract fun submissionDao(): CachedSubmissionDao
}
