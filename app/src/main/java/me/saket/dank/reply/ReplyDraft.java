package me.saket.dank.reply;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class ReplyDraft {

  public abstract String body();

  /**
   * Only used for recycling old drafts.
   */
  abstract long createdTimeMillis();

  public static ReplyDraft create(String body, long createdTimeMillis) {
    return new AutoValue_ReplyDraft(body, createdTimeMillis);
  }

  public static JsonAdapter<ReplyDraft> jsonAdapter(Moshi moshi) {
    return new AutoValue_ReplyDraft.MoshiJsonAdapter(moshi);
  }
}
