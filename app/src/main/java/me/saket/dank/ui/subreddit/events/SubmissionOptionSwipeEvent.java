package me.saket.dank.ui.subreddit.events;

import android.content.res.Resources;
import android.graphics.Point;
import android.view.Gravity;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.R;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.subreddit.SubmissionOptionsPopup;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ScrollingRecyclerViewSheet;
import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class SubmissionOptionSwipeEvent implements SwipeEvent {

  public abstract Submission submission();

  public abstract SwipeableLayout itemView();

  public static SubmissionOptionSwipeEvent create(Submission submission, SwipeableLayout itemView) {
    return new AutoValue_SubmissionOptionSwipeEvent(submission, itemView);
  }

  public void showPopupForSubredditScreen(String callingSubreddit) {
    Resources resources = itemView().getContext().getResources();
    Point showLocation = new Point(0, itemView().getTop() + Views.statusBarHeight(resources));

    // Align with submission body.
    int padding = resources.getDimensionPixelSize(R.dimen.subreddit_submission_padding);
    showLocation.offset(padding, padding);

    showPopup(Optional.of(callingSubreddit), showLocation);
  }

  public void showPopupForSubmissionScreen(Optional<String> callingSubreddit, ScrollingRecyclerViewSheet commentListParentSheet) {
    Point sheetLocation = Views.locationOnScreen(commentListParentSheet);
    Point menuLocation = new Point(0, sheetLocation.y);

    // Align with submission title.
    int headerPadding = itemView().getResources().getDimensionPixelSize(R.dimen.subreddit_submission_padding);
    menuLocation.offset(headerPadding, headerPadding);

    showPopup(callingSubreddit, menuLocation);
  }

  private void showPopup(Optional<String> optionalCallingSubreddit, Point showLocation) {
    boolean showVisitSubredditOption = optionalCallingSubreddit
        .map(name -> !submission().getSubreddit().equals(name))
        .orElse(true);

    SubmissionOptionsPopup optionsMenu = SubmissionOptionsPopup.builder(itemView().getContext(), submission())
        .showVisitSubreddit(showVisitSubredditOption)
        .build();
    optionsMenu.showAtLocation(itemView(), Gravity.NO_GRAVITY, showLocation);
  }
}
