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

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.VotingManager;
import me.saket.dank.data.links.Link;
import me.saket.dank.ui.submission.CommentInlineReplyItem;
import me.saket.dank.ui.submission.CommentPendingSyncReplyItem;
import me.saket.dank.ui.submission.CommentTreeConstructor;
import me.saket.dank.ui.submission.DankCommentNode;
import me.saket.dank.ui.submission.FocusedComment;
import me.saket.dank.ui.submission.LoadMoreCommentItem;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.submission.SubmissionCommentRow;
import me.saket.dank.ui.submission.SubmissionContentLoadError;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import timber.log.Timber;

// TODO: Build a subcomponent for SubredditActivity?
public class SubmissionUiConstructor {

  private final SubmissionContentLinkUiConstructor contentLinkUiModelConstructor;
  private final ReplyRepository replyRepository;
  private final VotingManager votingManager;
  private final Markdown markdown;
  private final UserSessionRepository userSessionRepository;

  @Inject
  public SubmissionUiConstructor(
      SubmissionContentLinkUiConstructor contentLinkUiModelConstructor,
      ReplyRepository replyRepository,
      VotingManager votingManager,
      Markdown markdown,
      UserSessionRepository userSessionRepository)
  {
    this.contentLinkUiModelConstructor = contentLinkUiModelConstructor;
    this.replyRepository = replyRepository;
    this.votingManager = votingManager;
    this.markdown = markdown;
    this.userSessionRepository = userSessionRepository;
  }

  /**
   * @param optionalSubmissions Can emit twice. Once w/o comments and once with comments.
   */
  @CheckResult
  public Observable<List<SubmissionScreenUiModel>> stream(
      Context context,
      CommentTreeConstructor commentTreeConstructor,
      Observable<Optional<Submission>> optionalSubmissions,
      Observable<DankSubmissionRequest> submissionRequests,
      Observable<Optional<Link>> contentLinks,
      Observable<Optional<SubmissionContentLoadError>> mediaContentLoadErrors,
      Observable<Optional<ResolvedError>> commentsLoadErrors)
  {
    return optionalSubmissions
        .distinctUntilChanged()
        .switchMap(optional -> {
          if (!optional.isPresent()) {
            return commentsLoadErrors.map(optionalError -> optionalError.isPresent()
                ? Collections.singletonList(SubmissionCommentsLoadError.UiModel.create(optionalError.get()))
                : Collections.emptyList());
          }

          Observable<Submission> submissions = optionalSubmissions
              // Not sure why, but the parent switchMap() on submission change gets triggered
              // after this chain receives an empty submission, so adding this extra takeWhile().
              .takeWhile(optionalSub -> optionalSub.isPresent())
              .map(submissionOptional -> submissionOptional.get());

          Observable<Optional<SubmissionContentLinkUiModel>> contentLinkUiModels = Observable
              .combineLatest(contentLinks, submissions.observeOn(io()), Pair::create)
              .distinctUntilChanged()
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
              .map(pendingSyncReplies -> pendingSyncReplies.size());

          Observable<SubmissionCommentsHeader.UiModel> headerUiModels = Observable.combineLatest(
              votingManager.streamChanges().observeOn(io()).map(o -> context),
              submissions.observeOn(io()),
              submissionPendingSyncReplyCounts,
              contentLinkUiModels,
              this::headerUiModel
          );

          // FIXME: The error message says "Reddit" even for imgur, and other services.
          Observable<Optional<SubmissionMediaContentLoadError.UiModel>> contentLoadErrorUiModels = mediaContentLoadErrors
              .map(optionalError -> optionalError.map(error -> error.uiModel(context)));

          Observable<Optional<SubmissionCommentsViewFullThread.UiModel>> viewFullThreadUiModels = submissionRequests
              .map(request -> request.focusCommentId() == null
                  ? Optional.<SubmissionCommentsViewFullThread.UiModel>empty()
                  : Optional.of(SubmissionCommentsViewFullThread.UiModel.create(request)));

          Observable<List<SubmissionScreenUiModel>> commentRowUiModels = Observable.combineLatest(
              votingManager.streamChanges().observeOn(io()).map(o -> context),
              submissions.observeOn(io()).compose(commentTreeConstructor.stream(io())),
              submissions.take(1).map(Submission::getAuthor),
              submissionRequests
                  .map(submissionRequest -> Optional.ofNullable(submissionRequest.focusCommentId()))
                  .map(optionalId -> optionalId.map(FocusedComment::create))
                  .distinctUntilChanged(),
              this::commentRowUiModels
          );

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

          return Observable.combineLatest(
              headerUiModels,
              contentLoadErrorUiModels,
              viewFullThreadUiModels,
              commentsLoadProgressUiModels,
              commentsLoadErrorUiModels,
              commentRowUiModels,
              (header, optionalContentError, viewFullThread, optionalCommentsLoadProgress, optionalCommentsLoadError, commentRowModels) -> {
                List<SubmissionScreenUiModel> allItems = new ArrayList<>(4 + commentRowModels.size());
                allItems.add(header);
                optionalContentError.ifPresent(allItems::add);
                viewFullThread.ifPresent(allItems::add);
                allItems.addAll(commentRowModels);

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
      int pendingSyncReplyCount,
      Optional<SubmissionContentLinkUiModel> contentLinkUiModel)
  {
    VoteDirection pendingOrDefaultVote = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
    int voteDirectionColor = Commons.voteColor(pendingOrDefaultVote);
    long adapterId = submission.getFullName().hashCode();

    Optional<CharSequence> selfTextOptional = submission.isSelfPost() && !submission.getSelftext().isEmpty()
        ? Optional.of(markdown.parseSelfText(submission))
        : Optional.empty();

    int vote = votingManager.getScoreAfterAdjustingPendingVote(submission);
    int postedAndPendingCommentCount = submission.getCommentCount() + pendingSyncReplyCount;

    Truss titleBuilder = new Truss();
    titleBuilder.pushSpan(new ForegroundColorSpan(color(context, voteDirectionColor)));
    titleBuilder.append(Strings.abbreviateScore(vote));
    titleBuilder.popSpan();
    titleBuilder.append("  ");
    //noinspection deprecation
    titleBuilder.append(Html.fromHtml(submission.getTitle()));
    CharSequence title = titleBuilder.build();
    String byline = context.getString(
        R.string.submission_byline,
        submission.getSubredditName(),
        submission.getAuthor(),
        Dates.createTimestamp(context.getResources(), JrawUtils.createdTimeUtc(submission)),
        Strings.abbreviateScore(postedAndPendingCommentCount)
    );

    return SubmissionCommentsHeader.UiModel.builder()
        .adapterId(adapterId)
        .title(title, Pair.create(vote, pendingOrDefaultVote))
        .byline(byline, postedAndPendingCommentCount)
        .selfText(selfTextOptional)
        .optionalContentLinkModel(contentLinkUiModel)
        .submission(submission)
        .build();
  }

  private List<SubmissionScreenUiModel> commentRowUiModels(
      Context context,
      List<SubmissionCommentRow> rows,
      String submissionAuthor,
      Optional<FocusedComment> focusedComment)
  {
    List<SubmissionScreenUiModel> uiModels = new ArrayList<>(rows.size());
    for (SubmissionCommentRow row : rows) {
      switch (row.type()) {
        case USER_COMMENT: {
          DankCommentNode dankCommentNode = (DankCommentNode) row;
          String commentFullName = dankCommentNode.commentNode().getComment().getFullName();
          boolean isFocused = focusedComment.isPresent() && focusedComment.get().fullname().equals(commentFullName);
          uiModels.add(syncedCommentUiModel(context, dankCommentNode, submissionAuthor, isFocused));
          break;
        }

        case PENDING_SYNC_REPLY:
          CommentPendingSyncReplyItem pendingSyncReplyItem = (CommentPendingSyncReplyItem) row;
          String replyFullName = pendingSyncReplyItem.pendingSyncReply().postedFullName();
          boolean isFocused = focusedComment.isPresent() && focusedComment.get().fullname().equals(replyFullName);
          uiModels.add(pendingSyncCommentUiModel(context, pendingSyncReplyItem, isFocused));
          break;

        case INLINE_REPLY:
          String loggedInUserName = userSessionRepository.loggedInUserName();
          uiModels.add(inlineReplyUiModel(context, (CommentInlineReplyItem) row, loggedInUserName));
          break;

        case LOAD_MORE_COMMENTS:
          uiModels.add(loadMoreUiModel(context, ((LoadMoreCommentItem) row)));
          break;

        default:
          throw new AssertionError("Unknown type: " + row.type());
      }
    }
    return uiModels;
  }

  private SubmissionComment.UiModel syncedCommentUiModel(Context context, DankCommentNode dankCommentNode, String submissionAuthor, boolean isFocused) {
    Comment comment = dankCommentNode.commentNode().getComment();
    Optional<String> authorFlairText = comment.getAuthorFlair() != null ? Optional.ofNullable(comment.getAuthorFlair().getText()) : Optional.empty();
    long createdTimeMillis = JrawUtils.createdTimeUtc(comment);
    VoteDirection pendingOrDefaultVoteDirection = votingManager.getPendingOrDefaultVote(comment, comment.getVote());
    int commentScore = votingManager.getScoreAfterAdjustingPendingVote(comment);
    boolean isAuthorOP = comment.getAuthor().equalsIgnoreCase(submissionAuthor);

    // TODO: getTotalSize() is buggy. See: https://github.com/thatJavaNerd/JRAW/issues/189
    int childCommentsCount = dankCommentNode.commentNode().getTotalSize();

    CharSequence byline = constructCommentByline(
        context,
        comment.getAuthor(),
        authorFlairText,
        isAuthorOP,
        createdTimeMillis,
        pendingOrDefaultVoteDirection,
        commentScore,
        childCommentsCount,
        dankCommentNode.isCollapsed()
    );

    CharSequence commentBody = dankCommentNode.isCollapsed()
        ? markdown.stripMarkdown(JrawUtils.commentBodyHtml(comment))
        : markdown.parse(comment);

    return commentUiModelBuilder(context, dankCommentNode.adapterId(), dankCommentNode.isCollapsed(), isFocused, dankCommentNode.commentNode().getDepth())
        .optionalComment(Optional.of(comment))
        .commentInfo(PostedOrInFlightContribution.from(comment))
        .optionalPendingSyncReply(Optional.empty())
        .byline(byline, commentScore)
        .body(commentBody)
        .build();
  }

  /**
   * Reply posted by the logged in user that hasn't synced yet or whose actual comment hasn't been fetched yet.
   */
  private SubmissionComment.UiModel pendingSyncCommentUiModel(Context context, CommentPendingSyncReplyItem pendingSyncReplyRow, boolean isFocused) {
    PendingSyncReply pendingSyncReply = pendingSyncReplyRow.pendingSyncReply();
    CharSequence byline;
    int commentScore = 1;

    if (pendingSyncReply.state() == PendingSyncReply.State.POSTED) {
      Optional<String> authorFlairText = Optional.empty();
      byline = constructCommentByline(
          context,
          pendingSyncReply.author(),
          authorFlairText,
          true,
          pendingSyncReply.createdTimeMillis(),
          VoteDirection.UPVOTE,
          commentScore,
          0,
          pendingSyncReplyRow.isCollapsed()
      );

    } else {
      Truss bylineBuilder = new Truss();
      if (pendingSyncReplyRow.isCollapsed()) {
        bylineBuilder.append(pendingSyncReply.author());
      } else {
        bylineBuilder.pushSpan(new ForegroundColorSpan(color(context, R.color.submission_comment_byline_author_op)));
        bylineBuilder.append(pendingSyncReply.author());
        bylineBuilder.popSpan();
      }
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));

      if (pendingSyncReply.state() == PendingSyncReply.State.POSTING) {
        bylineBuilder.append(context.getString(R.string.submission_comment_reply_byline_posting_status));
      } else if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
        bylineBuilder.pushSpan(new ForegroundColorSpan(color(context, R.color.submission_comment_byline_failed_to_post)));
        bylineBuilder.append(context.getString(R.string.submission_comment_reply_byline_failed_status));
        bylineBuilder.popSpan();
      } else {
        throw new AssertionError();
      }

      byline = bylineBuilder.build();
    }

    CharSequence commentBody = pendingSyncReplyRow.isCollapsed()
        ? markdown.stripMarkdown(pendingSyncReply.body())
        : markdown.parse(pendingSyncReply);

    return commentUiModelBuilder(context, pendingSyncReplyRow.adapterId(), pendingSyncReplyRow.isCollapsed(), isFocused, pendingSyncReplyRow.depth())
        .optionalComment(Optional.empty())
        .commentInfo(PostedOrInFlightContribution.createLocal(pendingSyncReply))
        .optionalPendingSyncReply(Optional.of(pendingSyncReply))
        .byline(byline, commentScore)
        .body(commentBody)
        .build();
  }

  /**
   * Builds common properties for both comments and pending-sync-replies.
   */
  private SubmissionComment.UiModel.Builder commentUiModelBuilder(
      Context context,
      String adapterId,
      boolean isCollapsed,
      boolean isFocused,
      int depth)
  {
    @ColorRes int backgroundColorRes = isFocused
        ? R.color.submission_comment_background_focused
        : R.color.submission_comment_background;

    return SubmissionComment.UiModel.builder()
        .adapterId(adapterId.hashCode())
        .bylineTextColor(color(context,
            isCollapsed
                ? R.color.submission_comment_body_collapsed
                : R.color.submission_comment_byline_default_color
        ))
        .bodyTextColor(color(context, isCollapsed
            ? R.color.submission_comment_body_collapsed
            : R.color.submission_comment_body_expanded
        ))
        .indentationDepth(depth - 1)  // TODO: Why are we subtracting 1 here?
        .bodyMaxLines(isCollapsed ? 1 : Integer.MAX_VALUE)
        .isCollapsed(isCollapsed)
        .backgroundColorRes(backgroundColorRes)
        .isFocused(isFocused);
  }

  private CharSequence constructCommentByline(
      Context context,
      String author,
      Optional<String> optionalAuthorFlairText,
      boolean isAuthorOP,
      long createdTimeMillis,
      VoteDirection voteDirection,
      int commentScore,
      int childCommentsCount,
      boolean isCollapsed)
  {
    Truss bylineBuilder = new Truss();
    if (isCollapsed) {
      bylineBuilder.append(author);
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));

      int hiddenCommentsCount = childCommentsCount + 1;   // +1 for the parent comment itself.
      String hiddenCommentsString = context.getResources().getQuantityString(R.plurals.submission_comment_hidden_comments, hiddenCommentsCount);
      bylineBuilder.append(String.format(hiddenCommentsString, hiddenCommentsCount));

    } else {
      bylineBuilder.pushSpan(new ForegroundColorSpan(color(context, isAuthorOP
          ? R.color.submission_comment_byline_author_op
          : R.color.submission_comment_byline_author)
      ));
      bylineBuilder.append(author);
      bylineBuilder.popSpan();
      optionalAuthorFlairText.ifPresent(flair -> {
        bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
        bylineBuilder.append(flair);
      });
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.pushSpan(new ForegroundColorSpan(color(context, Commons.voteColor(voteDirection))));
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_score, Strings.abbreviateScore(commentScore)));
      bylineBuilder.popSpan();
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.append(Dates.createTimestamp(context.getResources(), createdTimeMillis));
    }
    return bylineBuilder.build();
  }

  private SubmissionCommentInlineReply.UiModel inlineReplyUiModel(Context context, CommentInlineReplyItem inlineReplyItem, String loggedInUserName) {
    long adapterId = inlineReplyItem.adapterId().hashCode();
    PostedOrInFlightContribution parentContribution = PostedOrInFlightContribution.from(inlineReplyItem.parentContribution());
    CharSequence authorHint = context.getResources().getString(R.string.submission_comment_reply_author_hint, loggedInUserName);
    return SubmissionCommentInlineReply.UiModel.create(adapterId, authorHint, parentContribution, inlineReplyItem.depth());
  }

  /**
   * For loading more replies of a comment.
   */
  private SubmissionCommentsLoadMore.UiModel loadMoreUiModel(Context context, LoadMoreCommentItem row) {
    CommentNode parentCommentNode = row.parentCommentNode();
    boolean clickEnabled;
    String label;
    if (row.progressVisible()) {
      label = context.getString(R.string.submission_loading_more_comments);
      clickEnabled = false;

    } else {
      label = parentCommentNode.isThreadContinuation()
          ? context.getString(R.string.submission_continue_this_thread)
          : context.getString(R.string.submission_load_more_comments, parentCommentNode.getMoreChildren().getCount());
      clickEnabled = true;
    }

    return SubmissionCommentsLoadMore.UiModel.builder()
        .adapterId((long) row.adapterId().hashCode())
        .label(label)
        .iconRes(parentCommentNode.isThreadContinuation() ? R.drawable.ic_arrow_forward_12dp : 0)
        .indentationDepth(parentCommentNode.getDepth())
        .parentCommentNode(parentCommentNode)
        .clickEnabled(clickEnabled)
        .build();
  }

  @ColorInt
  private static int color(Context context, @ColorRes int colorRes) {
    return ContextCompat.getColor(context, colorRes);
  }
}
