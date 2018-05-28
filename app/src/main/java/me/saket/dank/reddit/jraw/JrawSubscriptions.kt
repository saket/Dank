package me.saket.dank.reddit.jraw

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.saket.dank.reddit.Reddit
import net.dean.jraw.RedditClient
import net.dean.jraw.models.Subreddit
import net.dean.jraw.pagination.Paginator

/** TODO: Should be merged to [JrawLoggedInUser] */
class JrawSubscriptions(private val clients: Observable<RedditClient>) : Reddit.Subscriptions {

  override fun userSubscriptions(): Single<List<Subreddit>> {
    return clients
        .firstOrError()
        .map {
          it.me()
              .subreddits("subscriber")
              .limit(Paginator.RECOMMENDED_MAX_LIMIT)
              .build()
              .accumulateMerged(200)
        }
  }

  override fun add(subreddit: Subreddit): Completable {
    return clients
        .firstOrError()
        .flatMapCompletable { Completable.fromAction { it.subreddit(subreddit.name).subscribe() } }
  }

  override fun remove(subreddit: Subreddit): Completable {
    return clients
        .firstOrError()
        .flatMapCompletable { Completable.fromAction { it.subreddit(subreddit.name).unsubscribe() } }
  }
}
