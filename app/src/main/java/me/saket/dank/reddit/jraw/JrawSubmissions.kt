package me.saket.dank.reddit.jraw

import io.reactivex.Observable
import io.reactivex.Single
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.submission.SubmissionAndComments
import me.saket.dank.utils.DankSubmissionRequest
import net.dean.jraw.RedditClient
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.tree.CommentNode
import net.dean.jraw.tree.RootCommentNode
import timber.log.Timber
import javax.inject.Inject

class JrawSubmissions @Inject constructor(private val clients: Observable<RedditClient>) : Reddit.Submissions {

  override fun fetch(request: DankSubmissionRequest): Single<RootCommentNode> {
    return clients
        .firstOrError()
        .map {
          Timber.i("Fetching comments")
          it.submission(request.id()).comments(request.toJraw())
        }
  }

  override fun <T : PublicContribution<*>> fetchMoreComments(
      submissionData: SubmissionAndComments,
      commentNode: CommentNode<T>
  ): Single<SubmissionAndComments> {
    // JRAW inserts the new comments directly inside submission's comment tree, which we
    // do not want because we treat persistence as the single source of truth. So we'll
    // instead return the submission so that it gets saved to to DB and let the UI
    // update itself.
    return clients
        .firstOrError()
        .map {
          commentNode.replaceMore(it)
          submissionData
        }
  }
}
