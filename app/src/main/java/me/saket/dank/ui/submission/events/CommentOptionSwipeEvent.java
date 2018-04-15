package me.saket.dank.ui.submission.events;

import android.content.res.Resources;
import android.graphics.Point;
import android.view.Gravity;
import android.view.View;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Comment;

import me.saket.dank.R;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.submission.CommentOptionsPopup;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class CommentOptionSwipeEvent implements SwipeEvent {

  public abstract Comment comment();

  public abstract SwipeableLayout itemView();

  public static CommentOptionSwipeEvent create(Comment comment, SwipeableLayout itemView) {
    return new AutoValue_CommentOptionSwipeEvent(comment, itemView);
  }

  public void showPopup(View toolbar) {
    SwipeableLayout commentLayout = itemView();
    Point sheetLocation = Views.locationOnScreen(commentLayout);
    Point popupLocation = new Point(0, sheetLocation.y);

    // Align with comment body.
    Resources resources = itemView().getResources();
    popupLocation.offset(
        resources.getDimensionPixelSize(R.dimen.submission_comment_horiz_padding),
        resources.getDimensionPixelSize(R.dimen.submission_comment_top_padding));

    // Keep below toolbar.
    int toolbarBottom = Views.locationOnScreen(toolbar).y + toolbar.getBottom() + resources.getDimensionPixelSize(R.dimen.spacing16);
    popupLocation.y = Math.max(popupLocation.y, toolbarBottom);

    CommentOptionsPopup optionsPopup = new CommentOptionsPopup(commentLayout.getContext(), comment());
    optionsPopup.showAtLocation(commentLayout, Gravity.TOP | Gravity.START, popupLocation);
  }
}
