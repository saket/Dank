package me.saket.dank.ui.subreddit;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.Gravity;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.R;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class SubmissionOptionSwipeEvent {

  public abstract Submission submission();

  public abstract SwipeableLayout itemView();

  public static SubmissionOptionSwipeEvent create(Submission submission, SwipeableLayout itemView) {
    return new AutoValue_SubmissionOptionSwipeEvent(submission, itemView);
  }

  public void showPopup(Context context, String subredditName) {
    Resources resources = context.getResources();
    Point showLocation = new Point(0, itemView().getTop() + Views.statusBarHeight(resources));

    // Align with submission body.
    int padding = resources.getDimensionPixelSize(R.dimen.subreddit_submission_start_padding);
    showLocation.offset(padding, padding);

    boolean showVisitSubredditOption = !subredditName.equals(submission().getSubredditName());

    SubmissionOptionsPopup optionsMenu = SubmissionOptionsPopup.builder(context, submission())
        .showVisitSubreddit(showVisitSubredditOption)
        .build();
    optionsMenu.showAtLocation(itemView(), Gravity.NO_GRAVITY, showLocation);
  }
}
