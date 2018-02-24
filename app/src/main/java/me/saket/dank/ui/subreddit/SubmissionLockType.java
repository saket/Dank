package me.saket.dank.ui.subreddit;

import net.dean.jraw.models.Submission;

public enum SubmissionLockType {
  LOCKED_BY_MOD,
  ARCHIVED,
  OPEN;

  public static SubmissionLockType from(Submission submission) {
    if (submission.isLocked()) {
      return LOCKED_BY_MOD;
    } else if (submission.isArchived()) {
      return ARCHIVED;
    } else {
      return OPEN;
    }
  }
}
