package me.saket.dank.ui.submission.events;

import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.CommentNode;

// WARNING: DO NOT ADD NEW FIELDS WITHOUT UPDATING EQUALS()!
@AutoValue
public abstract class LoadMoreCommentsClickEvent {

  /**
   * Node whose more comments have to be fetched.
   */
  public abstract CommentNode parentCommentNode();

  /**
   * Clicked itemView.
   */
  public abstract View loadMoreItemView();

  public static LoadMoreCommentsClickEvent create(CommentNode parentNode, View loadMoreItemView) {
    return new AutoValue_LoadMoreCommentsClickEvent(parentNode, loadMoreItemView);
  }

  // Custom equals() because CommentNode.equals() often results in a stackoverflow :/.
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof LoadMoreCommentsClickEvent) {
      LoadMoreCommentsClickEvent that = (LoadMoreCommentsClickEvent) o;
      return (this.parentCommentNode().getComment().getFullName().equals(that.parentCommentNode().getComment().getFullName()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h = 1;
    h *= 1000003;
    h ^= this.parentCommentNode().getComment().getFullName().hashCode();
    return h;
  }
}
