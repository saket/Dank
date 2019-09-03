package me.saket.dank.ui.user.messages;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;

@AutoValue
public abstract class MessageClickEvent {

  public abstract Message message();

  public abstract View itemView();

  public static MessageClickEvent create(Message message, View itemView) {
    return new AutoValue_MessageClickEvent(message, itemView);
  }
}
