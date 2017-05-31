package me.saket.dank.data;

import net.dean.jraw.models.Submission;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles saving and un-saving submissions.
 */
public class SubmissionManager {

  private Set<String> savedSubmissionIds;

  public SubmissionManager() {
    this.savedSubmissionIds = new HashSet<>();
  }

// ======== SAVE ======== //

  public void save(Submission submission) {
    savedSubmissionIds.add(submission.getId());
  }

  public void unSave(Submission submission) {
    savedSubmissionIds.remove(submission.getId());
  }

  public boolean isSaved(Submission submission) {
    return savedSubmissionIds.contains(submission.getId());
  }
}
