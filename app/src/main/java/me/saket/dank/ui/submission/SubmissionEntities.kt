package me.saket.dank.ui.submission

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.Query
import android.arch.persistence.room.TypeConverter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import io.reactivex.Flowable
import me.saket.dank.di.RootModule
import me.saket.dank.utils.DankSubmissionRequest
import me.saket.dank.utils.Optional
import net.dean.jraw.JrawUtils
import net.dean.jraw.databind.Enveloped
import net.dean.jraw.models.Listing
import net.dean.jraw.models.NestedIdentifiable
import net.dean.jraw.models.Submission
import net.dean.jraw.tree.CommentTreeSettings
import net.dean.jraw.tree.RootCommentNode

@Entity
data class CachedSubmission(
    @PrimaryKey
    val id: String,
    val submission: Submission,
    val subredditName: String,
    val saveTimeMillis: Long
)

@Entity
data class CachedSubmissionComments constructor(
    val submissionId: String,

    @field:[Enveloped()]
    val replies: Listing<NestedIdentifiable>,

    @PrimaryKey
    val request: DankSubmissionRequest
)

data class CachedSubmissionAndComments(
    val id: String,
    val submission: Submission,
    val replies: Listing<NestedIdentifiable>?,
    val request: DankSubmissionRequest?
) {

  fun comments(): Optional<RootCommentNode> {
    if (replies == null || request == null) {
      return Optional.empty()
    }
    return Optional.of(RootCommentNode(submission, replies, CommentTreeSettings(submission.id, request.commentSort().mode())))
  }
}

@Entity(primaryKeys = ["subredditName", "sortingAndTimePeriod", "saveTimeMillis"])
data class CachedSubmissionId2 constructor(
    val id: String,
    val subredditName: String,
    val sortingAndTimePeriod: SortingAndTimePeriod,
    val saveTimeMillis: Long
)

@Dao
interface CachedSubmissionDao {

  @Query("SELECT S.id, S.submission, C.replies, C.request\nFROM cachedsubmission S \nLEFT JOIN cachedsubmissioncomments C \nON (S.id = C.submissionId AND C.request = :request)\nWHERE S.id = :id\n")
  fun submissionWithComments(id: String, request: DankSubmissionRequest): Flowable<List<CachedSubmissionAndComments>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun saveSubmission(submission: CachedSubmission)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun saveComments(comments: CachedSubmissionComments)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun saveSubmissionIdIfNew(cachedSubmissionId: CachedSubmissionId2): Long

  @Query("SELECT * FROM cachedsubmissionid2 \nWHERE subredditName = :subredditName \nAND sortingAndTimePeriod = :sortingAndTimePeriod \nORDER BY saveTimeMillis DESC \nLIMIT 1")
  fun lastSubmissionId(subredditName: String, sortingAndTimePeriod: SortingAndTimePeriod): CachedSubmissionId2?

  @Query("DELETE FROM cachedsubmissionid2 WHERE subredditName = :subreddit")
  fun deleteSubmissionIdsInSubredit(subreddit: String)

  @Query("""DELETE FROM cachedsubmissionid2""")
  fun deleteAllSubmissionIds()

  @Query("SELECT S.submission FROM cachedsubmissionid2 ID\nINNER JOIN cachedsubmission S\nON ID.id = S.id\nWHERE ID.subredditName = :subredditName AND ID.sortingAndTimePeriod = :sortingAndTimePeriod\nORDER BY ID.saveTimeMillis ASC")
  fun submissionsInFolderAsc(subredditName: String, sortingAndTimePeriod: SortingAndTimePeriod): Flowable<List<Submission>>
}

class SubmissionRoomTypeConverter {

  private val adapter by lazy { JrawUtils.adapter<Submission>().serializeNulls() }

  @TypeConverter
  fun toJson(submission: Submission): String {
    return adapter.toJson(submission)
  }

  @TypeConverter
  fun fromJson(json: String): Submission {
    return adapter.fromJson(json)!!
  }
}

class RepliesRoomTypeConverter {
  private val adapter by lazy {
    val type = Types.newParameterizedType(Listing::class.java, NestedIdentifiable::class.java)
    JrawUtils.moshi.adapter<Listing<NestedIdentifiable>>(type, Enveloped::class.java).serializeNulls()
  }

  @TypeConverter
  fun toJson(replies: Listing<NestedIdentifiable>): String {
    return adapter.toJson(replies)
  }

  @TypeConverter
  fun fromJson(json: String?): Listing<NestedIdentifiable>? {
    if (json == null) {
      return null
    }
    return adapter.fromJson(json) as Listing<NestedIdentifiable>
  }
}

class DankSubmissionRequestRoomTypeConverter : MoshiBasedRoomTypeConverter<DankSubmissionRequest>(DankSubmissionRequest::class.java)

class SortingAndTimePeriodRoomTypeConverter : MoshiBasedRoomTypeConverter<SortingAndTimePeriod>(SortingAndTimePeriod::class.java)

open class MoshiBasedRoomTypeConverter<T>(private val klass: Class<T>) {

  private val adapter: JsonAdapter<T> by lazy { RootModule.provideMoshi().adapter(klass) }

  @TypeConverter
  fun toJson(request: T): String {
    return adapter.toJson(request)
  }

  @TypeConverter
  fun fromJson(json: String?): T? {
    return json?.let { adapter.fromJson(it) }
  }
}
