package me.saket.dank.data;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Message;

@AutoValue
public abstract class MessageFullNameWrapper extends StubMessage implements Parcelable {

  public abstract String fullName();

  public abstract String body();

  @Override
  public String getFullName() {
    return fullName();
  }

  @Override
  public String getBody() {
    return body();
  }

  public static MessageFullNameWrapper create(Message message) {
    return new AutoValue_MessageFullNameWrapper(message.getFullName(), message.getBody());
  }
}
