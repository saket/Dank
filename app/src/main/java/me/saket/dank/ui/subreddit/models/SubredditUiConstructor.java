package me.saket.dank.ui.subreddit.models;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;

import com.f2prateek.rx.preferences2.Preference;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.Thumbnails;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import me.saket.dank.R;
import me.saket.dank.data.EmptyState;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.submission.adapter.ImageWithMultipleVariants;
import me.saket.dank.ui.subreddit.NetworkCallStatus;
import me.saket.dank.ui.subreddit.RealSubmissionThumbnailType;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;

public class SubredditUiConstructor {

  private static final BiFunction<Boolean, Boolean, Boolean> AND_FUNCTION = (b1, b2) -> b1 && b2;
  private static final BiFunction<Boolean, Boolean, Boolean> OR_FUNCTION = (b1, b2) -> b1 || b2;

  private final VotingManager votingManager;
  private final ReplyRepository replyRepository;
  private final Preference<Boolean> showCommentCountInByline;
  private final Preference<Boolean> showNsfwContent;
  private final ErrorResolver errorResolver;

  @Inject
  public SubredditUiConstructor(
      VotingManager votingManager,
      ReplyRepository replyRepository,
      ErrorResolver errorResolver,
      @Named("comment_count_in_submission_list_byline") Preference<Boolean> showCommentCountInByline,
      @Named("show_nsfw_content") Preference<Boolean> showNsfwContent)
  {
    this.votingManager = votingManager;
    this.replyRepository = replyRepository;
    this.errorResolver = errorResolver;
    this.showCommentCountInByline = showCommentCountInByline;
    this.showNsfwContent = showNsfwContent;
  }

  @CheckResult
  public Observable<SubredditScreenUiModel> stream(
      Context context,
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<NetworkCallStatus> paginationResults)
  {
    Observable<?> userPrefChanges = Observable.merge(showCommentCountInByline.asObservable(), showNsfwContent.asObservable())
        .skip(1); // Skip initial values.

    Observable<Boolean> sharedFullscreenProgressVisibilities = fullscreenProgressVisibilities(cachedSubmissionLists, paginationResults).share();

    return Observable.combineLatest(
        sharedFullscreenProgressVisibilities.distinctUntilChanged(),
        fullscreenErrors(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        fullscreenEmptyStates(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        toolbarRefreshVisibilities(sharedFullscreenProgressVisibilities).distinctUntilChanged(),
        paginationProgressUiModels(cachedSubmissionLists, paginationResults).distinctUntilChanged(),
        cachedSubmissionLists,
        userPrefChanges,
        votingManager.streamChanges(),
        (fullscreenProgressVisible, optFullscreenError, optEmptyState, toolbarRefreshVisible, optPagination, optCachedSubs, o, oo) ->
        {
          int rowCount = optPagination.map(p -> 1).orElse(0) + optCachedSubs.map(subs -> subs.size()).orElse(0);
          List<SubredditScreenUiModel.SubmissionRowUiModel> rowUiModels = new ArrayList<>(rowCount);

          optCachedSubs.ifPresent(cachedSubs -> {
            for (Submission submission : cachedSubs) {
              int pendingSyncReplyCount = 0;  // TODO v2:  Get this from database.
              rowUiModels.add(submissionUiModel(context, submission, pendingSyncReplyCount));
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
      Observable<NetworkCallStatus> paginationResults)
  {
    Observable<Boolean> fullscreenProgressForAppLaunch = Observable.combineLatest(
        cachedSubmissionLists.map(Optional::isEmpty),
        paginationResults.map(NetworkCallStatus::isIdle),
        AND_FUNCTION
    );

    Observable<Boolean> fullscreenProgressForFolderChange = Observable.combineLatest(
        cachedSubmissionLists.map(Optional::isEmpty),
        paginationResults.map(NetworkCallStatus::isInFlight),
        AND_FUNCTION
    );

    return Observable.combineLatest(
        fullscreenProgressForAppLaunch,
        fullscreenProgressForFolderChange,
        OR_FUNCTION
    );
  }

  private Observable<Optional<ResolvedError>> fullscreenErrors(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<NetworkCallStatus> paginationResults)
  {
    return Observable.combineLatest(
        cachedSubmissionLists.map(optionalSubs -> optionalSubs.isEmpty() || optionalSubs.get().isEmpty()),
        paginationResults,
        (areSubsEmpty, paginationResult) -> {
          if (areSubsEmpty && paginationResult.isFailed()) {
            return Optional.of(errorResolver.resolve(paginationResult.error()));
          } else {
            return Optional.<ResolvedError>empty();
          }
        }
    );
  }

  private Observable<Optional<EmptyState>> fullscreenEmptyStates(
      Observable<Optional<List<Submission>>> cachedSubmissionLists,
      Observable<NetworkCallStatus> paginationResults)
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
      Observable<NetworkCallStatus> paginationResults)
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

  private SubredditSubmission.UiModel submissionUiModel(
      Context c,
      Submission submission,
      Integer pendingSyncReplyCount)
  {
    int submissionScore = votingManager.getScoreAfterAdjustingPendingVote(submission);
    VoteDirection voteDirection = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
    int postedAndPendingCommentCount = submission.getCommentCount() + pendingSyncReplyCount;

    Truss titleBuilder = new Truss();
    titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, Commons.voteColor(voteDirection))));
    titleBuilder.append(Strings.abbreviateScore(submissionScore));
    titleBuilder.popSpan();
    titleBuilder.append("  ");
    //noinspection deprecation
    titleBuilder.append(Html.fromHtml(submission.getTitle()));

    // Setting textAllCaps removes all spans, so I'm applying uppercase manually.
    Truss bylineBuilder = new Truss();
    bylineBuilder.append(c.getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()).toUpperCase(Locale.ENGLISH));
    bylineBuilder.append(" \u00b7 ");
    bylineBuilder.append(submission.getAuthor().toUpperCase(Locale.ENGLISH));
    if (showCommentCountInByline.get()) {
      bylineBuilder.append(" \u00b7 ");
      bylineBuilder.append(c.getString(
          R.string.subreddit_submission_item_byline_comment_count,
          Strings.abbreviateScore(postedAndPendingCommentCount)));
    }
    if (submission.isNsfw()) {
      bylineBuilder.append(" \u00b7 ");
      bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(c, R.color.pink_A100)));
      bylineBuilder.append("NSFW");
      bylineBuilder.popSpan();
    }

    Optional<Integer> backgroundResource = submission.isNsfw()
        ? Optional.of(R.drawable.background_subreddit_submission_nsfw)
        : Optional.empty();

    Optional<SubredditSubmission.UiModel.Thumbnail> thumbnail;
    RealSubmissionThumbnailType thumbnailType = RealSubmissionThumbnailType.parse(submission, showNsfwContent.get());
    switch (thumbnailType) {
      case NSFW_SELF_POST:
      case NSFW_LINK:
        thumbnail = Optional.of(
            thumbnailForStaticImage(c)
                .staticRes(Optional.of(R.drawable.ic_visibility_off_24dp))
                .contentDescription(c.getString(R.string.cd_subreddit_submission_item_nsfw_post))
                .build());
        break;

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
        thumbnail = Optional.of(thumbnailForRemoteImage(c, submission.getThumbnails()));
        break;

      case NONE:
        thumbnail = Optional.empty();
        break;

      default:
        throw new UnsupportedOperationException("Unknown submission thumbnail type: " + thumbnailType);
    }

    return SubredditSubmission.UiModel.builder()
        .submission(submission)
        .submissionInfo(PostedOrInFlightContribution.from(submission))
        .adapterId(submission.getFullName().hashCode())
        .thumbnail(thumbnail)
        .title(titleBuilder.build(), Pair.create(submissionScore, voteDirection))
        .byline(bylineBuilder.build(), postedAndPendingCommentCount)
        .backgroundDrawableRes(backgroundResource)
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

  private SubredditSubmission.UiModel.Thumbnail thumbnailForRemoteImage(Context c, Thumbnails submissionThumbnails) {
    ImageWithMultipleVariants redditThumbnails = ImageWithMultipleVariants.of(submissionThumbnails);
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
