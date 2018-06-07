package me.saket.dank.data

import me.saket.dank.reply.PendingSyncReply
import me.saket.dank.reply.PendingSyncReply.State.FAILED
import me.saket.dank.reply.PendingSyncReply.State.POSTED
import me.saket.dank.reply.PendingSyncReply.State.POSTING
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

  override val id: String
    get() = fullNameIfPostedOrThrowError().substring(0, FullNameType.COMMENT.prefix().length)

  override val uniqueId: String
    get() = fullName


  val postingStatusIndependentId = "${pendingSyncReply.parentContributionFullName()}_reply_${pendingSyncReply.createdTimeMillis()}"
}
