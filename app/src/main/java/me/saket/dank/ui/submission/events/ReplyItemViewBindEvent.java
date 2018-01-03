package me.saket.dank.ui.submission.events;

import android.widget.EditText;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.submission.CommentInlineReplyItem;
import me.saket.dank.ui.submission.adapter.SubmissionCommentInlineReply;

@AutoValue
public abstract class ReplyItemViewBindEvent {

  public abstract SubmissionCommentInlineReply.UiModel uiModel();

  public abstract EditText replyField();

  public static ReplyItemViewBindEvent create(SubmissionCommentInlineReply.UiModel uiModel, EditText replyField) {
    return new AutoValue_ReplyItemViewBindEvent(uiModel, replyField);
  }
}
