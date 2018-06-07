package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import me.saket.dank.reply.PendingSyncReply;
import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class ReplyRetrySendClickEvent implements UiEvent {

  public abstract PendingSyncReply failedPendingSyncReply();

  public static ReplyRetrySendClickEvent create(PendingSyncReply failedPendingSyncReply) {
    return new AutoValue_ReplyRetrySendClickEvent(failedPendingSyncReply);
  }
}
