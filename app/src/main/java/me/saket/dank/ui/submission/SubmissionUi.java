package me.saket.dank.ui.submission;

import net.dean.jraw.models.Submission;

import me.saket.dank.ui.submission.events.SubmissionChangeCommentSortClicked;
import me.saket.dank.utils.DankSubmissionRequest;

interface SubmissionUi {

  void showProgress();

  void hideProgress();

  void acceptRequest(DankSubmissionRequest lastRequest);

  void showChangeSortPopup(SubmissionChangeCommentSortClicked event, DankSubmissionRequest activeRequest);

  void acceptSubmission(Submission submission);
}
