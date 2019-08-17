package me.saket.dank.ui.submission.events;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Identifiable;

import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;

@AutoValue
public abstract class ReplyFullscreenClickEvent {

  public abstract long replyRowItemId();

  public abstract Identifiable parent();

  @Nullable
  public abstract String authorNameIfComment();

  public static ReplyFullscreenClickEvent create(long replyRowItemId, Identifiable parent, String authorNameIfComment) {
    return new AutoValue_ReplyFullscreenClickEvent(replyRowItemId, parent, authorNameIfComment);
  }

  public void openComposeForResult(Activity activity, Bundle payload, int requestCode) {
    ComposeStartOptions startOptions = ComposeStartOptions.builder()
        .secondPartyName(authorNameIfComment())
        .parent(parent())
        .draftKey(parent())
        .extras(payload)
        .build();
    activity.startActivityForResult(ComposeReplyActivity.intent(activity, startOptions), requestCode);
  }
}
