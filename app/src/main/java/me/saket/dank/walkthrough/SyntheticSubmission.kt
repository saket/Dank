package me.saket.dank.walkthrough

import me.saket.dank.data.FullNameType
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.submission.SubmissionAndComments
import me.saket.dank.utils.Optional
import net.dean.jraw.models.Comment
import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.DistinguishedStatus
import net.dean.jraw.models.EmbeddedMedia
import net.dean.jraw.models.Listing
import net.dean.jraw.models.NestedIdentifiable
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubmissionPreview
import net.dean.jraw.models.Subreddit
import net.dean.jraw.models.VoteDirection
import net.dean.jraw.tree.CommentTreeSettings
import net.dean.jraw.tree.RootCommentNode
import java.util.Date

class SyntheticSubmissionAndComments {

  val submission = SyntheticSubmission()
  val comments: Listing<NestedIdentifiable>

  init {
    val body = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc."
    val body2 = "Comments (and their replies) can be collapsed by tapping on them."
    val body3 = "Drag the cat's image downwards and release to close this tutorial."

    val comment2 = SyntheticComment(body2, submission)
    val comment1 = SyntheticComment(body, submission, listOf(comment2))
    val comment3 = SyntheticComment(body3, submission)

    comments = Listing.create(null, listOf(comment1, comment2, comment3))
  }

  fun toNonSynthetic(): SubmissionAndComments {
    val rootCommentNode = RootCommentNode(submission, comments, CommentTreeSettings(submission.id, submission.suggestedSort!!))
    return SubmissionAndComments(submission, Optional.of(rootCommentNode))
  }
}

class SyntheticSubmission : Submission() {

  companion object {
    val commentSubredditId = "t5_3kfea"
    val commentAuthorName = "Dank"
    val commentCreatedTimeUtc = System.currentTimeMillis()

    val comment1 = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc."

    //String comment2 = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc.";
    //String comment2Html = comment2;

    val comment3 = "Comments (and their replies) can be collapsed by tapping on them."

    val comment4 = "Drag the cat's image downwards and release to close this tutorial."

    val SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH = "https://i.imgur.com/NaWfFWR.jpg"

    const val SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH = "syntheticsubmissionforgesturewalkthrough"
    val SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH = "${FullNameType.SUBMISSION.prefix()} + syntheticsubmissionforgesturewalkthrough"
  }

  override fun getLinkFlairText(): String? {
    return null
  }

  override fun getPreview(): SubmissionPreview? {
    return null
  }

  override fun isVisited(): Boolean {
    return false
  }

  override fun isNsfw(): Boolean {
    return false
  }

  override fun isRemoved(): Boolean {
    return false
  }

  override fun isLocked(): Boolean {
    return false
  }

  override fun getEmbeddedMedia(): EmbeddedMedia? {
    return null
  }

  override fun getSelfText(): String? {
    return null
  }

  override fun getDomain(): String {
    return "i.imgur.com"
  }

  override fun getTitle(): String {
    return "Here's a heart-warming photo to start your journey"
  }

  override fun isHidden(): Boolean {
    return false
  }

  override fun isContestMode(): Boolean {
    return false
  }

  override fun getUrl(): String {
    return SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH
  }

  override fun getCrosspostParents(): MutableList<Submission>? {
    return null
  }

  override fun getPostHint(): String? {
    return "image"
  }

  override fun getCommentCount(): Int {
    return 4
  }

  override fun getSuggestedSort(): CommentSort? {
    return Reddit.DEFAULT_COMMENT_SORT
  }

  override fun getReports(): Int? {
    return 0
  }

  override fun getLinkFlairCssClass(): String? {
    return null
  }

  override fun getPermalink(): String {
    return "/r/GetDank"
  }

  override fun isSpoiler(): Boolean {
    return false
  }

  override fun isSelfPost(): Boolean {
    return false
  }

  override fun getThumbnail(): String? {
    return null
  }

  override fun getAuthorFlairText(): String? {
    return null
  }

  override fun isQuarantine(): Boolean {
    return false
  }

  override fun isSpam(): Boolean {
    return false
  }

  override val author: String
    get() = "Saketme"

  override val body: String?
    get() = null

  override val edited: Date?
    get() = null

  override val isArchived: Boolean
    get() = false

  override val isSaved: Boolean
    get() = false

  override val isScoreHidden: Boolean
    get() = false

  override val isStickied: Boolean
    get() = false

  override val subreddit: String
    get() = "GetDank"

  override val subredditFullName: String
    get() = "t5_32wow"

  override val created: Date
    get() = Date(System.currentTimeMillis())

  override val distinguished: DistinguishedStatus
    get() = DistinguishedStatus.NORMAL

  override val gilded: Short
    get() = 0

  override val isGildable: Boolean
    get() = false

  override val fullName: String
    get() = SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH

  override val id: String
    get() = SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH

  override val score: Int
    get() = 1

  override val vote: VoteDirection
    get() = VoteDirection.NONE
}

class SyntheticComment(override val body: String, private val parent: Submission, val replies: List<Comment> = listOf()) : Comment() {

  override fun getUrl(): String? {
    return null
  }

  override fun getSubredditType(): Subreddit.Access {
    return Subreddit.Access.PUBLIC
  }

  override fun getSubmissionTitle(): String? {
    return null
  }

  override fun getPermalink(): String {
    return parent.permalink
  }

  override fun getControversiality(): Int {
    return 0
  }

  override fun getAuthorFlairText(): String? {
    return null
  }

  override fun getReplies(): Listing<NestedIdentifiable> {
    return Listing.create(null, replies)
  }

  override fun getSubmissionFullName(): String {
    return parent.fullName
  }

  override val author: String
    get() = "Dank"

  override val edited: Date?
    get() = null

  override val isArchived: Boolean
    get() = false

  override val isSaved: Boolean
    get() = false

  override val isScoreHidden: Boolean
    get() = false

  override val isStickied: Boolean
    get() = false

  override val subreddit: String
    get() = "GetDank"

  override val subredditFullName: String
    get() = "t5_3kfea"

  override val created: Date
    get() = Date(System.currentTimeMillis())

  override val distinguished: DistinguishedStatus
    get() = DistinguishedStatus.NORMAL

  override val gilded: Short
    get() = 0

  override val isGildable: Boolean
    get() = false

  override val fullName: String
    get() = "t1_$id"

  override val id: String
    get() = "dyhg7y2"

  override val score: Int
    get() = 1

  override val vote: VoteDirection
    get() = VoteDirection.NONE

  override val parentFullName: String
    get() = "t1_dyhfxah"
}
