package me.saket.dank.walkthrough

import me.saket.dank.data.FullNameType
import net.dean.jraw.models.Comment
import net.dean.jraw.models.Submission
import javax.inject.Inject

class SyntheticData @Inject constructor() {

  companion object {
    const val SUBMISSION_IMAGE_URL_FOR_GESTURE_WALKTHROUGH = "https://i.imgur.com/NaWfFWR.jpg"
    const val SUBMISSION_ID_FOR_GESTURE_WALKTHROUGH = "syntheticsubmissionforgesturewalkthrough"
    val SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH = "${FullNameType.SUBMISSION.prefix()} + syntheticsubmissionforgesturewalkthrough"

    fun isSynthetic(comment: Comment): Boolean {
      return comment.submissionFullName.equals(SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH, ignoreCase = true)
    }

    fun isSynthetic(submission: Submission): Boolean {
      return submission.fullName.equals(SUBMISSION_FULLNAME_FOR_GESTURE_WALKTHROUGH, ignoreCase = true)
    }
  }
}

