package me.saket.dank.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.saket.dank.ui.submission.CachedSubmission
import me.saket.dank.ui.submission.CachedSubmissionComments
import me.saket.dank.ui.submission.CachedSubmissionDao
import me.saket.dank.ui.submission.CachedSubmissionId2
import me.saket.dank.ui.submission.DankSubmissionRequestRoomTypeConverter
import me.saket.dank.ui.submission.RepliesRoomTypeConverter
import me.saket.dank.ui.submission.SortingAndTimePeriodRoomTypeConverter
import me.saket.dank.ui.submission.SubmissionRoomTypeConverter

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
