package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

/**
 * Parent thread == submission or a private message thread.
 */
@AutoValue
public abstract class ParentThread {

  public enum Type {
    SUBMISSION,
    PRIVATE_MESSAGE
  }

  public abstract Type type();

  public abstract String fullName();

  public static ParentThread of(Submission submission) {
    return createSubmission(submission.getFullName());
  }

  public static ParentThread of(Message parentMessage) {
    return createPrivateMessage(parentMessage.getFullName());
  }

  public static ParentThread createSubmission(String submissionFullName) {
    return new AutoValue_ParentThread(Type.SUBMISSION, submissionFullName);
  }

  public static ParentThread createPrivateMessage(String privateMessageFullName) {
    return new AutoValue_ParentThread(Type.PRIVATE_MESSAGE, privateMessageFullName);
  }
}
