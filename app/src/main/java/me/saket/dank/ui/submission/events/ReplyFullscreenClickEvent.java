package me.saket.dank.ui.submission.events;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.ui.compose.ComposeReplyActivity;
import me.saket.dank.ui.compose.ComposeStartOptions;

@AutoValue
public abstract class ReplyFullscreenClickEvent {

  public abstract long replyRowItemId();

  public abstract ContributionFullNameWrapper parentContribution();

  @Nullable
  public abstract String authorNameIfComment();

  public static ReplyFullscreenClickEvent create(long replyRowItemId, ContributionFullNameWrapper parentContribution, String authorNameIfComment) {
    return new AutoValue_ReplyFullscreenClickEvent(replyRowItemId, parentContribution, authorNameIfComment);
  }

  public void openComposeForResult(Activity activity, Bundle payload, int requestCode) {
    ComposeStartOptions startOptions = ComposeStartOptions.builder()
        .secondPartyName(authorNameIfComment())
        .parentContribution(parentContribution())
        .draftKey(parentContribution())
        .extras(payload)
        .build();
    activity.startActivityForResult(ComposeReplyActivity.intent(activity, startOptions), requestCode);
  }
}
