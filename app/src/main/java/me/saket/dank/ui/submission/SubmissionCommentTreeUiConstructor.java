package me.saket.dank.ui.submission;

import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.Arrays2.immutable;
import static me.saket.dank.utils.Preconditions.checkNotNull;

import android.content.Context;
import androidx.annotation.CheckResult;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import android.text.style.ForegroundColorSpan;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.Identifiable;
import net.dean.jraw.models.MoreChildren;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;
import net.dean.jraw.tree.CommentNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import kotlin.Triple;
import me.saket.dank.R;
import me.saket.dank.data.LocallyPostedComment;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.reply.PendingSyncReply;
import me.saket.dank.reply.ReplyRepository;
import me.saket.dank.ui.compose.SimpleIdentifiable;
import me.saket.dank.ui.submission.adapter.SubmissionCommentInlineReply;
import me.saket.dank.ui.submission.adapter.SubmissionCommentsLoadMore;
import me.saket.dank.ui.submission.adapter.SubmissionLocalComment;
import me.saket.dank.ui.submission.adapter.SubmissionRemoteComment;
import me.saket.dank.ui.submission.adapter.SubmissionScreenUiModel;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.CombineLatestWithLog;
import me.saket.dank.utils.CombineLatestWithLog.O;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils2;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.RxHashSet;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Themes;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.markdown.Markdown;
import me.saket.dank.vote.VotingManager;
import timber.log.Timber;

/**
 * Constructs comments to show in a submission. Ignores collapsed comments + adds reply fields + adds "load more"
 * & "continue thread ->" items.
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class SubmissionCommentTreeUiConstructor {

  private static final ActiveReplyIds ACTIVE_REPLY_IDS = new ActiveReplyIds();
  public static final CollapsedCommentIds COLLAPSED_COMMENT_IDS = new CollapsedCommentIds(50);
  private static final InFlightLoadMoreIds IN_FLIGHT_LOAD_MORE_IDS = new InFlightLoadMoreIds();

  private final Lazy<ReplyRepository> replyRepository;
  private final Lazy<VotingManager> votingManager;
  private final Lazy<Markdown> markdown;
  private final Lazy<UserSessionRepository> userSessionRepository;

  /** Contribution IDs for which inline replies are active. */
  static class ActiveReplyIds extends RxHashSet<String> {
    public boolean isActive(Identifiable parentContribution) {
      return contains(keyFor(parentContribution));
    }

    public void showFor(Identifiable parentContribution) {
      add(keyFor(parentContribution));
    }

    public void hideFor(Identifiable parentContribution) {
      remove(keyFor(parentContribution));
    }
  }

  /** Comment IDs that are collapsed. */
  public static class CollapsedCommentIds extends RxHashSet<String> {
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

    public boolean isCollapsed(Identifiable comment) {
      return contains(keyFor(comment));
    }

    public void expand(Identifiable comment) {
      boolean removed = remove(keyFor(comment));
      if (!removed) {
        throw new AssertionError("This comment isn't collapsed: " + comment);
      }
    }

    public void collapse(Identifiable comment) {
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
      return contains(keyFor(commentNode.getSubject()));
    }

    public void showLoadMoreFor(Identifiable parentComment) {
      add(keyFor(parentComment));
    }

    public void hideLoadMoreFor(Identifiable parentComment) {
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

    public boolean hasForParent(PublicContribution parentComment) {
      return containsKey(parentComment.getFullName());
    }

    public List<PendingSyncReply> getForParent(PublicContribution parentComment) {
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
      Observable<SubmissionAndComments> submissionDatum,
      Observable<DankSubmissionRequest> submissionRequests,
      Scheduler scheduler)
  {
    Observable<PendingSyncRepliesMap> pendingSyncRepliesMaps = submissionDatum
        .distinctUntilChanged()
        .flatMap(submissionData -> replyRepository.get()
            .streamPendingSyncReplies(ParentThread.of(submissionData.getSubmission()))
            .subscribeOn(io())
        )
        // I've seen this stream very occasionally take a second to emit, so starting with a default value.
        .startWith(Collections.<PendingSyncReply>emptyList())
        .map(replyList -> createPendingSyncReplyMap(replyList));

    Observable<?> rowVisibilityChanges = Observable
        .merge(
            ACTIVE_REPLY_IDS.changes(),
            COLLAPSED_COMMENT_IDS.changes(),
            IN_FLIGHT_LOAD_MORE_IDS.changes()
        )
        //.observeOn(scheduler)   // observeOn() because the relays emit on the main thread)
        .startWith(0);          // Occasionally takes a while to emit something. I'm guessing the scheduler gets blocked.

    Observable<Optional<FocusedComment>> focusedComments = submissionRequests
        .map(submissionRequest -> Optional.ofNullable(submissionRequest.focusCommentId()))
        .startWith(Optional.empty())  // submissionRequests stream sometimes takes too long to emit anything.
        .map(optionalId -> optionalId.map(FocusedComment::create))
        .distinctUntilChanged();

    Observable<Object> voteChanges = votingManager.get().streamChanges();

    return CombineLatestWithLog
        .from(
            O.of("submission and root comments", submissionDatum),
            O.of("pendingSyncRepliesMap", pendingSyncRepliesMaps),
            O.of("focusedComment", focusedComments),
            O.of("row-visibility", rowVisibilityChanges),
            O.of("votes", voteChanges),
            (submissionData, pendingSyncRepliesMap, focusedComment, o, oo) -> new Triple<>(submissionData, pendingSyncRepliesMap, focusedComment))
        .observeOn(scheduler)
        .map(triple -> {
          SubmissionAndComments submissionData = triple.getFirst();
          PendingSyncRepliesMap pendingSyncRepliesMap = triple.getSecond();
          Optional<FocusedComment> focusedComment = triple.getThird();
          String submissionAuthor = submissionData.getSubmission().getAuthor();
          return constructComments(context, submissionData, pendingSyncRepliesMap, submissionAuthor, focusedComment);
        })
        .as(immutable());
  }

  private static String keyFor(Identifiable contribution) {
    // We're doing an exhaustive check here just to make sure
    // there's no unknown data model being passed.
    if (contribution instanceof Submission) {
      return contribution.getFullName();
    }
    if (contribution instanceof SimpleIdentifiable) {
      return contribution.getFullName();
    }
    if (contribution instanceof LocallyPostedComment) {
      String key = ((LocallyPostedComment) contribution).getPostingStatusIndependentId();
      return checkNotNull(key, "LocallyPostedComment#getPostingStatusIndependentId()");
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
  void toggleCollapse(Identifiable comment) {
    if (COLLAPSED_COMMENT_IDS.isCollapsed(comment)) {
      COLLAPSED_COMMENT_IDS.expand(comment);
    } else {
      COLLAPSED_COMMENT_IDS.collapse(comment);
    }
  }

  boolean isCollapsed(Identifiable contribution) {
    return COLLAPSED_COMMENT_IDS.isCollapsed(contribution);
  }

  /**
   * Enable "loading more…" progress indicator.
   *
   * @param parentComment for which more child nodes are being fetched.
   */
  void setMoreCommentsLoading(Identifiable parentComment, boolean loading) {
    if (loading) {
      IN_FLIGHT_LOAD_MORE_IDS.showLoadMoreFor(parentComment);
    } else {
      IN_FLIGHT_LOAD_MORE_IDS.hideLoadMoreFor(parentComment);
    }
  }

  /**
   * Show reply field for a comment and also expand any hidden comments.
   */
  void showReplyAndExpandComments(Identifiable parentComment) {
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
  void showReply(Identifiable parentContribution) {
    ACTIVE_REPLY_IDS.showFor(parentContribution);
  }

  /**
   * Hide reply field for a comment.
   */
  void hideReply(Identifiable parentContribution) {
    ACTIVE_REPLY_IDS.hideFor(parentContribution);
  }

  boolean isReplyActiveFor(Identifiable contribution) {
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
      SubmissionAndComments submissionData,
      PendingSyncRepliesMap pendingSyncRepliesMap,
      String submissionAuthor,
      Optional<FocusedComment> focusedComment)
  {
    int totalRowsSize = 0;
    if (ACTIVE_REPLY_IDS.isActive(submissionData.getSubmission())) {
      totalRowsSize += 1;
    }

    totalRowsSize += submissionData
        .getComments()
        .map(node -> node.totalSize())
        .orElse(0);

    ArrayList<SubmissionScreenUiModel> flattenComments = new ArrayList<>(totalRowsSize);
    if (ACTIVE_REPLY_IDS.isActive(submissionData.getSubmission())) {
      String loggedInUserName = userSessionRepository.get().loggedInUserName();
      flattenComments.add(inlineReplyUiModel(context, submissionData.getSubmission(), submissionAuthor, loggedInUserName, 0));
    }

    if (submissionData.getComments().isEmpty()) {
      return flattenComments;
    } else {
      return constructComments(
          context,
          flattenComments,
          submissionData.getComments().get(),
          submissionData.getSubmission(),
          pendingSyncRepliesMap,
          submissionAuthor,
          focusedComment);
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

    boolean isCommentNodeCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(nextNode.getSubject());
    boolean isReplyActive = ACTIVE_REPLY_IDS.isActive(nextNode.getSubject());

    if (nextNode.getDepth() != 0) {
      //Timber.i("%s(%s) %s: %s", indentation, nextNode.getComment().getFullName(), nextNode.getComment().getAuthor(), nextNode.getComment().getBody());
      String commentFullName = nextNode.getSubject().getFullName();
      boolean isFocused = focusedComment.isPresent() && focusedComment.get().fullname().equals(commentFullName);
      flattenComments.add(syncedCommentUiModel(context, nextNode, isCommentNodeCollapsed, submissionAuthor, isFocused));
    }

    // Reply box.
    // Skip for root-node because we already added a reply for the submission in constructComments().
    boolean isSubmission = nextNode.getSubject() instanceof Submission;
    if (!isSubmission && isReplyActive && !isCommentNodeCollapsed) {
      String loggedInUserName = userSessionRepository.get().loggedInUserName();
      String parentCommentAuthor = nextNode.getSubject().getAuthor();
      //noinspection ConstantConditions
      flattenComments.add(inlineReplyUiModel(context, nextNode.getSubject(), parentCommentAuthor, loggedInUserName, nextNode.getDepth()));
    }

    // Pending-sync replies.
    if (!isCommentNodeCollapsed && pendingSyncRepliesMap.hasForParent(nextNode.getSubject())) {
      List<PendingSyncReply> pendingSyncReplies = pendingSyncRepliesMap.getForParent(nextNode.getSubject());
      for (int i = 0; i < pendingSyncReplies.size(); i++) {     // Intentionally avoiding thrashing Iterator objects.
        LocallyPostedComment locallyPostedComment = new LocallyPostedComment(pendingSyncReplies.get(i));
        boolean isReplyCollapsed = COLLAPSED_COMMENT_IDS.isCollapsed(locallyPostedComment);
        int depth = nextNode.getDepth() + 1;
        boolean isFocused = focusedComment.isPresent()
            && locallyPostedComment.isPosted()
            && focusedComment.get().fullname().equals(locallyPostedComment.getFullName());
        flattenComments.add(locallyPostedCommentUiModel(context, locallyPostedComment, isReplyCollapsed, depth, isFocused));
      }
    }

    // Next, the child comment tree.
    if (nextNode.getReplies().isEmpty()) {
      return flattenComments;

    } else {
      // Ignore collapsed children.
      if (!isCommentNodeCollapsed) {
        //noinspection unchecked
        List<CommentNode> childCommentsTree = nextNode.getReplies();
        for (int i = 0; i < childCommentsTree.size(); i++) {  // Intentionally avoiding thrashing Iterator objects.
          CommentNode node = childCommentsTree.get(i);
          constructComments(context, flattenComments, node, submission, pendingSyncRepliesMap, submissionAuthor, focusedComment);
        }

        if (nextNode.hasMoreChildren()) {
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

  private SubmissionRemoteComment.UiModel syncedCommentUiModel(
      Context context,
      CommentNode commentNode,
      boolean isCollapsed,
      String submissionAuthor,
      boolean isFocused)
  {
    Comment comment = (Comment) commentNode.getSubject();
    Optional<String> authorFlairText = comment.getAuthorFlairText() != null ? Optional.ofNullable(comment.getAuthorFlairText()) : Optional.empty();
    long createdTimeMillis = comment.getCreated().getTime();
    VoteDirection pendingOrDefaultVoteDirection = votingManager.get().getPendingOrDefaultVote(comment, comment.getVote());

    int commentScore = votingManager.get().getScoreAfterAdjustingPendingVote(comment);
    Optional<Integer> commentScoreIfNotHidden;
    if (comment.isScoreHidden()) {
      commentScoreIfNotHidden = Optional.empty();
    } else {
      commentScoreIfNotHidden = Optional.of(commentScore);
    }

    boolean isAuthorOP = comment.getAuthor().equalsIgnoreCase(submissionAuthor);

    // TODO: getTotalSize() is buggy. See: https://github.com/thatJavaNerd/JRAW/issues/189
    int childCommentsCount = commentNode.totalSize();

    CharSequence byline = constructCommentByline(
        context,
        comment.getAuthor(),
        authorFlairText,
        isAuthorOP,
        createdTimeMillis,
        pendingOrDefaultVoteDirection,
        commentScoreIfNotHidden,
        childCommentsCount,
        isCollapsed
    );

    CharSequence commentBody = isCollapsed
        ? markdown.get().stripMarkdown(comment)
        : markdown.get().parse(comment);

    @ColorRes int backgroundColorRes = isFocused
        ? R.color.submission_comment_background_focused
        : R.color.submission_comment_background;

    return SubmissionRemoteComment.UiModel.builder()
        .adapterId(JrawUtils2.generateAdapterId(commentNode.getSubject()))
        .bylineTextColor(color(context,
            isCollapsed
                ? R.color.submission_comment_body_collapsed
                : R.color.submission_comment_byline_default_color
        ))
        .bodyTextColor(color(context, isCollapsed
            ? R.color.submission_comment_body_collapsed
            : R.color.submission_comment_body_expanded
        ))
        .indentationDepth(commentNode.getDepth() - 1)  // TODO: Why are we subtracting 1 here?
        .bodyMaxLines(isCollapsed ? 1 : Integer.MAX_VALUE)
        .isCollapsed(isCollapsed)
        .backgroundColorRes(backgroundColorRes)
        .isFocused(isFocused)
        .comment(comment)
        .byline(byline, commentScore)
        .body(commentBody)
        .build();
  }

  /**
   * Reply posted by the logged in user that hasn't synced yet or whose actual comment hasn't been fetched yet.
   */
  private SubmissionScreenUiModel locallyPostedCommentUiModel(
      Context context,
      LocallyPostedComment locallyPostedComment,
      boolean isCollapsed,
      int depth,
      boolean isFocused)
  {
    PendingSyncReply pendingSyncReply = locallyPostedComment.getPendingSyncReply();
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
          VoteDirection.UP,
          Optional.of(commentScore),
          0,
          isCollapsed
      );

    } else {
      Truss bylineBuilder = new Truss();
      if (isCollapsed) {
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

    CharSequence processedMarkdown = markdown.get().parse(pendingSyncReply);
    CharSequence commentBody = isCollapsed
        ? processedMarkdown.toString()  // toString() strips all spans.
        : processedMarkdown;

    return new SubmissionLocalComment.UiModel(
        locallyPostedComment.getPostingStatusIndependentId().hashCode(),
        SpannableWithTextEquality.wrap(byline, commentScore),
        SpannableWithTextEquality.wrap(commentBody),
        color(context,
            isCollapsed
                ? R.color.submission_comment_body_collapsed
                : R.color.submission_comment_byline_default_color
        ),
        color(context, isCollapsed
            ? R.color.submission_comment_body_collapsed
            : R.color.submission_comment_body_expanded
        ),
        isCollapsed ? 1 : Integer.MAX_VALUE,
        depth - 1,    // TODO: Why are we subtracting 1 here?
        locallyPostedComment,
        isFocused
            ? R.color.submission_comment_background_focused
            : R.color.submission_comment_background,
        isCollapsed
    );
  }

  /**
   * @param optionalCommentScore Empty when the score is hidden.
   */
  private CharSequence constructCommentByline(
      Context context,
      String author,
      Optional<String> optionalAuthorFlairText,
      boolean isAuthorOP,
      long createdTimeMillis,
      VoteDirection voteDirection,
      Optional<Integer> optionalCommentScore,
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
        bylineBuilder.append(markdown.get().parseAuthorFlair(flair));
      });
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.pushSpan(new ForegroundColorSpan(color(context, Themes.voteColor(voteDirection))));

      String scoreText = optionalCommentScore
          .map(score -> context.getResources().getQuantityString(
              R.plurals.submission_comment_byline_item_score,
              score,
              Strings.abbreviateScore(score)))
          .orElse(context.getString(R.string.submission_comment_byline_score_hidden));
      bylineBuilder.append(scoreText);

      bylineBuilder.popSpan();
      bylineBuilder.append(context.getString(R.string.submission_comment_byline_item_separator));
      bylineBuilder.append(Dates.createTimestamp(context.getResources(), createdTimeMillis));
    }
    return bylineBuilder.build();
  }

  private SubmissionCommentInlineReply.UiModel inlineReplyUiModel(
      Context context,
      PublicContribution parentContribution,
      String parentContributionReply,
      String loggedInUserName,
      int indentationDepth)
  {
    long adapterId = JrawUtils2.generateAdapterId(parentContribution) + "_reply".hashCode();
    CharSequence authorHint = context.getResources().getString(R.string.submission_comment_reply_author_hint, loggedInUserName);
    return SubmissionCommentInlineReply.UiModel.create(
        adapterId,
        authorHint,
        parentContribution,
        parentContributionReply,
        indentationDepth
    );
  }

  /**
   * For loading more replies of a comment.
   */
  private SubmissionCommentsLoadMore.UiModel loadMoreUiModel(Context context, CommentNode parentCommentNode, boolean progressVisible) {
    boolean isThreadContinuation = parentCommentNode instanceof MoreChildren && ((MoreChildren) parentCommentNode).isThreadContinuation();

    boolean clickEnabled;
    String label;
    if (progressVisible) {
      label = context.getString(R.string.submission_loading_more_comments);
      clickEnabled = false;

    } else {
      //noinspection ConstantConditions
      label = isThreadContinuation
          ? context.getString(R.string.submission_continue_this_thread)
          : context.getString(R.string.submission_load_more_comments, parentCommentNode.getMoreChildren().getChildrenIds().size());
      clickEnabled = true;
    }

    long adapterId = JrawUtils2.generateAdapterId(parentCommentNode.getSubject()) + "_loadMore".hashCode();

    return SubmissionCommentsLoadMore.UiModel.builder()
        .adapterId(adapterId)
        .label(label)
        .iconRes(isThreadContinuation ? R.drawable.ic_arrow_forward_12dp : 0)
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
