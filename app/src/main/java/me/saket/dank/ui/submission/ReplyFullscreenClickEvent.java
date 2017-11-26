package me.saket.dank.ui.submission;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.PublicContribution;

@AutoValue
public abstract class ReplyFullscreenClickEvent implements Parcelable {

  public abstract long replyRowItemId();

  public abstract String parentContributionFullName();

  public abstract String replyMessage();

  @Nullable
  public abstract String authorNameIfComment();

  public static ReplyFullscreenClickEvent create(long replyRowItemId, PublicContribution parentContribution, String replyMessage,
      String authorNameIfComment)
  {
    return new AutoValue_ReplyFullscreenClickEvent(replyRowItemId, parentContribution.getFullName(), replyMessage, authorNameIfComment);
  }
}
