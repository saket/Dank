package me.saket.dank.ui.submission.adapter;

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
import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.VotingManager;
import me.saket.dank.data.links.Link;
import me.saket.dank.ui.submission.CommentInlineReplyItem;
import me.saket.dank.ui.submission.CommentPendingSyncReplyItem;
import me.saket.dank.ui.submission.DankCommentNode;
import me.saket.dank.ui.submission.LoadMoreCommentItem;
import me.saket.dank.ui.submission.ParentThread;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.submission.ReplyRepository;
import me.saket.dank.ui.submission.SubmissionCommentRow;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Arrays2;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Trio;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.Urls;
import timber.log.Timber;

// TODO: Build a subcomponent for SubredditActivity?
public class SubmissionScreenUiModelConstructor {

  private final SubmissionContentLinkUiModelConstructor contentLinkUiModelConstructor;
  private final ReplyRepository replyRepository;
  private final VotingManager votingManager;
  private final Markdown markdown;
  private final UserSessionRepository userSessionRepository;

  @Inject
  public SubmissionScreenUiModelConstructor(
      SubmissionContentLinkUiModelConstructor contentLinkUiModelConstructor,
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

  @CheckResult
  public Observable<List<SubmissionScreenUiModel>> stream(
      Context context,
      Observable<Submission> submissions,
      Observable<Optional<Link>> contentLinks,
      Observable<List<SubmissionCommentRow>> commentRows)
  {
    Observable<Optional<SubmissionContentLinkUiModel>> contentLinkUiModels = contentLinks
        .switchMap(optional -> {
          if (optional.isPresent()) {
            return contentLinkUiModelConstructor
                .streamLoad(context, optional.get())
                .doOnError(e -> Timber.e(e))
                .as(Optional.of());
          } else {
            return Observable.just(Optional.empty());
          }
        });

    Observable<Integer> submissionPendingSyncReplyCounts = submissions
        .switchMap(submission -> replyRepository.streamPendingSyncReplies(ParentThread.of(submission)))
        .map(pendingSyncReplies -> pendingSyncReplies.size());

    Observable<SubmissionCommentsHeader.UiModel> submissionHeaderUiModels = Observable
        .combineLatest(submissions, submissionPendingSyncReplyCounts, contentLinkUiModels, Trio::create)
        .map(trio -> {
          Submission submission = trio.first();
          Integer pendingSyncReplyCount = trio.second();
          Optional<SubmissionContentLinkUiModel> contentLinkUiModel = trio.third();
          return headerUiModel(context, submission, pendingSyncReplyCount, contentLinkUiModel);
        });

    Observable<List<SubmissionScreenUiModel>> commentRowUiModels = commentRows
        .withLatestFrom(submissions.map(submission -> submission.getAuthor()), Pair::create)
        .map(pair -> {
          List<SubmissionCommentRow> rows = pair.first();
          String submissionAuthor = pair.second();

          List<SubmissionScreenUiModel> uiModels = new ArrayList<>(rows.size());
          for (SubmissionCommentRow row : rows) {
            switch (row.type()) {
              case USER_COMMENT:
                uiModels.add(commentUiModel(context, ((DankCommentNode) row), submissionAuthor));
                break;

              case PENDING_SYNC_REPLY:
                uiModels.add(pendingSyncCommentUiModel(context, ((CommentPendingSyncReplyItem) row)));
                break;

              case INLINE_REPLY:
                String loggedInUserName = userSessionRepository.loggedInUserName();
                uiModels.add(inlineReplyUiModel(context, ((CommentInlineReplyItem) row), loggedInUserName));
                break;

              case LOAD_MORE_COMMENTS:
                uiModels.add(loadMoreUiModel(context, ((LoadMoreCommentItem) row)));
                break;

              default:
                throw new AssertionError("Unknown type: " + row.type());
            }
          }
          return uiModels;
        });

    return Observable.combineLatest(submissionHeaderUiModels, commentRowUiModels, Arrays2::concatenate);
  }

  /** Header contains submission details, content link and self-text post. */
  private SubmissionCommentsHeader.UiModel headerUiModel(
      Context context,
      Submission submission,
      int pendingSyncReplyCount,
      Optional<SubmissionContentLinkUiModel> contentLinkUiModel)
  {
    VoteDirection pendingOrDefaultVote = votingManager.getPendingOrDefaultVote(submission, submission.getVote());
    int voteDirectionColor = Commons.voteColor(pendingOrDefaultVote);
    long adapterId = submission.getFullName().hashCode();

    // TODO.
//    selfPostTextView.setVisibility(markdownHtml.length() > 0 ? View.VISIBLE : View.GONE);
    Optional<CharSequence> selfTextOptional = submission.isSelfPost()
        ? Optional.of(markdown.parseSelfText(submission))
        : Optional.empty();

    Truss titleBuilder = new Truss();
    titleBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(context, voteDirectionColor)));
    titleBuilder.append(Strings.abbreviateScore(votingManager.getScoreAfterAdjustingPendingVote(submission)));
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
        Strings.abbreviateScore(submission.getCommentCount() + pendingSyncReplyCount)
    );

    return SubmissionCommentsHeader.UiModel.create(adapterId, title, byline, selfTextOptional, contentLinkUiModel);
  }

  // TODO.
  private SubmissionContentLinkUiModel contentLinkUiModel(Link link) {
    return SubmissionContentLinkUiModel.builder()
        .title(link.unparsedUrl())
        .byline(Urls.parseDomainName(link.unparsedUrl()))
        .icon(null)
        .thumbnail(null)
        .progressVisible(false)
        .titleMaxLines(2)
        .build();
  }

  private SubmissionComment.UiModel commentUiModel(Context context, DankCommentNode dankCommentNode, String submissionAuthor) {
    Comment comment = dankCommentNode.commentNode().getComment();
    String authorFlairText = comment.getAuthorFlair() != null ? comment.getAuthorFlair().getText() : null;
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
        ? Markdown.stripMarkdown(JrawUtils.commentBodyHtml(comment))
        : markdown.parse(comment);

    return commentUiModelBuilder(context, dankCommentNode.fullName(), dankCommentNode.isCollapsed(), dankCommentNode.commentNode().getDepth())
        .byline(byline)
        .body(commentBody)
        .build();
  }

  /** Reply posted by the logged in user that hasn't synced yet or whose actual comment hasn't been fetched yet. */
  private SubmissionComment.UiModel pendingSyncCommentUiModel(Context context, CommentPendingSyncReplyItem pendingSyncReplyRow) {
    PendingSyncReply pendingSyncReply = pendingSyncReplyRow.pendingSyncReply();
    CharSequence byline;

    if (pendingSyncReply.state() == PendingSyncReply.State.POSTED) {
      byline = constructCommentByline(
          context,
          pendingSyncReply.author(),
          null,
          true,
          pendingSyncReply.createdTimeMillis(),
          VoteDirection.UPVOTE,
          1,
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
        ? Markdown.stripMarkdown(pendingSyncReply.body())
        : markdown.parse(pendingSyncReply);

    return commentUiModelBuilder(context, pendingSyncReplyRow.fullName(), pendingSyncReplyRow.isCollapsed(), pendingSyncReplyRow.depth())
        .byline(byline)
        .body(commentBody)
        .build();
  }

  /** Builds common properties for both comments and pending-sync-replies. */
  private SubmissionComment.UiModel.Builder commentUiModelBuilder(
      Context context,
      String fullName,
      boolean isCollapsed,
      int depth)
  {
    return SubmissionComment.UiModel.builder()
        .adapterId(fullName.hashCode())
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
        .bodyMaxLines(isCollapsed ? 1 : Integer.MAX_VALUE);
  }

  private CharSequence constructCommentByline(
      Context context,
      String author,
      String authorFlairText,
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
      if (authorFlairText != null) {
        bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
        bylineBuilder.append(authorFlairText);
      }
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(context, Commons.voteColor(voteDirection))));
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_score, Strings.abbreviateScore(commentScore)));
      bylineBuilder.popSpan();
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.append(Dates.createTimestamp(context.getResources(), createdTimeMillis));
    }
    return bylineBuilder.build();
  }

  private SubmissionInlineReply.UiModel inlineReplyUiModel(Context context, CommentInlineReplyItem inlineReplyItem, String loggedInUserName) {
    long adapterId = inlineReplyItem.fullName().hashCode();
    String parentContributionFullName = inlineReplyItem.parentContribution().getFullName();
    CharSequence authorHint = context.getResources().getString(R.string.submission_comment_reply_author_hint, loggedInUserName);
    return SubmissionInlineReply.UiModel.create(adapterId, authorHint, parentContributionFullName, inlineReplyItem.depth());
  }

  /** For loading more replies of a comment. */
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
        .adapterId((long) row.fullName().hashCode())
        .label(label)
        .iconRes(parentCommentNode.isThreadContinuation() ? R.drawable.ic_arrow_forward_12dp : 0)
        .indentationDepth(parentCommentNode.getDepth())
        .clickEnabled(clickEnabled)
        .build();
  }

  @ColorInt
  private static int color(Context context, @ColorRes int colorRes) {
    return ContextCompat.getColor(context, colorRes);
  }
}
