package me.saket.dank.ui.subreddit.events;

import android.content.Context;
import android.content.Intent;

import com.google.auto.value.AutoValue;

import net.dean.jraw.models.Submission;

import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.submission.SubmissionPageLayoutActivity;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.widgets.swipe.SwipeableLayout;

@AutoValue
public abstract class SubmissionOpenInNewTabSwipeEvent implements SwipeEvent {

  public abstract Submission submission();

  public abstract SwipeableLayout itemView();

  public static SubmissionOpenInNewTabSwipeEvent create(Submission submission, SwipeableLayout itemView) {
    return new AutoValue_SubmissionOpenInNewTabSwipeEvent(submission, itemView);
  }

  public void openInNewTab(UrlRouter urlRouter, UrlParser urlParser) {
    RedditSubmissionLink submissionLink = (RedditSubmissionLink) urlParser.parse(submission().getPermalink());
    Context context = itemView().getContext();
    Intent newTabIntent = urlRouter.forLink(submissionLink)
        .intent(context)
        .putExtra(SubmissionPageLayoutActivity.KEY_NEW_TAB, true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
    context.startActivity(newTabIntent);
  }
}
