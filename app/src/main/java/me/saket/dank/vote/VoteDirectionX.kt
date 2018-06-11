package me.saket.dank.vote

import net.dean.jraw.models.VoteDirection

class VoteDirectionX {

  companion object {

    /**
     * [VoteDirection] had different enum names before v0.6.1.
     */
    @JvmStatic
    fun valueOfWithMigration(voteString: String): VoteDirection {
      return when (voteString) {
        "UPVOTE" -> VoteDirection.UP
        "DOWNVOTE" -> VoteDirection.DOWN
        "NO_VOTE" -> VoteDirection.NONE
        else -> VoteDirection.valueOf(voteString)
      }
    }
  }
}
