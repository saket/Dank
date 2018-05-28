package me.saket.dank.ui.submission

sealed class LocalOrRemoteComment()

class LocallyPostedComment2 : LocalOrRemoteComment() {

}

class RemotePostedComment : LocalOrRemoteComment() {

}

/**
 * TODO: Should this implement [PublicContribution] instead?
 */
//interface LocalOrRemoteComment : NestedIdentifiable, Votable {
//  override val fullName: String
//  override val id: String
//  override val parentFullName: String
//  override val uniqueId: String
//  override val score: Int
//  override val vote: VoteDirection
//  val body: String
//  val permalink: String
//  val author: String
//  val submissionFullName: String
//  val wasLocallyPosted: Boolean
//
//  companion object {
//    fun local(pendingSyncReply: PendingSyncReply): LocalOrRemoteComment {
//      LocallyPostedComment2(
//          fullName = pendingSyncReply.postedFullName()
//      )
//    }
//  }
//}
//
//class LocallyPostedComment2(
//    override val fullName: String,
//    override val id: String,
//    override val parentFullName: String,
//    override val uniqueId: String,
//    override val score: Int,
//    override val vote: VoteDirection,
//    override val body: String,
//    override val permalink: String,
//    override val author: String,
//    override val submissionFullName: String,
//    val pendingSyncReply: PendingSyncReply
//) : LocalOrRemoteComment {
//
//  override val wasLocallyPosted: Boolean
//    get() = true
//
//  val postingStatusIndependentId: String = "${pendingSyncReply.parentContributionFullName()}_reply_${pendingSyncReply.createdTimeMillis()}"
//}
//
//class RemotePostedComment : LocalOrRemoteComment {
//}
