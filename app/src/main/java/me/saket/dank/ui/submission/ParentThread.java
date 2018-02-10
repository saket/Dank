package me.saket.dank.ui.submission;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;
import net.dean.jraw.models.Submission;

/**
 * Thread == submission comment thread or private message thread.
 */
@AutoValue
public abstract class ParentThread {

  public enum Type {
    SUBMISSION {
      @Override
      public String fullNamePrefix() {
        return "t4_";
      }
    },
    PRIVATE_MESSAGE {
      @Override
      public String fullNamePrefix() {
        return "t1_";
      }
    };

    public String fullNamePrefix() {
      throw new AbstractMethodError();
    }
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
