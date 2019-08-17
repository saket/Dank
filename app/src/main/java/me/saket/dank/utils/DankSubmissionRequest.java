package me.saket.dank.utils;

import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import net.dean.jraw.models.CommentSort;
import net.dean.jraw.references.CommentsRequest;

import me.saket.dank.ui.submission.AuditedCommentSort;

/**
 * Because {@link CommentsRequest} does not have a toBuilder() method. Update: Then send a PR to JRAW?
 * <p>
 * TODO Use CommentsRequest#copy().
 */
@AutoValue
public abstract class DankSubmissionRequest implements Parcelable {

  /** Submission Id */
  public abstract String id();

  public abstract AuditedCommentSort commentSort();

  @Nullable
  public abstract String focusCommentId();

  @Nullable
  public abstract Integer contextCount();

  @Nullable
  public abstract Integer commentLimit();

  public static Builder builder(String submissionId) {
    return new AutoValue_DankSubmissionRequest.Builder().id(submissionId);
  }

  public abstract Builder toBuilder();

  public CommentsRequest toJraw() {
    // Note: context count is only null in case of "continue thread" requests.
    return new CommentsRequest.Builder()
        .sort(commentSort().mode())
        .focus(focusCommentId())
        .context(contextCount())
        .limit(commentLimit())
        .build();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    @SuppressWarnings("unused")
    abstract Builder id(String id);

    /**
     * Sets the ID of the comment to focus on. If this comment does not exist, then this parameter is ignored.
     * Otherwise, only one comment tree is returned: the one in which the given comment resides.
     */
    public abstract Builder focusCommentId(@Nullable String commentId);

    /**
     * Sets the sorting for the comments in the response. This will be used to request more comments using
     * {@link CommentNode#loadMoreComments}. A null value will exclude this parameter.
     */
    public abstract Builder commentSort(AuditedCommentSort sort);

    /** See {@link #commentSort(AuditedCommentSort)}. */
    public Builder commentSort(CommentSort sort, AuditedCommentSort.SelectedBy selectedBy) {
      return commentSort(AuditedCommentSort.create(sort, selectedBy));
    }

    /**
     * <p>Sets the number of parents shown in relation to the focused comment. For example, if the focused comment is
     * in the eighth level of the comment tree (meaning there are seven replies above it), and the context is set to
     * six, then the response will also contain the six direct parents of the given comment. For a better
     * understanding, play with
     * <a href="https://www.reddit.com/comments/92dd8?comment=c0b73aj&context=8">this link</a>.
     * <p>
     * <p>A null value will exclude this parameter.
     */
    public abstract Builder contextCount(@Nullable Integer context);

    /**
     * Sets the maximum amount of comments to return. A null value will exclude this parameter.
     */
    public abstract Builder commentLimit(@Nullable Integer limit);

    /**
     * Creates a new SubmissionRequest
     */
    public abstract DankSubmissionRequest build();
  }

  public static JsonAdapter<DankSubmissionRequest> jsonAdapter(Moshi moshi) {
    return new AutoValue_DankSubmissionRequest.MoshiJsonAdapter(moshi);
  }
}
