package me.saket.dank.ui.submission.adapter;

// Also see SubmissionScreenUiModel, which does not implement SubmissionScreenUiModel.
public interface SubmissionScreenUiModel {

  long adapterId();

  SubmissionCommentRowType type();
}
