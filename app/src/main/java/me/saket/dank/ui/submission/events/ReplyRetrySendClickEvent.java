package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.reply.PendingSyncReply;

@AutoValue
public abstract class ReplyRetrySendClickEvent {

  public abstract PendingSyncReply failedPendingSyncReply();

  public static ReplyRetrySendClickEvent create(PendingSyncReply failedPendingSyncReply) {
    return new AutoValue_ReplyRetrySendClickEvent(failedPendingSyncReply);
  }
}
