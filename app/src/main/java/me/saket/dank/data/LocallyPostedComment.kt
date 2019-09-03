package me.saket.dank.data

import me.saket.dank.reply.PendingSyncReply
import me.saket.dank.reply.PendingSyncReply.State.*
import net.dean.jraw.models.Identifiable

data class LocallyPostedComment(val pendingSyncReply: PendingSyncReply) : Identifiable {

  override val fullName: String
    get() = fullNameIfPostedOrThrowError()

  private fun fullNameIfPostedOrThrowError(): String {
    return when (pendingSyncReply.state()!!) {
      POSTING, FAILED -> throw AssertionError("Comment hasn't been posted yet.")
      POSTED -> pendingSyncReply.postedFullName()!!
    }
  }

  val isPosted = pendingSyncReply.state() == POSTED

  override val id: String
    get() = fullNameIfPostedOrThrowError().substring(0, FullNameType.COMMENT.prefix().length)

  override val uniqueId: String
    get() = fullName


  val postingStatusIndependentId = "${pendingSyncReply.parentContributionFullName()}_reply_${pendingSyncReply.createdTimeMillis()}"
}
