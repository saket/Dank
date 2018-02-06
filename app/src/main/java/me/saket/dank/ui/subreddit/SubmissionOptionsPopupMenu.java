package me.saket.dank.ui.subreddit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.support.annotation.AnimRes;
import android.support.v4.content.ContextCompat;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.jakewharton.rxbinding2.view.RxView;

import net.dean.jraw.models.Submission;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.links.Link;
import me.saket.dank.data.links.RedditSubredditLink;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.UrlRouter;
import me.saket.dank.ui.user.PopupWindowWithMaterialTransition;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Clipboards;
import me.saket.dank.utils.Intents;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxPopupWindow;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.UrlParser;
import timber.log.Timber;

// TODO: Inflate layout programmatically?
public class SubmissionOptionsPopupMenu extends PopupWindowWithMaterialTransition {

  @BindView(R.id.submissionoptions_viewflipper) ViewFlipper contentViewFlipper;
  @BindView(R.id.submissionoptions_title) TextView titleView;
  @BindView(R.id.submissionoptions_author) TextView authorButton;
  @BindView(R.id.submissionoptions_subreddit) TextView subredditButton;
  @BindView(R.id.submissionoptions_share) TextView shareButton;
  @BindView(R.id.submissionoptions_copy) TextView copyButton;

  @BindView(R.id.submissionoptions_sharecopy_submenu) ViewGroup shareOrCopySubMenu;
  @BindView(R.id.submissionoptions_sharecopy_title) TextView shareOrCopySubMenuTitleView;
  @BindView(R.id.submissionoptions_sharecopy_reddit_comments) Button shareOrCopyRedditCommentsLinkButton;
  @BindView(R.id.submissionoptions_sharecopy_content) Button shareOrCopyContentLinkButton;

  @Inject Lazy<UrlRouter> urlRouter;

  enum ShareOrCopySubMenuMode {
    SHARE {
      @Override
      public void setupSubMenuUi(Context c, SubmissionOptionsPopupMenu menu) {
        menu.shareOrCopySubMenuTitleView.setText(R.string.submission_option_share);
        menu.shareOrCopyRedditCommentsLinkButton.setContentDescription(c.getString(R.string.cd_submission_option_share_reddit_link));
        menu.shareOrCopyContentLinkButton.setContentDescription(c.getString(R.string.cd_submission_option_share_content_link));
      }

      @Override
      public void executeAction(Context c, Link link) {
        c.startActivity(Intents.createForSharingUrl(null, link.unparsedUrl()));
      }
    },

    COPY {
      @Override
      public void setupSubMenuUi(Context c, SubmissionOptionsPopupMenu menu) {
        menu.shareOrCopySubMenuTitleView.setText(R.string.submission_option_copy);
        menu.shareOrCopyRedditCommentsLinkButton.setContentDescription(c.getString(R.string.cd_submission_option_copy_reddit_link));
        menu.shareOrCopyContentLinkButton.setContentDescription(c.getString(R.string.cd_submission_option_copy_content_link));
      }

      @Override
      public void executeAction(Context c, Link link) {
        Clipboards.save(c, link.unparsedUrl());
        Toast.makeText(c, R.string.copy_to_clipboard_confirmation, Toast.LENGTH_SHORT).show();
      }
    };

    public void setupSubMenuUi(Context c, SubmissionOptionsPopupMenu menu) {
      throw new UnsupportedOperationException();
    }

    public void executeAction(Context c, Link link) {
      throw new UnsupportedOperationException();
    }
  }

  @SuppressLint("InflateParams")
  public SubmissionOptionsPopupMenu(Context c, Submission submission, boolean showVisitSubredditOption) {
    super(c);
    final long startTime = System.currentTimeMillis();

    setContentView(R.layout.popup_submission_options);

    Timber.i("Inflated in: %sms", System.currentTimeMillis() - startTime);
    ButterKnife.bind(this, getContentView());
    Dank.dependencyInjector().inject(this);

    setupMenu(c, submission, showVisitSubredditOption);
  }

  private void setupMenu(Context c, Submission submission, boolean showVisitSubredditOption) {
    String redditCommentsPermalink = "https://reddit.com" + submission.getPermalink();

    titleView.setText(submission.getTitle());
    authorButton.setText(c.getString(R.string.user_name_u_prefix, submission.getAuthor()));
    subredditButton.setText(c.getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));
    shareOrCopyRedditCommentsLinkButton.setText(
        new Truss()
            .append(c.getString(R.string.submission_option_reddit_link))
            .append("\n")
            .pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.submission_option_button_byline)))
            .append(stripSchemeAndWww(redditCommentsPermalink))
            .popSpan()
            .build()
    );
    shareOrCopyContentLinkButton.setText(
        new Truss()
            .append(c.getString(R.string.submission_option_content_link))
            .append("\n")
            .pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.submission_option_button_byline)))
            .append(stripSchemeAndWww(submission.getUrl()))
            .popSpan()
            .build()
    );

    subredditButton.setVisibility(showVisitSubredditOption ? View.VISIBLE : View.GONE);

    authorButton.setOnClickListener(o -> {
      Toast.makeText(c, R.string.work_in_progress, Toast.LENGTH_SHORT).show();
      dismiss();
    });
    subredditButton.setOnClickListener(o -> {
      String subredditName = submission.getSubredditName();
      urlRouter.get()
          .forLink(RedditSubredditLink.create("https://reddit.com/r/" + subredditName, subredditName))
          .expandFromBelowToolbar()
          .open(c);
      dismiss();
    });

    if (submission.isSelfPost()) {
      Link parsedPermalink = UrlParser.parse(redditCommentsPermalink);
      shareButton.setOnClickListener(o -> {
        ShareOrCopySubMenuMode.SHARE.executeAction(c, parsedPermalink);
      });
      copyButton.setOnClickListener(o -> {
        ShareOrCopySubMenuMode.COPY.executeAction(c, parsedPermalink);
        dismiss();
      });

    } else {
      setupSubMenu(c, submission, redditCommentsPermalink);
    }
  }

  private void setupSubMenu(Context c, Submission submission, String redditCommentsPermalink) {
    Observable<Object> popupDismiss = RxPopupWindow.dismisses(this).take(1);
    Observable<ShareOrCopySubMenuMode> subMenuModeChanges = RxView.clicks(shareButton)
        .map(o -> ShareOrCopySubMenuMode.SHARE)
        .mergeWith(RxView.clicks(copyButton).map(o -> ShareOrCopySubMenuMode.COPY))
        .share();

    subMenuModeChanges
        .takeUntil(popupDismiss)
        .subscribe(mode -> mode.setupSubMenuUi(c, this));

    RxView.clicks(shareOrCopySubMenuTitleView)
        .takeUntil(popupDismiss)
        .subscribe(o -> {
          contentViewFlipper.setInAnimation(animationWithInterpolator(c, R.anim.submission_options_viewflipper_mainmenu_enter));
          contentViewFlipper.setOutAnimation(animationWithInterpolator(c, R.anim.submission_options_viewflipper_submenu_exit));
          contentViewFlipper.setDisplayedChild(0);
        });

    subMenuModeChanges
        .takeUntil(popupDismiss)
        .subscribe(o -> {
          contentViewFlipper.setInAnimation(animationWithInterpolator(c, R.anim.submission_options_viewflipper_submenu_enter));
          contentViewFlipper.setOutAnimation(animationWithInterpolator(c, R.anim.submission_options_viewflipper_mainmenu_exit));
          contentViewFlipper.setDisplayedChild(contentViewFlipper.indexOfChild(shareOrCopySubMenu));
        });

    Observable
        .merge(
            RxView.clicks(shareOrCopyRedditCommentsLinkButton).map(o -> redditCommentsPermalink),
            RxView.clicks(shareOrCopyContentLinkButton).map(o -> submission.getUrl()))
        .map(url -> UrlParser.parse(url))
        .withLatestFrom(subMenuModeChanges, Pair::create)
        .takeUntil(popupDismiss)
        .subscribe(pair -> {
          Link link = pair.first();
          ShareOrCopySubMenuMode mode = pair.second();
          mode.executeAction(c, link);
          dismiss();
        });
  }

  private Animation animationWithInterpolator(Context c, @AnimRes int animRes) {
    Animation animation = AnimationUtils.loadAnimation(c, animRes);
    animation.setInterpolator(Animations.INTERPOLATOR);
    return animation;
  }

  private static String stripSchemeAndWww(String url) {
    try {
      Uri URI = Uri.parse(url);
      String schemeStripped = url.substring(URI.getScheme().length() + "://" .length());
      if (schemeStripped.startsWith("www.")) {
        return schemeStripped.substring("www." .length());
      } else {
        return schemeStripped;
      }
    } catch (Exception e) {
      return url;
    }
  }

  /**
   * Calculation copied from {@link PopupWindow}.
   */
  @Override
  protected Rect calculateTransitionEpicenter(View anchor, ViewGroup popupDecorView, Point showLocation) {
    int[] anchorLocation = new int[2];
    int[] popupLocation = new int[2];
    anchor.getLocationOnScreen(anchorLocation);
    popupDecorView.getLocationOnScreen(popupLocation);

    // Compute the position of the anchor relative to the popup.
    final Rect bounds = new Rect(0, 0, 0, anchor.getHeight());
    bounds.offset(anchorLocation[0] - popupLocation[0], anchorLocation[1] - popupLocation[1]);
    return bounds;
  }
}
