package me.saket.dank.reddit.jraw

import io.reactivex.Observable
import io.reactivex.Single
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.subreddit.SubredditSearchResult
import me.saket.dank.ui.subreddit.Subscribeable
import net.dean.jraw.ApiException
import net.dean.jraw.RedditClient
import net.dean.jraw.http.NetworkException
import net.dean.jraw.models.Listing
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.models.TimePeriod

class JrawSubreddits(private val clients: Observable<RedditClient>) : Reddit.Subreddits {

  override fun find(subredditName: String): Single<SubredditSearchResult> {
    return findOld(subredditName)
        .map { SubredditSearchResult.success(it) }
        .onErrorReturn { error ->
          if (error is IllegalArgumentException && error.message?.contains("is private") == true) {
            SubredditSearchResult.privateError()

          } else if (error is IllegalArgumentException && (error.message?.contains("does not exist") == true)
              || (error is NetworkException && error.res.code == 404)) {
            SubredditSearchResult.notFound()

          } else {
            SubredditSearchResult.unknownError(error)
          }
        }
  }

  override fun findOld(subredditName: String): Single<Subscribeable> {
    if (subredditName.equals("frontpage", ignoreCase = true)) {
      return Single.just(Subscribeable.local("Frontpage"))
    }
    if (subredditName.equals("popular", ignoreCase = true)) {
      return Single.just(Subscribeable.local("Popular"))
    }
    if (subredditName.equals("all", ignoreCase = true)) {
      return Single.just(Subscribeable.local("All"))
    }

    return clients
        .firstOrError()
        .map { it.subreddit(subredditName).about() }
        .map { Subscribeable.create(it) }
  }

  override fun parseSubmissionPaginationError(error: Throwable): SubredditSearchResult {
    return when {
      error is ApiException && error.code == "403" -> SubredditSearchResult.privateError()
      error is NetworkException && error.res.code == 404 -> SubredditSearchResult.notFound()
      error is NullPointerException && "Null id".equals(error.message, ignoreCase = true) -> {
        // https://github.com/mattbdean/JRAW/issues/261
        SubredditSearchResult.notFound()
      }
      else -> SubredditSearchResult.unknownError(error)
    }
  }

  override fun submissions(
      subredditName: String,
      isFrontpage: Boolean,
      sorting: SubredditSort,
      timePeriod: TimePeriod
  ): Iterator<Listing<Submission>> {

    return submissions(subredditName, isFrontpage, sorting, timePeriod, null)
  }

  override fun submissions(
      subredditName: String,
      isFrontpage: Boolean,
      sorting: SubredditSort,
      timePeriod: TimePeriod,
      anchorFullName: String?
  ): Iterator<Listing<Submission>> {

    // blockingFirst() is not very ideal, but I haven't got any other option here.
    val client = clients.blockingFirst()

    val paginatorBuilder = when {
      isFrontpage -> client.frontPage()
      else -> client.subreddit(subredditName).posts()
    }

    return paginatorBuilder
        .sorting(sorting)
        .timePeriod(timePeriod)
        .apply {
          if (anchorFullName != null) {
            customAnchor(anchorFullName)
          }
        }
        .build()
        .iterator()
  }
}
