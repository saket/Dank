package me.saket.dank.ui.subreddit;

import android.view.View;

import net.dean.jraw.models.Submission;

import me.saket.dank.utils.DankSubmissionRequest;

public interface SubredditUi {

  void setToolbarUserProfileIcon(SubredditUserProfileIconType iconType);

  void populateSubmission(Submission submission, DankSubmissionRequest submissionRequest, String currentSubredditName);

  void expandSubmissionRow(View submissionRowView, long submissionRowId);

  void setFabVisible(boolean visible);

  void setWindowSoftInputMode(WindowSoftInputMode mode);
}
