package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;

import me.saket.dank.ui.UiEvent;

@AutoValue
public abstract class MarkMessageAsReadRequested implements UiEvent {

  public abstract Message message();

  public static MarkMessageAsReadRequested create(Message message) {
    return new AutoValue_MarkMessageAsReadRequested(message);
  }
}
