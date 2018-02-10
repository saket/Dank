package me.saket.dank.ui.submission.events;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Comment;

import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class CommentOptionSwipeEvent {

  public abstract Comment comment();

  public abstract SwipeableLayout itemView();

  public static CommentOptionSwipeEvent create(Comment comment, SwipeableLayout itemView) {
    return new AutoValue_CommentOptionSwipeEvent(comment, itemView);
  }
}
