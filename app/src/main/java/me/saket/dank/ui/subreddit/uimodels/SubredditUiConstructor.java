package me.saket.dank.ui.subreddit.uimodels;

import android.content.Context;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;

import androidx.annotation.CheckResult;
import androidx.core.content.ContextCompat;

import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.SubmissionPreview;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import me.saket.dank.R;
import me.saket.dank.data.EmptyState;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ErrorState;
import me.saket.dank.ui.preferences.gestures.submissions.SubmissionSwipeActionsRepository;
import me.saket.dank.ui.submission.BookmarksRepository;
import me.saket.dank.ui.submission.CachedSubmissionFolder;
import me.saket.dank.ui.submission.PrivateSubredditException;
import me.saket.dank.ui.submission.SubredditNotFoundException;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.subreddit.SubmissionPaginationResult;
import me.saket.dank.ui.subreddit.SubmissionThumbnailTypeMinusNsfw;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Themes;
import me.saket.dank.utils.Truss;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.walkthrough.SubmissionGesturesWalkthrough;
import me.saket.dank.widgets.swipe.SwipeActions;

public class SubredditUiConstructor {

  private static final BiFunction<Boolean, Boolean, Boolean> AND_FUNCTION = (b1, b2) -> b1 && b2;
  private static final BiFunction<Boolean, Boolean, Boolean> OR_FUNCTION = (b1, b2) -> b1 || b2;

  private final VotingManager votingManager;
  private final Lazy<SubmissionGesturesWalkthrough> gesturesWalkthrough;
  private final Preference<Boolean> showCommentCountInByline;
  private final Preference<Boolean> showNsfwContent;
  private final Preference<Boolean> showThumbnailsPref;
  private final Preference<Boolean> showSubmissionThumbnailsOnLeft;
  private final ErrorResolver errorResolver;
  private final Lazy<BookmarksRepository> bookmarksRepository;
  private final Lazy<SubmissionSwipeActionsRepository> swipeActionsRepository;

  @Inject
  public SubredditUiConstructor(
      VotingManager votingManager,
      ErrorResolver errorResolver,
      Lazy<BookmarksRepository> bookmarksRepository,
      Lazy<SubmissionGesturesWalkthrough> gesturesWalkthrough,
      @Named("comment_count_in_submission_list_byline") Preference<Boolean> showCommentCountInByline,
      @Named("show_nsfw_content") Preference<Boolean> showNsfwContent,
      @Named("show_submission_thumbnails") Preference<Boolean> showThumbnailsPref,
      @Named("show_submission_thumbnails_on_left") Preference<Boolean> showSubmissionThumbnailsOnLeft,
      Lazy<SubmissionSwipeActionsRepository> swipeActionsRepository
  ) {
    this.votingManager = votingManager;
    this.errorResolver = errorResolver;
    this.bookmarksRepository = bookmarksRepository;
    this.gesturesWalkthrough = gesturesWalkthrough;
    this.showCommentCountInByline = showCommentCountInByline;
    this.showNsfwContent = showNsfwContent;
    this.showThumbnailsPref = showThumbnailsPref;
    this.showSubmissionThumbnailsOnLeft = showSubmissionThumbnailsOnLeft;
    this.swipeActionsRepository = swipeActionsRepository;
  }

  @CheckResult
  public Observable<SubredditScreenUiModel> stream(
      Context context,
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<SubmissionPaginationResult> paginationResults,
      Observable<CachedSubmissionFolder> submissionFolderStream)
  {
    Observable<?> userPrefChanges = Observable
        .merge(
            showCommentCountInByline.asObservable(),
            showNsfwContent.asObservable(),
            showThumbnailsPref.asObservable(),
            showSubmissionThumbnailsOnLeft.asObservable()
        )
        .skip(1); // Skip initial values.

    Observable<Object> externalChanges = Observable.merge(
        userPrefChanges,
        votingManager.streamChanges(),
        bookmarksRepository.get().streamChanges());

    Observable<Boolean> sharedFullscreenProgressVisibilities = fullscreenProgressVisibilities(cachedSubmissionLists, paginationResults)
        .share();

    return Observable.combineLatest(Arrays.asList(
        sharedFullscreenProgressVisibilities.distinctUntilChanged(),
        fullscreenErrors(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        fullscreenEmptyStates(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        toolbarRefreshVisibilities(sharedFullscreenProgressVisibilities).distinctUntilChanged(),
        paginationProgressUiModels(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        gesturesWalkthrough.get().walkthroughRows(),
        cachedSubmissionLists,
        submissionFolderStream,
        swipeActionsRepository.get().getSwipeActions().distinctUntilChanged(),
        externalChanges
    ), (data) -> {
      Boolean fullscreenProgressVisible = (Boolean) data[0];
      Optional<ErrorState> optFullscreenError = (Optional<ErrorState>) data[1];
      Optional<EmptyState> optEmptyState = (Optional<EmptyState>) data[2];
      Boolean toolbarRefreshVisible = (Boolean) data[3];
      Optional<SubredditSubmissionPagination.UiModel> optPagination = (Optional<SubredditSubmissionPagination.UiModel>) data[4];
      Optional<SubmissionGesturesWalkthrough.UiModel> optWalkthroughRow = (Optional<SubmissionGesturesWalkthrough.UiModel>) data[5];
      Optional<List<Submission>> optCachedSubs = (Optional<List<Submission>>) data[6];
      CachedSubmissionFolder folder = (CachedSubmissionFolder) data[7];
      SwipeActions swipeActions = (SwipeActions) data[8];
      int rowCount = optPagination.map(p -> 1).orElse(0) + optCachedSubs.map(subs -> subs.size()).orElse(0);
      List<SubredditScreenUiModel.SubmissionRowUiModel> rowUiModels = new ArrayList<>(rowCount);

      optCachedSubs.ifPresent(cachedSubs -> {
        optWalkthroughRow.ifPresent(walkthroughUiModel -> {
          rowUiModels.add(walkthroughUiModel);
        });

        for (Submission submission : cachedSubs) {
          int pendingSyncReplyCount = 0;  // TODO v2:  Get this from database.
          rowUiModels.add(submissionUiModel(context, submission, pendingSyncReplyCount, folder.subredditName(), swipeActions));
        }
      });
      optPagination.ifPresent(pagination -> rowUiModels.add(pagination));

      return SubredditScreenUiModel.builder()
          .fullscreenProgressVisible(fullscreenProgressVisible)
          .fullscreenError(optFullscreenError)
          .emptyState(optEmptyState)
          .toolbarRefreshVisible(toolbarRefreshVisible)
          .rowUiModels(rowUiModels)
          .build();
    });
  }

  private Observable<Boolean> fullscreenProgressVisibilities(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<SubmissionPaginationResult> paginationResults)
  {
    Observable<Boolean> fullscreenProgressForAppLaunch = Observable.combineLatest(
        cachedSubmissionLists.map(Optional::isEmpty),
        paginationResults.map(SubmissionPaginationResult::isIdle),
        AND_FUNCTION
    );

    Observable<Boolean> fullscreenProgressForFolderChange = Observable.combineLatest(
        cachedSubmissionLists.map(Optional::isEmpty),
        paginationResults.map(SubmissionPaginationResult::isInFlight),
        AND_FUNCTION
    );

    return Observable.combineLatest(
        fullscreenProgressForAppLaunch,
        fullscreenProgressForFolderChange,
        OR_FUNCTION
    );
  }

  private Observable<Optional<ErrorState>> fullscreenErrors(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<SubmissionPaginationResult> paginationResults)
  {
    return Observable.combineLatest(
        cachedSubmissionLists.map(optionalSubs -> optionalSubs.isEmpty() || optionalSubs.get().isEmpty()),
        paginationResults,
        (areSubsEmpty, paginationResult) -> {
          if (areSubsEmpty && paginationResult.isFailed()) {
            Throwable error = paginationResult.error();
            if (error instanceof PrivateSubredditException) {
              return Optional.of(ErrorState.create(R.string.subreddit_error_emoji, R.string.subreddit_error_private));
            }
            if (error instanceof SubredditNotFoundException) {
              return Optional.of(ErrorState.create(R.string.subreddit_error_emoji, R.string.subreddit_error_not_found));
            }
            return Optional.of(ErrorState.from(errorResolver.resolve(error)));
          } else {
            return Optional.<ErrorState>empty();
          }
        }
    );
  }

  private Observable<Optional<EmptyState>> fullscreenEmptyStates(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<SubmissionPaginationResult> paginationResults)
  {
    return Observable.combineLatest(
        cachedSubmissionLists.map(optionalSubs -> optionalSubs.isPresent() && optionalSubs.get().isEmpty()),
        paginationResults,
        (areSubsEmpty, paginationResult) -> {
          if (areSubsEmpty && paginationResult.isIdle()) {
            return Optional.of(EmptyState.create(R.string.subreddit_emptystate_emoji, R.string.subreddit_emptystate_message));
          } else {
            return Optional.<EmptyState>empty();
          }
        }
    );
  }

  private Observable<Boolean> toolbarRefreshVisibilities(Observable<Boolean> fullscreenProgressVisibilities) {
    return fullscreenProgressVisibilities.map(fullscreenProgressVisible -> !fullscreenProgressVisible);
  }

  private Observable<Optional<SubredditSubmissionPagination.UiModel>> paginationProgressUiModels(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<SubmissionPaginationResult> paginationResults)
  {
    return Observable.combineLatest(
        cachedSubmissionLists.map(optionalSubs -> optionalSubs.isPresent() && !optionalSubs.get().isEmpty()),
        paginationResults,
        (cachedSubsPresent, paginationResult) -> {
          if (cachedSubsPresent) {
            switch (paginationResult.state()) {
              case IN_FLIGHT:
                return Optional.of(SubredditSubmissionPagination.UiModel.createProgress());

              case IDLE:
                return Optional.empty();

              case FAILED:
                return Optional.of(SubredditSubmissionPagination.UiModel.createError(R.string.subreddit_error_failed_to_load_more_submissions));

              default:
                throw new AssertionError("Unknown pagination state: " + paginationResult);
            }
          } else {
            return Optional.empty();
          }
        }
    );
  }

  public SubredditSubmission.UiModel submissionUiModel(
      Context c,
      Submission submission,
      Integer pendingSyncReplyCount,
      String subredditName,
      SwipeActions swipeActions
  ) {
    int submissionScore = votingManager.getScoreAfterAdjustingPendingVote(submission);
    VoteDirection voteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
    int postedAndPendingCommentCount = submission.getCommentCount() + pendingSyncReplyCount;

    Truss titleBuilder = new Truss();
    titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, Themes.voteColor(voteDirection))));
    titleBuilder.append(Strings.abbreviateScore(submissionScore));
    titleBuilder.popSpan();
    titleBuilder.append("  ");
    //noinspection deprecation
    titleBuilder.append(Html.fromHtml(submission.getTitle()));

    // Setting textAllCaps removes all spans, so I'm applying uppercase manually.
    Truss bylineBuilder = new Truss();
    if (!submission.getSubreddit().equals(subredditName)) {
      bylineBuilder.append(c.getString(R.string.subreddit_name_r_prefix, submission.getSubreddit()).toUpperCase(Locale.ENGLISH));
      bylineBuilder.append(" \u00b7 ");
    }
    bylineBuilder.append(submission.getAuthor().toUpperCase(Locale.ENGLISH));
    if (showCommentCountInByline.get()) {
      bylineBuilder.append(" \u00b7 ");
      bylineBuilder.append(c.getString(
          R.string.subreddit_submission_item_byline_comment_count,
          Strings.abbreviateScore(postedAndPendingCommentCount)).toUpperCase(Locale.ENGLISH));
    }
    if (submission.isNsfw()) {
      bylineBuilder.append(" \u00b7 ");
      bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.pink_A100)));
      bylineBuilder.append("NSFW");
      bylineBuilder.popSpan();
    }
    if (submission.isStickied()) {
      bylineBuilder.append(" \u00b7 ");
      bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.green_A100)));
      bylineBuilder.append("STICKY");
      bylineBuilder.popSpan();
    }

    SubmissionThumbnailTypeMinusNsfw thumbnailType = SubmissionThumbnailTypeMinusNsfw.Companion.parse(submission);
    Optional<SubredditSubmission.UiModel.Thumbnail> thumbnail;

    if (!showThumbnailsPref.get()) {
      thumbnail = Optional.empty();

    } else if (thumbnailType == SubmissionThumbnailTypeMinusNsfw.NONE) {
      thumbnail = Optional.empty();

    } else {
      if (submission.isNsfw() && !showNsfwContent.get()) {
        thumbnail = Optional.of(
            thumbnailForStaticImage(c)
                .staticRes(Optional.of(R.drawable.ic_visibility_off_24dp))
                .contentDescription(c.getString(R.string.cd_subreddit_submission_item_nsfw_post))
                .build());

      } else {
        switch (thumbnailType) {
          case SELF_POST:
            thumbnail = Optional.of(
                thumbnailForStaticImage(c)
                    .staticRes(Optional.of(R.drawable.ic_text_fields_24dp))
                    .contentDescription(c.getString(R.string.subreddit_submission_item_cd_self_text))
                    .build());
            break;

          case URL_STATIC_ICON:
            thumbnail = Optional.of(
                thumbnailForStaticImage(c)
                    .staticRes(Optional.of(R.drawable.ic_link_24dp))
                    .contentDescription(c.getString(R.string.subreddit_submission_item_cd_external_url))
                    .build());
            break;

          case URL_REMOTE_THUMBNAIL:
            thumbnail = Optional.of(thumbnailForRemoteImage(c, submission.getPreview()));
            break;

          //noinspection ConstantConditions
          case NONE:
            throw new AssertionError();

          case UNKNOWN:
            thumbnail = Optional.empty();
            break;

          default:
            throw new UnsupportedOperationException("Unknown submission thumbnail type: " + thumbnailType);
        }
      }
    }

    boolean isThumbnailClickable;
    switch (thumbnailType) {
      case SELF_POST:
        isThumbnailClickable = false;
        break;

      case URL_STATIC_ICON:
      case URL_REMOTE_THUMBNAIL:
        // Don't want to display NSFW content if it's disabled on thumbnail click.
        // Might get flagged by Play Store's automatic review thing.
        isThumbnailClickable = !submission.isSelfPost() && (!submission.isNsfw() || showNsfwContent.get());
        break;

      case UNKNOWN:
      case NONE:
        isThumbnailClickable = false;
        break;

      default:
        throw new UnsupportedOperationException("Unknown submission thumbnail type: " + thumbnailType);
    }

    // Just in case I forget again. This == row background, not thumbnail background.
    Optional<Integer> rowBackgroundResource = Optional.empty();

    if (submission.isStickied()) {
      rowBackgroundResource = Optional.of(R.drawable.background_subreddit_submission_sticky);
    } else if (submission.isNsfw()) {
      rowBackgroundResource = Optional.of(R.drawable.background_subreddit_submission_nsfw);
    }

    return SubredditSubmission.UiModel.builder()
        .submission(submission)
        .adapterId(JrawUtils2.generateAdapterId(submission))
        .thumbnail(thumbnail)
        .isThumbnailClickable(isThumbnailClickable)
        .displayThumbnailOnLeftSide(showSubmissionThumbnailsOnLeft.get())
        .title(titleBuilder.build(), Pair.create(submissionScore, voteDirection))
        .byline(bylineBuilder.build(), postedAndPendingCommentCount)
        .backgroundDrawableRes(rowBackgroundResource)
        .isSaved(bookmarksRepository.get().isSaved(submission))
        .swipeActions(swipeActions)
        .build();
  }

  private SubredditSubmission.UiModel.Thumbnail.Builder thumbnailForStaticImage(Context c) {
    //noinspection ConstantConditions
    return SubredditSubmission.UiModel.Thumbnail.builder()
        .remoteUrl(Optional.empty())
        .scaleType(ImageView.ScaleType.CENTER_INSIDE)
        .tintColor(Optional.of(ContextCompat.getColor(c, R.color.gray_100)))
        .backgroundRes(Optional.of(R.drawable.background_submission_self_thumbnail));
  }

  private SubredditSubmission.UiModel.Thumbnail thumbnailForRemoteImage(Context c, SubmissionPreview preview) {
    ImageWithMultipleVariants redditThumbnails = ImageWithMultipleVariants.Companion.of(preview);
    String optimizedThumbnailUrl = redditThumbnails.findNearestFor(c.getResources().getDimensionPixelSize(R.dimen.subreddit_submission_thumbnail));

    return SubredditSubmission.UiModel.Thumbnail.builder()
        .staticRes(Optional.empty())
        .remoteUrl(Optional.of(optimizedThumbnailUrl))
        .contentDescription(c.getString(R.string.subreddit_submission_item_cd_external_url))
        .scaleType(ImageView.ScaleType.CENTER_CROP)
        .tintColor(Optional.empty())
        .backgroundRes(Optional.empty())
        .build();
  }
}
