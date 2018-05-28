package me.saket.dank.reddit.jraw

import io.reactivex.Observable
import io.reactivex.Single
import me.saket.dank.reddit.Reddit
import net.dean.jraw.RedditClient
import net.dean.jraw.models.AccountQuery

class JrawUsers(private val clients: Observable<RedditClient>) : Reddit.Users {

  override fun fetch(username: String): Single<AccountQuery> {
    return clients
        .firstOrError()
        .map { it.user(username).query() }
  }
}
