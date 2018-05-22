package me.saket.dank.ui.submission;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.submission.events.ChangeCommentSortingClicked;
import me.saket.dank.utils.DankSubmissionRequest;

interface SubmissionUi {

  void showProgress();

  void hideProgress();

  void acceptRequest(DankSubmissionRequest lastRequest);

  void showChangeSortPopup(ChangeCommentSortingClicked event, DankSubmissionRequest activeRequest);

  void acceptSubmission(Submission submission);
}
