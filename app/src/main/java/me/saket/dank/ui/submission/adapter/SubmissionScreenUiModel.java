package me.saket.dank.ui.submission.adapter;

/**
 * Also see {@link SubmissionContentLinkUiModel}, which does not implement this interface.
 */
public interface SubmissionScreenUiModel {

  long adapterId();

  SubmissionCommentRowType type();
}
