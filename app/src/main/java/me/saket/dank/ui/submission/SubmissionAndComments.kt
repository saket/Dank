package me.saket.dank.ui.submission

import com.squareup.moshi.JsonClass
import me.saket.dank.utils.Optional
import net.dean.jraw.models.Submission
import net.dean.jraw.tree.RootCommentNode

// TODO: Merge this with CachedSubmissionAndComments.
@JsonClass(generateAdapter = true)
data class SubmissionAndComments constructor(val submission: Submission, val comments: Optional<RootCommentNode> = Optional.empty()) {

  companion object {
    fun from(tuple: CachedSubmissionAndComments): SubmissionAndComments {
      return SubmissionAndComments(tuple.submission, tuple.comments())
    }
  }
}
