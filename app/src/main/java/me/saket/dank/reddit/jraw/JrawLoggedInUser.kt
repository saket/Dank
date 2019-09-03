package me.saket.dank.reddit.jraw

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers.io
import me.saket.dank.data.FullNameType.*
import me.saket.dank.data.PaginationAnchor
import me.saket.dank.reddit.Reddit
import me.saket.dank.ui.user.messages.InboxFolder
import net.dean.jraw.RedditClient
import net.dean.jraw.models.*
import net.dean.jraw.oauth.AccountHelper

class JrawLoggedInUser(private val clients: Observable<RedditClient>, private val accountHelper: AccountHelper) : Reddit.LoggedInUser {

  @Suppress("DEPRECATION")
  override fun about(): Single<Account> {
    return clients
        .firstOrError()
        .map { it.me().about() }
  }

  override fun logout(): Completable {
    clients
        .firstOrError()
        .flatMapCompletable { Completable.fromAction { it.authManager.revokeAccessToken() } }
        .onErrorComplete()
        .subscribeOn(io())
        .subscribe()

    return Completable.fromAction {
      accountHelper.logout()
      accountHelper.switchToUserless()
    }
  }

  override fun reply(parent: Identifiable, body: String): Single<out Identifiable> {
    return clients
        .firstOrError()
        .map<Identifiable> {
          val fullNameType = parse(parent.fullName)
          when (fullNameType) {
            COMMENT -> it.comment(parent.id).reply(body)
            SUBMISSION -> it.submission(parent.id).reply(body)
            MESSAGE -> it.me().inbox().replyTo(parent.fullName, body)
            else -> throw AssertionError("Unknown contribution for reply: $parent")
          }
        }
  }

  override fun vote(thing: Identifiable, voteDirection: VoteDirection): Completable {
    return clients
        .firstOrError()
        .flatMapCompletable {
          Completable.fromAction {
            when (thing) {
              is Comment -> it.comment(thing.id).setVote(voteDirection)
              is Submission -> it.submission(thing.id).setVote(voteDirection)
              else -> throw AssertionError("Unknown contribution for vote: $thing")
            }
          }
        }
  }

  override fun messages(folder: InboxFolder, limit: Int, paginationAnchor: PaginationAnchor): Single<Iterator<Listing<Message>>> {
    return clients
        .firstOrError()
        .map {
          it.me().inbox()
              .iterate(folder.value)
              .limit(limit)
              .customAnchor(paginationAnchor.fullName())
              .build()
              .iterator()
        }
  }

  override fun setMessagesRead(read: Boolean, vararg messages: Identifiable): Completable {
    val firstMessageFullName = messages.first().fullName

    val otherMessageFullNames = messages
        .filterIndexed { index, _ -> index > 0 }
        .map { it.fullName }
        .toTypedArray()

    return clients
        .firstOrError()
        .flatMapCompletable { Completable.fromAction { it.me().inbox().markRead(read, firstMessageFullName, *otherMessageFullNames) } }
  }

  override fun setAllMessagesRead(): Completable {
    return clients
        .firstOrError()
        .flatMapCompletable { Completable.fromAction { it.me().inbox().markAllRead() } }
  }
}
