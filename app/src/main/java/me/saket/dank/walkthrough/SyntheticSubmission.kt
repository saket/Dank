package me.saket.dank.walkthrough

import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.submission.SubmissionAndComments
import me.saket.dank.utils.Optional
import net.dean.jraw.models.*
import net.dean.jraw.tree.CommentTreeSettings
import net.dean.jraw.tree.RootCommentNode
import java.util.*

class SyntheticSubmissionAndComments {

  val submission: Submission
  val comments: Listing<NestedIdentifiable>

  init {
    // TODO: Get these from localized strings.
    val body = "Both the submission title and comments can be swiped horizontally to reveal actions like upvote, options, etc."
    val body2 = "Comments (and their replies) can be collapsed by tapping on them."
    val body3 = "Drag the cat's image downwards and release to close this tutorial."

    submission = SyntheticSubmission(commentCount = 3, title = "Here's a heart-warming photo to start your journey")

    val comment2 = SyntheticComment(body2, submission)
    val comment1And2 = SyntheticComment(body, submission, listOf(comment2))
    val comment3 = SyntheticComment(body3, submission)

    comments = Listing.create(null, listOf(comment1And2, comment3))
  }

  fun toNonSynthetic(): SubmissionAndComments {
    val rootCommentNode = RootCommentNode(submission, comments, CommentTreeSettings(submission.id, submission.suggestedSort!!))
    return SubmissionAndComments(submission, Optional.of(rootCommentNode))
  }
}

class SyntheticSubmission(
  private val commentCount: Int,
  private val title: String
) : Submission() {

  override fun getCommentCount(): Int = commentCount

  override fun getLinkFlairText(): String? = null

  override fun getPreview(): SubmissionPreview? = null

  override fun isVisited(): Boolean = false

  override fun isNsfw(): Boolean = false

  override fun isRemoved(): Boolean = false

  override fun isLocked(): Boolean = false

  override fun getEmbeddedMedia(): EmbeddedMedia? = null

  override fun getSelfText(): String? = null

  override fun getDomain(): String = "i.imgur.com"

  override fun getTitle(): String = title

  override fun isHidden(): Boolean = false

  override fun isContestMode(): Boolean = false

  override fun getUrl(): String = SyntheticData.SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH

  override fun getCrosspostParents(): MutableList<Submission>? = null

  override fun getPostHint(): String? = "image"

  override fun getSuggestedSort(): CommentSort? = Reddit.DEFAULT_COMMENT_SORT

  override fun getReports(): Int? = 0

  override fun getLinkFlairCssClass(): String? = null

  override fun getPermalink(): String = "/r/GetDank"

  override fun isSpoiler(): Boolean = false

  override fun isSelfPost(): Boolean = false

  override fun getThumbnail(): String? = null

  override fun getAuthorFlairText(): String? = null

  override fun isQuarantine(): Boolean = false

  override fun isSpam() = false

  override val author: String = "Saketme"

  override val body: String? = null

  override val edited: Date? = null

  override val isArchived: Boolean = false

  override val isSaved: Boolean = false

  override val isScoreHidden: Boolean = false

  override val isStickied: Boolean = false

  override val subreddit: String = "GetDank"

  override val subredditFullName: String = "t5_32wow"

  override val created: Date = Date(System.currentTimeMillis())

  override val distinguished: DistinguishedStatus = DistinguishedStatus.NORMAL

  override val gilded: Short = 0

  override val isGildable: Boolean = false

  override val fullName: String = SyntheticData.SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH

  override val id: String = SyntheticData.SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH

  override val score: Int = 1

  override val vote: VoteDirection = VoteDirection.NONE
}

class SyntheticComment(
    override val body: String,
    private val parent: Submission,
    private val replies: List<Comment> = listOf()
) : Comment() {

  override fun getUrl(): String? = null

  override fun getSubredditType(): Subreddit.Access = Subreddit.Access.PUBLIC

  override fun getSubmissionTitle(): String? = null

  override fun getPermalink(): String = parent.permalink

  override fun getControversiality(): Int = 0

  override fun getAuthorFlairText(): String? = null

  override fun getReplies(): Listing<NestedIdentifiable> = Listing.create(null, replies)

  override fun getSubmissionFullName(): String = parent.fullName

  override val author: String = "Dank"

  override val edited: Date? = null

  override val isArchived: Boolean = false

  override val isSaved: Boolean = false

  override val isScoreHidden: Boolean = false

  override val isStickied: Boolean = false

  override val subreddit: String = "GetDank"

  override val subredditFullName: String = "t5_3kfea"

  override val created: Date = Date(System.currentTimeMillis())

  override val distinguished: DistinguishedStatus = DistinguishedStatus.NORMAL

  override val gilded: Short = 0

  override val isGildable: Boolean = false

  override val id: String = "${body.hashCode()}"

  override val fullName: String = "t1_$id"

  override val score: Int = 1

  override val vote: VoteDirection = VoteDirection.NONE

  override val parentFullName: String = "t1_parent_comment_fullname"
}
