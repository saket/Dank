package me.saket.dank.ui.submission;

/**
 * Represents one row in the comments section.
 */
@Deprecated
public interface SubmissionCommentRow {

  enum Type {
    USER_COMMENT,
    PENDING_SYNC_REPLY,
    INLINE_REPLY,
    LOAD_MORE_COMMENTS,
  }

  String adapterId();

  Type type();
}
