package me.saket.dank.ui.submission.events;

import android.widget.EditText;

import com.google.auto.value.AutoValue;

import me.saket.dank.ui.submission.CommentInlineReplyItem;

@AutoValue
public abstract class ReplyItemViewBindEvent {

  public abstract CommentInlineReplyItem replyItem();

  public abstract EditText replyField();

  public static ReplyItemViewBindEvent create(CommentInlineReplyItem replyItem, EditText replyField) {
    return new AutoValue_ReplyItemViewBindEvent(replyItem, replyField);
  }
}
