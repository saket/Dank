package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.Nullable;

import net.dean.jraw.models.Message;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.compose.InsertGifDialog;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.submission.AuditedCommentSort.SelectedBy;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.urlparser.RedditCommentLink;
import me.saket.dank.urlparser.RedditSubmissionLink;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Optional;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;

/**
 * An Activity that can only show a submission, unlike {@link SubredditActivity} which can shows
 * subreddit submissions as well as holds the submission fragment.
 * <p>
 * This Activity exists because we want to show a submission directly when a comment URL is clicked,
 * where pull-to-collapse will take the user back to the previous screen. This is unlike
 * {@link SubredditActivity}, which takes user back to the submission's subreddit.
 */
public class SubmissionPageLayoutActivity extends DankPullCollapsibleActivity
    implements SubmissionPageLayout.Callbacks, InsertGifDialog.OnGifInsertListener
{

  public static final String KEY_NEW_TAB = "createdInNewTab";
  private static final String KEY_SUBMISSION_REQUEST = "submission";
  private static final String KEY_MESSAGE_TO_MARK_AS_READ = "messageToMarkAsRead";

  @BindView(R.id.independentsubmission_root) IndependentExpandablePageLayout contentPage;
  @BindView(R.id.independentsubmission_submission_page) SubmissionPageLayout submissionPageLayout;

  /**
   * @param expandFromShape The initial shape from where this Activity will begin its entry expand animation.
   */
  public static Intent intent(
      Context context,
      DankSubmissionRequest submissionRequest,
      @Nullable Rect expandFromShape,
      @Nullable Message messageToMarkAsRead)
  {
    Intent intent = new Intent(context, SubmissionPageLayoutActivity.class);

    if (messageToMarkAsRead != null) {
      intent.putExtra(KEY_MESSAGE_TO_MARK_AS_READ, messageToMarkAsRead);
    }

    intent.putExtra(KEY_SUBMISSION_REQUEST, submissionRequest);
    intent.putExtra(KEY_EXPAND_FROM_SHAPE, expandFromShape);
    return intent;
  }

  public static Intent intent(
      Context context,
      RedditSubmissionLink submissionLink,
      @Nullable Rect expandFromShape,
      @Nullable Message messageToMarkAsRead)
  {
    return intent(context, defaultRequest(submissionLink), expandFromShape, messageToMarkAsRead);
  }

  public static Intent intent(Context context, DankSubmissionRequest submissionRequest, @Nullable Rect expandFromShape) {
    return intent(context, submissionRequest, expandFromShape, null);
  }

  public static Intent intent(Context context, RedditSubmissionLink submissionLink, @Nullable Rect expandFromShape) {
    return intent(context, defaultRequest(submissionLink), expandFromShape);
  }

  public static void start(Context context, DankSubmissionRequest submissionRequest, @Nullable Rect expandFromShape) {
    context.startActivity(intent(context, submissionRequest, expandFromShape));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    boolean openedInNewTab = getIntent().getBooleanExtra(KEY_NEW_TAB, false);
    setEntryAnimationEnabled(!openedInNewTab);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_submission_fragment);
    ButterKnife.bind(this);

    setupContentExpandablePage(contentPage);
    submissionPageLayout.expandImmediately();
    expandFrom(getIntent().getParcelableExtra(KEY_EXPAND_FROM_SHAPE));
    contentPage.setPullToCollapseIntercepter(submissionPageLayout);

    // SubmissionPageLayout will handle retaining its data on its own when savedInstance != null.
    if (savedInstanceState == null) {
      DankSubmissionRequest submissionRequest;
      if (getIntent().hasExtra(KEY_SUBMISSION_REQUEST)) {
        submissionRequest = getIntent().getParcelableExtra(KEY_SUBMISSION_REQUEST);
      } else {
        throw new AssertionError();
      }
      submissionPageLayout.populateUi(Optional.empty(), submissionRequest, Optional.empty());
    }

    if (getIntent().hasExtra(KEY_MESSAGE_TO_MARK_AS_READ)) {
      Message messageToMarkAsRead = (Message) getIntent().getSerializableExtra(KEY_MESSAGE_TO_MARK_AS_READ);
      submissionPageLayout.handleMessageToMarkAsRead(messageToMarkAsRead);
    }
  }

  @Override
  public void finish() {
    super.finish();

    if (getIntent().getBooleanExtra(KEY_NEW_TAB, false)) {
      Observable.timer(IndependentExpandablePageLayout.ANIMATION_DURATION_MILLIS, TimeUnit.MILLISECONDS, mainThread())
          .takeUntil(lifecycle().onDestroy())
          .subscribe(o -> startActivity(SubredditActivity.intent(this)));
    }
  }

  private static DankSubmissionRequest defaultRequest(RedditSubmissionLink submissionLink) {
    // We don't know the suggested sort yet. Attempt with the default
    // sort and if it's found to be different, then do another load.
    DankSubmissionRequest.Builder submissionReqBuilder = DankSubmissionRequest
        .builder(submissionLink.id())
        .commentSort(Reddit.Companion.getDEFAULT_COMMENT_SORT(), SelectedBy.DEFAULT);

    RedditCommentLink initialComment = submissionLink.initialComment();
    if (initialComment != null) {
      submissionReqBuilder
          .focusCommentId(initialComment.id())
          .contextCount(initialComment.contextCount());
    }

    return submissionReqBuilder.build();
  }

//  @Override
//  public SubmissionPageAnimationOptimizer submissionPageAnimationOptimizer() {
//    // Should never get used.
//    //noinspection ConstantConditions
//    return new SubmissionPageAnimationOptimizer(null) {
//      @Override
//      public boolean isOptimizationPending() {
//        throw new AssertionError();
//      }
//    };
//  }

  @Override
  public void onClickSubmissionToolbarUp() {
    finish();
  }

  @Override
  public void onGifInsert(String title, GiphyGif gif, @Nullable Parcelable payload) {
    assert payload != null;
    submissionPageLayout.onGifInsert(title, gif, payload);
  }
}
