package me.saket.dank.ui.submission

import androidx.room.*
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import io.reactivex.Flowable
import me.saket.dank.di.StorageModule
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
data class CachedSubmissionComments(
    val submissionId: String,

    @field:[Enveloped()]
    val replies: Listing<NestedIdentifiable>,

    @PrimaryKey
    val request: DankSubmissionRequest,

    val saveTimeMillis: Long
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

@Entity(primaryKeys = ["id", "subredditName", "sortingAndTimePeriod"])
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

  @Query("SELECT * FROM cachedsubmissioncomments WHERE saveTimeMillis < :savedBeforeMillis")
  fun countOfSubmissionWithComments(savedBeforeMillis: Long): Flowable<List<CachedSubmissionComments>>

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  fun saveSubmissionIdIfNew(cachedSubmissionId: CachedSubmissionId2): Long

  @Query("SELECT * FROM cachedsubmissionid2 \nWHERE subredditName = :subredditName \nAND sortingAndTimePeriod = :sortingAndTimePeriod \nORDER BY saveTimeMillis DESC \nLIMIT 1")
  fun lastSubmissionId(subredditName: String, sortingAndTimePeriod: SortingAndTimePeriod): CachedSubmissionId2?

  @Query("DELETE FROM cachedsubmissionid2 WHERE subredditName = :subreddit")
  fun deleteSubmissionIdsInSubredit(subreddit: String)

  @Query("DELETE FROM cachedsubmissionid2")
  fun deleteAllSubmissionIds()

  @Query("DELETE FROM cachedsubmissioncomments WHERE request = :request")
  fun deleteComments(request: DankSubmissionRequest)

  @Query("DELETE FROM cachedsubmissioncomments")
  fun deleteAllComments()

  @Query("DELETE FROM cachedsubmissionid2 WHERE saveTimeMillis < :savedBeforeMillis")
  fun deleteSubmissionIdsBefore(savedBeforeMillis: Long): Int

  @Query("DELETE FROM cachedsubmission WHERE saveTimeMillis < :savedBeforeMillis")
  fun deleteSubmissionsBefore(savedBeforeMillis: Long): Int

  @Query("DELETE FROM cachedsubmissioncomments WHERE saveTimeMillis < :savedBeforeMillis")
  fun deleteSubmissionCommentsBefore(savedBeforeMillis: Long): Int

  @Transaction
  fun deleteAllSubmissionRelatedRows(savedBeforeMillis: Long): Int {
    var deletedRowCount = 0
    deletedRowCount += deleteSubmissionIdsBefore(savedBeforeMillis)
    deletedRowCount += deleteSubmissionsBefore(savedBeforeMillis)
    deletedRowCount += deleteSubmissionCommentsBefore(savedBeforeMillis)
    return deletedRowCount
  }

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

  private val adapter: JsonAdapter<T> by lazy {
    StorageModule().provideMoshi().adapter(klass)
  }

  @TypeConverter
  fun toJson(request: T): String {
    return adapter.toJson(request)
  }

  @TypeConverter
  fun fromJson(json: String?): T? {
    return json?.let { adapter.fromJson(it) }
  }
}
