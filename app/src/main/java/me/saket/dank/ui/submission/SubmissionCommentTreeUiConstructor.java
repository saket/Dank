package me.saket.dank.ui.submission;


import static me.saket.dank.utils.Arrays2.immutable;

import android.content.Context;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.text.style.ForegroundColorSpan;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import me.saket.dank.R;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.data.LocallyPostedComment;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.submission.adapter.SubmissionComment;
import me.saket.dank.ui.submission.adapter.SubmissionCommentInlineReply;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsLoadMore;
import me.saket.dank.ui.submission.adapter.SubmissionScreenUiModel;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Preconditions;
import me.saket.dank.utils.RxHashSet;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import timber.log.Timber;

/**
 * Constructs comments to show in a submission. Ignores collapsed comments + adds reply fields + adds "load more"
 * & "continue thread ->" items.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class SubmissionCommentTreeUiConstructor {

  private static final ActiveReplyIds ACTIVE_REPLY_IDS = new ActiveReplyIds();
  private static final CollapsedCommentIds COLLAPSED_COMMENT_IDS = new CollapsedCommentIds(50);
  private static final InFlightLoadMoreIds IN_FLIGHT_LOAD_MORE_IDS = new InFlightLoadMoreIds();

  private final Lazy<ReplyRepository> replyRepository;
  private final Lazy<VotingManager> votingManager;
  private final Lazy<Markdown> markdown;
  private final Lazy<UserSessionRepository> userSessionRepository;

  /** Contribution IDs for which inline replies are active. */
  static class ActiveReplyIds extends RxHashSet<String> {
    public boolean isActive(Contribution parentContribution) {
      return contains(keyFor(parentContribution));
    }

    public void showFor(Contribution parentContribution) {
      add(keyFor(parentContribution));
    }

    public void hideFor(Contribution parentContribution) {
      remove(keyFor(parentContribution));
    }
  }

  /** Comment IDs that are collapsed. */
  static class CollapsedCommentIds extends RxHashSet<String> {
    private boolean changeEventsEnabled = true;

    public CollapsedCommentIds(int initialCapacity) {
      super(initialCapacity);
    }

    @Override
    public Observable<Integer> changes() {
      return super
          .changes()
          .filter(o -> {
            if (!changeEventsEnabled) {
              Timber.w("Filtering collapse change event");
            }
            return changeEventsEnabled;
          });
    }

    public boolean isCollapsed(Comment comment) {
      return contains(keyFor(comment));
    }

    public void expand(Comment comment) {
      boolean removed = remove(keyFor(comment));
      if (!removed) {
        throw new AssertionError("This comment isn't collapsed: " + comment);
      }
    }

    public void collapse(Comment comment) {
      add(keyFor(comment));
    }

    public void pauseChangeEvents() {
      changeEventsEnabled = false;
    }

    public void resumeChangeEvents() {
      changeEventsEnabled = true;
    }
  }

  /** Comment IDs for which more child comments are being fetched. */
  static class InFlightLoadMoreIds extends RxHashSet<String> {
    public boolean isInFlightFor(CommentNode commentNode) {
      return contains(keyFor(commentNode.getComment()));
    }

    public void showLoadMoreFor(Comment parentComment) {
      add(keyFor(parentComment));
    }

    public void hideLoadMoreFor(Comment parentComment) {
      boolean removed = remove(keyFor(parentComment));
      if (!removed) {
        throw new AssertionError("More comments weren't in flight for: " + parentComment);
      }
    }
  }

  class PendingSyncRepliesMap extends HashMap<String, List<PendingSyncReply>> {
    @Override
    public List<PendingSyncReply> put(String key, List<PendingSyncReply> value) {
      return super.put(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
      return super.remove(key, value);
    }

    public boolean hasForParent(Comment parentComment) {
      return containsKey(parentComment.getFullName());
    }

    public List<PendingSyncReply> getForParent(Comment parentComment) {
      return get(parentComment.getFullName());
    }
  }

  @Inject
  public SubmissionCommentTreeUiConstructor(
      Lazy<ReplyRepository> replyRepository,
      Lazy<VotingManager> votingManager,
      Lazy<Markdown> markdown,
      Lazy<UserSessionRepository> userSessionRepository)
  {
    this.replyRepository = replyRepository;
    this.votingManager = votingManager;
    this.markdown = markdown;
    this.userSessionRepository = userSessionRepository;
  }

  @CheckResult
  public Observable<List<SubmissionScreenUiModel>> stream(
      Context context,
      Observable<Submission> submissions,
      Observable<DankSubmissionRequest> submissionRequests,
      Scheduler scheduler)
  {
    Observable<PendingSyncRepliesMap> pendingSyncRepliesMaps = submissions
        .distinctUntilChanged()
        .flatMap(submission -> replyRepository.get().streamPendingSyncReplies(ParentThread.of(submission)))
        // I've seen this stream very occasionally take a second to emit, so starting with a default value.
        .startWith(Collections.<PendingSyncReply>emptyList())
        .map(replyList -> createPendingSyncReplyMap(replyList));

    Observable<?> rowVisibilityChanges = Observable.merge(
        ACTIVE_REPLY_IDS.changes(),
        COLLAPSED_COMMENT_IDS.changes(),
        IN_FLIGHT_LOAD_MORE_IDS.changes()
    );

    Observable<Optional<FocusedComment>> focusedComments = submissionRequests
        .map(submissionRequest -> Optional.ofNullable(submissionRequest.focusCommentId()))
        .map(optionalId -> optionalId.map(FocusedComment::create))
        .distinctUntilChanged();

    return Observable
        .combineLatest(
            submissions,
            pendingSyncRepliesMaps,
            focusedComments,
            rowVisibilityChanges.observeOn(scheduler),                // observeOn() because the relays emit on the main thread.
            votingManager.get().streamChanges().observeOn(scheduler),
            (submission, pendingSyncRepliesMap, focusedComment, o, oo) -> {
              String submissionAuthor = submission.getAuthor();
              return constructComments(context, submission, pendingSyncRepliesMap, submissionAuthor, focusedComment);
            })
        .as(immutable());
  }

  private static String keyFor(Contribution contribution) {
    if (contribution instanceof Submission) {
      return contribution.getFullName();
    }
    if (contribution instanceof ContributionFullNameWrapper) {
      return contribution.getFullName();
    }
    if (contribution instanceof LocallyPostedComment) {
      String key = ((LocallyPostedComment) contribution).getPostingStatusIndependentId();
      return Preconditions.checkNotNull(key, "LocallyPostedComment#getPostingStatusIndependentId()");
    }
    if (contribution instanceof Comment) {
      return contribution.getFullName();
    }
    throw new UnsupportedOperationException("Unknown contribution: " + contribution);
  }

  // Key: comment full-name.
  private PendingSyncRepliesMap createPendingSyncReplyMap(List<PendingSyncReply> pendingReplies) {
    PendingSyncRepliesMap pendingReplyMap = new PendingSyncRepliesMap();

    for (PendingSyncReply pendingSyncReply : pendingReplies) {
      String parentCommentFullName = pendingSyncReply.parentContributionFullName();

      if (!pendingReplyMap.containsKey(parentCommentFullName)) {
        pendingReplyMap.put(parentCommentFullName, new ArrayList<>(4));
      }
      List<PendingSyncReply> existingReplies = pendingReplyMap.get(parentCommentFullName);
      existingReplies.add(pendingSyncReply);
      pendingReplyMap.put(parentCommentFullName, existingReplies);
    }
    return pendingReplyMap;
  }

  /**
   * Collapse/expand a comment.
   */
  void toggleCollapse(Comment comment) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(comment)) {
      COLLAPSED_COMMENT_IDS.expand(comment);
    } else {
      COLLAPSED_COMMENT_IDS.collapse(comment);
    }
  }

  boolean isCollapsed(Comment contribution) {
    return COLLAPSED_COMMENT_IDS.isCollapsed(contribution);
  }

  /**
   * Enable "loading more…" progress indicator.
   *
   * @param parentComment for which more child nodes are being fetched.
   */
  void setMoreCommentsLoading(Comment parentComment, boolean loading) {
    if (loading) {
      IN_FLIGHT_LOAD_MORE_IDS.showLoadMoreFor(parentComment);
    } else {
      IN_FLIGHT_LOAD_MORE_IDS.hideLoadMoreFor(parentComment);
    }
  }

  /**
   * Show reply field for a comment and also expand any hidden comments.
   */
  void showReplyAndExpandComments(Comment parentComment) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(parentComment)) {
      COLLAPSED_COMMENT_IDS.pauseChangeEvents();
      COLLAPSED_COMMENT_IDS.expand(parentComment);
      COLLAPSED_COMMENT_IDS.resumeChangeEvents();
    }

    ACTIVE_REPLY_IDS.showFor(parentComment);
  }

  /**
   * Show reply field for the submission or a comment.
   */
  void showReply(Contribution parentContribution) {
    ACTIVE_REPLY_IDS.showFor(parentContribution);
  }

  /**
   * Hide reply field for a comment.
   */
  void hideReply(Contribution parentContribution) {
    ACTIVE_REPLY_IDS.hideFor(parentContribution);
  }

  boolean isReplyActiveFor(Contribution contribution) {
    return ACTIVE_REPLY_IDS.isActive(contribution);
  }

  public boolean isMoreCommentsInFlightFor(CommentNode commentNode) {
    return IN_FLIGHT_LOAD_MORE_IDS.isInFlightFor(commentNode);
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionScreenUiModel> constructComments(
      Context context,
      Submission submission,
      PendingSyncRepliesMap pendingSyncRepliesMap,
      String submissionAuthor,
      Optional<FocusedComment> focusedComment)
  {
    int totalRowsSize = 0;
    if (ACTIVE_REPLY_IDS.isActive(submission)) {
      totalRowsSize += 1;
    }

    CommentNode rootCommentNode = submission.getComments();
    if (rootCommentNode != null) {
      totalRowsSize += rootCommentNode.getTotalSize();
    }

    ArrayList<SubmissionScreenUiModel> flattenComments = new ArrayList<>(totalRowsSize);
    if (ACTIVE_REPLY_IDS.isActive(submission)) {
      String loggedInUserName = userSessionRepository.get().loggedInUserName();
      flattenComments.add(inlineReplyUiModel(context, submission, loggedInUserName, 0));
    }

    if (rootCommentNode == null) {
      return flattenComments;
    } else {
      return constructComments(context, flattenComments, rootCommentNode, submission, pendingSyncRepliesMap, submissionAuthor, focusedComment);
    }
  }

  /**
   * Walk through the tree in pre-order, ignoring any collapsed comment tree node and flatten them in a single List.
   */
  private List<SubmissionScreenUiModel> constructComments(
      Context context,
      List<SubmissionScreenUiModel> flattenComments,
      CommentNode nextNode,
      Submission submission,
      PendingSyncRepliesMap pendingSyncRepliesMap,
      String submissionAuthor,
      Optional<FocusedComment> focusedComment)
  {
    //String indentation = "";
    //if (nextNode.getDepth() != 0) {
    //  for (int step = 0; step < nextNode.getDepth(); step++) {
    //    indentation += "  ";
    //  }
    //}

    boolean isCommentNodeCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(nextNode.getComment());
    boolean isReplyActive = ACTIVE_REPLY_IDS.isActive(nextNode.getComment());

    if (nextNode.getDepth() != 0) {
      //Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
      String commentFullName = nextNode.getComment().getFullName();
      boolean isFocused = focusedComment.isPresent() && focusedComment.get().fullname().equals(commentFullName);
      flattenComments.add(syncedCommentUiModel(context, nextNode, isCommentNodeCollapsed, submissionAuthor, isFocused));
    }

    // Reply box.
    // Skip for root-node because we already added a reply for the submission in constructComments().
    boolean isSubmission = nextNode.getComment().getFullName().equals(submission.getFullName());
    if (!isSubmission && isReplyActive && !isCommentNodeCollapsed) {
      String loggedInUserName = userSessionRepository.get().loggedInUserName();
      flattenComments.add(inlineReplyUiModel(context, nextNode.getComment(), loggedInUserName, nextNode.getDepth()));
    }

    // Pending-sync replies.
    if (!isCommentNodeCollapsed && pendingSyncRepliesMap.hasForParent(nextNode.getComment())) {
      List<PendingSyncReply> pendingSyncReplies = pendingSyncRepliesMap.getForParent(nextNode.getComment());
      for (int i = 0; i < pendingSyncReplies.size(); i++) {     // Intentionally avoiding thrashing Iterator objects.
        LocallyPostedComment locallyPostedComment = LocallyPostedComment.create(pendingSyncReplies.get(i));
        boolean isReplyCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(locallyPostedComment);
        int depth = nextNode.getDepth() + 1;
        boolean isFocused = focusedComment.isPresent() && focusedComment.get().fullname().equals(locallyPostedComment.getFullName());
        flattenComments.add(locallyPostedCommentUiModel(context, locallyPostedComment, isReplyCollapsed, depth, isFocused));
      }
    }

    // Next, the child comment tree.
    if (nextNode.isEmpty() && !nextNode.hasMoreComments()) {
      return flattenComments;

    } else {
      // Ignore collapsed children.
      if (!isCommentNodeCollapsed) {
        List<CommentNode> childCommentsTree = nextNode.getChildren();
        for (int i = 0; i < childCommentsTree.size(); i++) {  // Intentionally avoiding thrashing Iterator objects.
          CommentNode node = childCommentsTree.get(i);
          constructComments(context, flattenComments, node, submission, pendingSyncRepliesMap, submissionAuthor, focusedComment);
        }

        if (nextNode.hasMoreComments()) {
          //Timber.d("%s(%s) %s has %d MORE ---------->",
          //    indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getMoreChildren().getCount()
          //);
          //Timber.d("%s %s", indentation, nextNode.getMoreChildren().getChildrenIds());
          flattenComments.add(loadMoreUiModel(context, nextNode, IN_FLIGHT_LOAD_MORE_IDS.isInFlightFor(nextNode)));
        }
      }
      return flattenComments;
    }
  }

  private SubmissionComment.UiModel syncedCommentUiModel(
      Context context,
      CommentNode commentNode,
      boolean isCollapsed,
      String submissionAuthor,
      boolean isFocused)
  {
    Comment comment = commentNode.getComment();
    Optional<String> authorFlairText = comment.getAuthorFlair() != null ? Optional.ofNullable(comment.getAuthorFlair().getText()) : Optional.empty();
    long createdTimeMillis = JrawUtils.createdTimeUtc(comment);
    VoteDirection pendingOrDefaultVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());
    int commentScore = votingManager.get().getScoreAfterAdjustingPendingVote(comment);
    boolean isAuthorOP = comment.getAuthor().equalsIgnoreCase(submissionAuthor);

    // TODO: getTotalSize() is buggy. See: https://github.com/thatJavaNerd/JRAW/issues/189
    int childCommentsCount = commentNode.getTotalSize();

    CharSequence byline = constructCommentByline(
        context,
        comment.getAuthor(),
        authorFlairText,
        isAuthorOP,
        createdTimeMillis,
        pendingOrDefaultVoteDirection,
        commentScore,
        childCommentsCount,
        isCollapsed
    );

    CharSequence commentBody = isCollapsed
        ? markdown.get().stripMarkdown(JrawUtils.commentBodyHtml(comment))
        : markdown.get().parse(comment);

    return commentUiModelBuilder(context, commentNode.getComment().getFullName(), isCollapsed, isFocused, commentNode.getDepth())
        .comment(comment)
        .optionalPendingSyncReply(Optional.empty())
        .byline(byline, commentScore)
        .body(commentBody)
        .build();
  }

  /**
   * Reply posted by the logged in user that hasn't synced yet or whose actual comment hasn't been fetched yet.
   */
  private SubmissionComment.UiModel locallyPostedCommentUiModel(
      Context context,
      LocallyPostedComment locallyPostedComment,
      boolean isReplyCollapsed,
      int depth,
      boolean isFocused)
  {
    PendingSyncReply pendingSyncReply = locallyPostedComment.pendingSyncReply();
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
          isReplyCollapsed
      );

    } else {
      Truss bylineBuilder = new Truss();
      if (isReplyCollapsed) {
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

    CharSequence commentBody = isReplyCollapsed
        ? markdown.get().stripMarkdown(pendingSyncReply.body())
        : markdown.get().parse(pendingSyncReply);

    return commentUiModelBuilder(context, locallyPostedComment.getPostingStatusIndependentId(), isReplyCollapsed, isFocused, depth)
        .comment(LocallyPostedComment.create(pendingSyncReply))
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

  private SubmissionCommentInlineReply.UiModel inlineReplyUiModel(
      Context context,
      Contribution parentContribution,
      String loggedInUserName,
      int indentationDepth)
  {
    long adapterId = (parentContribution.getFullName() + "_reply").hashCode();
    CharSequence authorHint = context.getResources().getString(R.string.submission_comment_reply_author_hint, loggedInUserName);
    return SubmissionCommentInlineReply.UiModel.create(
        adapterId,
        authorHint,
        ContributionFullNameWrapper.createFrom(parentContribution),
        indentationDepth
    );
  }

  /**
   * For loading more replies of a comment.
   */
  private SubmissionCommentsLoadMore.UiModel loadMoreUiModel(Context context, CommentNode parentCommentNode, boolean progressVisible) {
    boolean clickEnabled;
    String label;
    if (progressVisible) {
      label = context.getString(R.string.submission_loading_more_comments);
      clickEnabled = false;

    } else {
      label = parentCommentNode.isThreadContinuation()
          ? context.getString(R.string.submission_continue_this_thread)
          : context.getString(R.string.submission_load_more_comments, parentCommentNode.getMoreChildren().getCount());
      clickEnabled = true;
    }

    String adapterId = parentCommentNode.getComment().getFullName() + "_loadMore";

    return SubmissionCommentsLoadMore.UiModel.builder()
        .adapterId(adapterId.hashCode())
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
