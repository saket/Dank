package me.saket.dank.utils;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import net.dean.jraw.http.SubmissionRequest;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.CommentSort;

/**
 * Because {@link SubmissionRequest} does not have a toBuilder() method.
 */
@AutoValue
public abstract class DankSubmissionRequest implements Parcelable {

  public abstract String id();

  public abstract CommentSort commentSort();

  @Nullable
  public abstract String focusComment();

  @Nullable
  public abstract Integer contextCount();

  /**
   * @param id Submission id.
   */
  public static Builder builder(String id) {
    return new AutoValue_DankSubmissionRequest.Builder().id(id);
  }

  public Builder toBuilder() {
    return new AutoValue_DankSubmissionRequest.Builder(this);
  }

  @Nullable
  public abstract Integer commentLimit();

  @AutoValue.Builder
  public abstract static class Builder {
    @SuppressWarnings("unused")
    abstract Builder id(String id);

    /**
     * Sets the ID of the comment to focus on. If this comment does not exist, then this parameter is ignored.
     * Otherwise, only one comment tree is returned: the one in which the given comment resides.
     */
    public abstract Builder focusComment(@Nullable String commentId);

    /**
     * Sets the sorting for the comments in the response. This will be used to request more comments using
     * {@link CommentNode#loadMoreComments}. A null value will exclude this parameter.
     */
    public abstract Builder commentSort(CommentSort sort);

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

}
