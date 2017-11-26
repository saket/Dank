package me.saket.dank.ui.submission.events;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ReplyInsertGifClickEvent implements Parcelable {

  /**
   * Adapter ID of this row.
   */
  public abstract long replyRowItemId();

  public static ReplyInsertGifClickEvent create(long replyRowItemId) {
    return new AutoValue_ReplyInsertGifClickEvent(replyRowItemId);
  }
}
