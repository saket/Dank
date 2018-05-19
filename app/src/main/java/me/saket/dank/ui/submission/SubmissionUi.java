package me.saket.dank.ui.submission;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.submission.events.ChangeCommentSortingClicked;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Optional;

interface SubmissionUi {

  void showChangeSortPopup(ChangeCommentSortingClicked event);

  void populateUi(Optional<Submission> submission, DankSubmissionRequest request, Optional<String> callingSubreddit);
}
