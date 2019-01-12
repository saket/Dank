package me.saket.dank.reddit

import io.reactivex.Completable
import io.reactivex.Single
import me.saket.dank.data.PaginationAnchor
import me.saket.dank.reddit.jraw.UserLoginHelper
import me.saket.dank.ui.submission.SubmissionAndComments
import me.saket.dank.ui.subreddit.SubredditSearchResult
import me.saket.dank.ui.subreddit.Subscribeable
import me.saket.dank.ui.user.messages.InboxFolder
import me.saket.dank.utils.DankSubmissionRequest
import net.dean.jraw.models.Account
import net.dean.jraw.models.AccountQuery
import net.dean.jraw.models.CommentSort
import net.dean.jraw.models.Identifiable
import net.dean.jraw.models.Listing
import net.dean.jraw.models.Message
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Submission
import net.dean.jraw.models.Subreddit
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.TimePeriod
import net.dean.jraw.models.VoteDirection
import net.dean.jraw.tree.CommentNode
import net.dean.jraw.tree.RootCommentNode

interface Reddit {

  companion object {
    const val CONTEXT_QUERY_PARAM = "context"
    const val COMMENT_DEFAULT_CONTEXT_COUNT = 3

    // "Confidence" is now "Best" in Reddit's API.
    val DEFAULT_COMMENT_SORT = CommentSort.CONFIDENCE

    @get:JvmName("DEFAULT_SUBREDDIT_SORT")
    val DEFAULT_SUBREDDIT_SORT = SubredditSort.BEST
  }

  fun submissions(): Submissions

  fun subreddits(): Subreddits

  fun subscriptions(): Subscriptions

  fun loggedInUser(): LoggedInUser

  fun users(): Users

  fun login(): Logins

  interface Subreddits {

    fun find(subredditName: String): Single<SubredditSearchResult>

    @Deprecated(replaceWith = ReplaceWith("find()"), level = DeprecationLevel.WARNING, message = "Use find() instead")
    fun findOld(subredditName: String): Single<Subscribeable>

    fun parseSubmissionPaginationError(error: Throwable): SubredditSearchResult

    fun submissions(
        subredditName: String,
        isFrontpage: Boolean,
        sorting: SubredditSort,
        timePeriod: TimePeriod
    ): Iterator<Listing<Submission>>

    fun submissions(
        subredditName: String,
        isFrontpage: Boolean,
        sorting: SubredditSort,
        timePeriod: TimePeriod,
        anchorFullName: String?
    ): Iterator<Listing<Submission>>
  }

  interface Submissions {

    fun fetch(request: DankSubmissionRequest): Single<RootCommentNode>

    fun <T : PublicContribution<*>>  fetchMoreComments(submissionData: SubmissionAndComments, commentNode: CommentNode<T>): Single<SubmissionAndComments>
  }

  interface Subscriptions {

    fun userSubscriptions(): Single<List<Subreddit>>

    fun add(subreddit: Subreddit): Completable

    fun remove(subreddit: Subreddit): Completable

    fun needsRemoteSubscription(subredditName: String): Boolean {
      return !subredditName.equals("frontpage", ignoreCase = true) && !subredditName.equals("popular", ignoreCase = true)
    }
  }

  interface LoggedInUser {

    fun about(): Single<Account>

    fun logout(): Completable

    fun reply(parent: Identifiable, body: String): Single<out Identifiable>

    fun vote(thing: Identifiable, voteDirection: VoteDirection): Completable

    fun messages(folder: InboxFolder, limit: Int, paginationAnchor: PaginationAnchor): Single<Iterator<Listing<Message>>>

    fun setMessagesRead(read: Boolean, vararg messages: Identifiable): Completable

    fun setAllMessagesRead(): Completable
  }

  interface Users {

    fun fetch(username: String): Single<AccountQuery>
  }

  interface Logins {

    fun loginHelper(): UserLoginHelper
  }
}
