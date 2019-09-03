package me.saket.dank.vote

import net.dean.jraw.models.Identifiable
import net.dean.jraw.models.PublicContribution
import net.dean.jraw.models.Votable
import net.dean.jraw.models.VoteDirection

data class SimpleVotable(
    override val score: Int,
    override val vote: VoteDirection,
    override val fullName: String,
    override val id: String
) : Identifiable, Votable {

  override val uniqueId: String
    get() = fullName

  companion object {

    fun from(contribution: PublicContribution<*>): SimpleVotable {
      return SimpleVotable(
          score = contribution.score,
          vote = contribution.vote,
          fullName = contribution.fullName,
          id = contribution.id)
    }
  }
}
