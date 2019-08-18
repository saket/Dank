package me.saket.dank.ui.submission.events;

import android.graphics.Point;
import android.view.Gravity;
import android.view.View;

import com.google.auto.value.AutoValue;
import net.dean.jraw.models.Submission;

import java.util.List;

import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.submission.LinkOptionsPopup;
import me.saket.dank.ui.subreddit.SubmissionOptionsPopup;
import me.saket.dank.urlparser.Link;
import me.saket.dank.urlparser.MediaLink;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.urlparser.RedditUserLink;
import me.saket.dank.utils.NestedOptionsPopupMenu;
import me.saket.dank.utils.Views;

@AutoValue
public abstract class SubmissionContentLinkClickEvent {

  public abstract View contentLinkView();

  public abstract Link link();

  public static SubmissionContentLinkClickEvent create(View contentLinkView, Link link) {
    return new AutoValue_SubmissionContentLinkClickEvent(contentLinkView, link);
  }

  public void openContent(Submission submission, UrlRouter urlRouter) {
    if (link() instanceof RedditUserLink) {
      // TODO: Open fullscreen user profile.
      Point expandFromPoint = new Point(contentLinkView().getLeft(), contentLinkView().getBottom());
      urlRouter.forLink(((RedditUserLink) link()))
          .expandFrom(expandFromPoint)
          .open(contentLinkView());

    } else if (link() instanceof MediaLink) {
      urlRouter.forLink(((MediaLink) link()))
          .withRedditSuppliedImages(submission.getPreview())
          .open(contentLinkView().getContext());

    } else {
      urlRouter.forLink(link())
          .expandFromBelowToolbar()
          .open(contentLinkView().getContext());
    }
  }

  public void showOptionsPopup(Submission submission) {
    NestedOptionsPopupMenu popup;
    if (link() instanceof RedditSubmissionLink) {
        List<Submission> crosspostParents = submission.getCrosspostParents();
        if (crosspostParents != null && !crosspostParents.isEmpty()) {
          popup = SubmissionOptionsPopup.builder(contentLinkView().getContext(), crosspostParents.get(0))
              .showVisitSubreddit(true)
              .build();

        } else {
          popup = new LinkOptionsPopup(contentLinkView().getContext(), link());
        }

    } else {
      popup = new LinkOptionsPopup(contentLinkView().getContext(), link());
    }
    Point linkViewLocation = Views.locationOnScreen(contentLinkView());
    popup.showAtLocation(contentLinkView(), Gravity.TOP | Gravity.START, linkViewLocation);
  }
}
