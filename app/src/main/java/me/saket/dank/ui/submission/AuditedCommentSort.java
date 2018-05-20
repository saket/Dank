package me.saket.dank.ui.submission;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.CommentSort;

@AutoValue
public abstract class AuditedCommentSort implements Parcelable {

  public enum SelectedBy {
    USER,
    DEFAULT,
    SUBMISSION_SUGGESTED
  }

  public abstract CommentSort mode();

  public abstract SelectedBy selectedBy();

  public boolean canOverrideWithSuggestedSort() {
    return selectedBy() == SelectedBy.DEFAULT;
  }

  public static AuditedCommentSort create(CommentSort sort, SelectedBy selectedBy) {
    return new AutoValue_AuditedCommentSort(sort, selectedBy);
  }

  public static JsonAdapter<AuditedCommentSort> jsonAdapter(Moshi moshi) {
    return new AutoValue_AuditedCommentSort.MoshiJsonAdapter(moshi);
  }
}
