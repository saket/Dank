package me.saket.dank.ui.submission.adapter;

import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Arrays2.immutable;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.style.ForegroundColorSpan;

import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.submission.BookmarksRepository;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.submission.SubmissionCommentTreeUiConstructor;
import me.saket.dank.ui.submission.SubmissionContentLoadError;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.urlparser.Link;
import me.saket.dank.utils.CombineLatestWithLog;
import me.saket.dank.utils.CombineLatestWithLog.O;
import me.saket.dank.utils.CommentSortUtils;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Themes;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.vote.VotingManager;
import me.saket.dank.widgets.span.RoundedBackgroundSpan;
import timber.log.Timber;

public class SubmissionUiConstructor {

  private static final Object NOTHING = LifecycleStreams.NOTHING;

  private final SubmissionContentLinkUiConstructor contentLinkUiModelConstructor;
  private final ReplyRepository replyRepository;
  private final VotingManager votingManager;
  private final Markdown markdown;
  private final UserSessionRepository userSessionRepository;
  private final Lazy<BookmarksRepository> bookmarksRepository;

  @Inject
  public SubmissionUiConstructor(
      SubmissionContentLinkUiConstructor contentLinkUiModelConstructor,
      ReplyRepository replyRepository,
      VotingManager votingManager,
      Markdown markdown,
      UserSessionRepository userSessionRepository,
      Lazy<BookmarksRepository> bookmarksRepository)
  {
    this.contentLinkUiModelConstructor = contentLinkUiModelConstructor;
    this.replyRepository = replyRepository;
    this.votingManager = votingManager;
    this.markdown = markdown;
    this.userSessionRepository = userSessionRepository;
    this.bookmarksRepository = bookmarksRepository;
  }

  /**
   * @param optionalSubmissions Can emit twice. Once w/o comments and once with comments.
   */
  @CheckResult
  public Observable<List<SubmissionScreenUiModel>> stream(
      Context context,
      SubmissionCommentTreeUiConstructor submissionCommentTreeUiConstructor,
      Observable<Optional<Submission>> optionalSubmissions,
      Observable<DankSubmissionRequest> submissionRequests,
      Observable<Optional<Link>> contentLinks,
      Observable<Optional<SubmissionContentLoadError>> mediaContentLoadErrors,
      Observable<Optional<ResolvedError>> commentsLoadErrors)
  {
    return optionalSubmissions
        .distinctUntilChanged() // Submission#equals() only compares IDs.
        .switchMap(optional -> {
          if (!optional.isPresent()) {
            return commentsLoadErrors
                .map(optionalError -> optionalError.isPresent()
                    ? Collections.singletonList(SubmissionCommentsLoadError.UiModel.create(optionalError.get()))
                    : Collections.emptyList());
          }

          Observable<Submission> submissions = optionalSubmissions
              // Not sure why, but the parent switchMap() on submission change gets triggered
              // after this chain receives an empty submission, so adding this extra takeWhile().
              .takeWhile(optionalSub -> optionalSub.isPresent())
              .map(submissionOptional -> submissionOptional.get())
              .startWith(optional.get());

          Observable<Optional<SubmissionContentLinkUiModel>> contentLinkUiModels = Observable
              .combineLatest(contentLinks, submissions.observeOn(io()), Pair::create)
              .distinctUntilChanged()
              .doOnDispose(() -> contentLinkUiModelConstructor.clearGlideTargets(context))
              .switchMap(pair -> {
                Optional<Link> contentLink = pair.first();
                if (!contentLink.isPresent()) {
                  return Observable.just(Optional.empty());
                }
                Submission submission = pair.second();
                return contentLinkUiModelConstructor
                    .streamLoad(context, contentLink.get(), ImageWithMultipleVariants.of(submission.getThumbnails()))
                    .doOnError(e -> Timber.e(e, "Error while creating content link ui model"))
                    .map(Optional::of);
              });

          Observable<Integer> submissionPendingSyncReplyCounts = submissions
              .observeOn(io())
              .take(1)  // replace flatMap -> switchMap if we expect more than 1 emissions.
              .flatMap(submission -> replyRepository.streamPendingSyncReplies(ParentThread.of(submission)))
              .map(pendingSyncReplies -> pendingSyncReplies.size())
              .startWith(0);  // Stream sometimes takes too long to emit anything.

          Observable<Object> externalChanges = Observable
              .merge(votingManager.streamChanges(), bookmarksRepository.get().streamChanges())
              .observeOn(io())
              .startWith(NOTHING);

          Observable<SubmissionCommentsHeader.UiModel> headerUiModels = CombineLatestWithLog.from(
              O.of("ext-change", externalChanges.map(o -> context)),
              O.of("submission 2", submissions.observeOn(io())),
              O.of("content-link", contentLinkUiModels),
              this::headerUiModel
          );

          Observable<SubmissionCommentOptions.UiModel> commentOptionsUiModels = CombineLatestWithLog.from(
              O.of("submission 3", submissions),
              O.of("submission requests", submissionRequests),
              O.of("pending-sync-reply-count", submissionPendingSyncReplyCounts),
              this::commentOptionsUiModel
          );

          Observable<Optional<SubmissionMediaContentLoadError.UiModel>> contentLoadErrorUiModels = mediaContentLoadErrors
              .map(optionalError -> optionalError.map(error -> error.uiModel(context)));

          Observable<Optional<SubmissionCommentsViewFullThread.UiModel>> viewFullThreadUiModels = submissionRequests
              .map(request -> request.focusCommentId() == null
                  ? Optional.<SubmissionCommentsViewFullThread.UiModel>empty()
                  : Optional.of(SubmissionCommentsViewFullThread.UiModel.create(request)))
              .startWith(Optional.empty());  // submissionRequests stream sometimes takes too long to emit anything.

          Observable<List<SubmissionScreenUiModel>> commentRowUiModels = submissionCommentTreeUiConstructor
              .stream(
                  context,
                  submissions.observeOn(io()),
                  submissionRequests,
                  io())
              .startWith(Collections.<SubmissionScreenUiModel>emptyList());

          Observable<Optional<SubmissionCommentsLoadProgress.UiModel>> commentsLoadProgressUiModels = Observable
              .combineLatest(submissions.observeOn(io()), commentsLoadErrors, Pair::create)
              .map(pair -> {
                CommentNode comments = pair.first().getComments();
                boolean commentsLoadingFailed = pair.second().isPresent();
                return comments == null && !commentsLoadingFailed
                    ? Optional.of(SubmissionCommentsLoadProgress.UiModel.create())
                    : Optional.<SubmissionCommentsLoadProgress.UiModel>empty();
              })
              .startWith(Optional.of(SubmissionCommentsLoadProgress.UiModel.create()));

          Observable<Optional<SubmissionCommentsLoadError.UiModel>> commentsLoadErrorUiModels = commentsLoadErrors
              .map(optionalError -> optionalError.map(error -> SubmissionCommentsLoadError.UiModel.create(error)));

          return CombineLatestWithLog.from(
              O.of("header", headerUiModels),
              O.of("comment options", commentOptionsUiModels),
              O.of("content-load-error", contentLoadErrorUiModels),
              O.of("view-full-thread", viewFullThreadUiModels),
              O.of("comments-load-progress", commentsLoadProgressUiModels),
              O.of("comments-load-error", commentsLoadErrorUiModels),
              O.of("comment-rows", commentRowUiModels),
              (header, commentOptions, optionalContentError, viewFullThread, optionalCommentsLoadProgress, optionalCommentsLoadError, commentModels) -> {
                // Steps to update this list:
                // 1. Update the initial capacity.
                // 2. Ensure that the ordering is correct. This is
                //    the same order in which they'll be displayed.
                List<SubmissionScreenUiModel> allItems = new ArrayList<>(6 + commentModels.size());

                allItems.add(header);
                optionalContentError.ifPresent(allItems::add);
                allItems.add(commentOptions);
                viewFullThread.ifPresent(allItems::add);
                allItems.addAll(commentModels);

                // Comments progress and error go after comment rows
                // so that inline reply for submission appears above them.
                optionalCommentsLoadProgress.ifPresent(allItems::add);
                optionalCommentsLoadError.ifPresent(allItems::add);
                return allItems;
              })
              .as(immutable());
        });
  }

  /**
   * Header contains submission details, content link and self-text post.
   */
  private SubmissionCommentsHeader.UiModel headerUiModel(
      Context context,
      Submission submission,
      Optional<SubmissionContentLinkUiModel> contentLinkUiModel)
  {
    VoteDirection pendingOrDefaultVote = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
    int voteDirectionColor = Themes.voteColor(pendingOrDefaultVote);
    long adapterId = submission.getFullName().hashCode();

    Optional<CharSequence> selfTextOptional = submission.isSelfPost() && !submission.getSelftext().isEmpty()
        ? Optional.of(markdown.parseSelfText(submission))
        : Optional.empty();

    int vote = votingManager.getScoreAfterAdjustingPendingVote(submission);

    Truss titleBuilder = new Truss();

    if (submission.isArchived() || submission.isLocked()) {
      String tag = submission.isArchived()
          ? context.getString(R.string.submission_header_archived)
          : context.getString(R.string.submission_header_locked);

      titleBuilder
          .pushSpan(new RoundedBackgroundSpan(
              color(context, R.color.yellow_600),
              color(context, R.color.black_opacity_75),
              context.getResources().getDimensionPixelSize(R.dimen.spacing2),
              context.getResources().getDimensionPixelSize(R.dimen.textsize12),
              context.getResources().getDimensionPixelSize(R.dimen.spacing16),
              context.getResources().getDimensionPixelSize(R.dimen.spacing8)
          ))
          .append(tag.toUpperCase())
          .popSpan()
          .append("\n")
          .append("\n");
    }

    titleBuilder = titleBuilder.pushSpan(new ForegroundColorSpan(color(context, voteDirectionColor)))
        .append(Strings.abbreviateScore(vote))
        .popSpan()
        .append("  ")
        .append(Html.fromHtml(submission.getTitle()));

    String byline = context.getString(
        R.string.submission_byline,
        submission.getSubredditName(),
        submission.getAuthor(),
        Dates.createTimestamp(context.getResources(), JrawUtils.createdTimeUtc(submission)));

    return SubmissionCommentsHeader.UiModel.builder()
        .adapterId(adapterId)
        .title(titleBuilder.build(), Pair.create(vote, pendingOrDefaultVote))
        .byline(byline)
        .selfText(selfTextOptional)
        .optionalContentLinkModel(contentLinkUiModel)
        .submission(submission)
        .isSaved(bookmarksRepository.get().isSaved(submission))
        .build();
  }

  private SubmissionCommentOptions.UiModel commentOptionsUiModel(Submission submission, DankSubmissionRequest request, int pendingSyncReplyCount) {
    int postedAndPendingCommentCount = submission.getCommentCount() + pendingSyncReplyCount;
    String abbreviatedCommentCount = Strings.abbreviateScore(postedAndPendingCommentCount);
    int sortTextRes = CommentSortUtils.sortingDisplayTextRes(request.commentSort().mode());
    return SubmissionCommentOptions.UiModel.create(abbreviatedCommentCount, sortTextRes);
  }

  @ColorInt
  private static int color(Context context, @ColorRes int colorRes) {
    return ContextCompat.getColor(context, colorRes);
  }
}
