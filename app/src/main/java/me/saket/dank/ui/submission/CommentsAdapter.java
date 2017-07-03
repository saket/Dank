package me.saket.dank.ui.submission;

import static me.saket.dank.utils.RxUtils.applySchedulersSingle;

import android.support.annotation.CheckResult;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.BehaviorRelay;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.VoteDirection;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.VotingManager;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.IndentedLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public class CommentsAdapter extends RecyclerViewArrayAdapter<SubmissionCommentRow, RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_USER_COMMENT = 100;
  private static final int VIEW_TYPE_LOAD_MORE = 101;
  private static final int VIEW_TYPE_REPLY = 102;
  private static final int VIEW_TYPE_PENDING_SYNC_REPLY = 103;

  private final BetterLinkMovementMethod linkMovementMethod;
  private final VotingManager votingManager;
  private final UserSession userSession;
  private final ReplyDraftStore replyDraftStore;
  private final CommentSwipeActionsProvider swipeActionsProvider;
  private final BehaviorRelay<CommentClickEvent> commentClickStream = BehaviorRelay.create();
  private final BehaviorRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClickStream = BehaviorRelay.create();
  private String submissionAuthor;
  private ReplyActionsListener replyActionsListener;

  public interface ReplyActionsListener {
    void onClickDiscardReply(CommentNode nodeBeingRepliedTo);

    void onClickEditReplyInFullscreenMode(CommentNode nodeBeingRepliedTo);

    void onClickSendReply(CommentNode parentCommentNode, String replyMessage);

    void onClickRetrySendingReply(CommentNode parentCommentNode, PendingSyncReply pendingSyncReply);
  }

  @AutoValue
  abstract static class CommentClickEvent {
    public abstract SubmissionCommentRow commentRow();

    public abstract View commentItemView();

    public static CommentClickEvent create(SubmissionCommentRow commentRow, View commentItemView) {
      return new AutoValue_CommentsAdapter_CommentClickEvent(commentRow, commentItemView);
    }
  }

  @AutoValue
  abstract static class LoadMoreCommentsClickEvent {
    /**
     * Node whose more comments have to be fetched.
     */
    abstract CommentNode parentCommentNode();

    /**
     * Clicked itemView.
     */
    abstract View loadMoreItemView();

    public static LoadMoreCommentsClickEvent create(CommentNode parentNode, View loadMoreItemView) {
      return new AutoValue_CommentsAdapter_LoadMoreCommentsClickEvent(parentNode, loadMoreItemView);
    }
  }

  public CommentsAdapter(BetterLinkMovementMethod commentsLinkMovementMethod, VotingManager votingManager, UserSession userSession,
      ReplyDraftStore replyDraftStore, CommentSwipeActionsProvider swipeActionsProvider)
  {
    this.linkMovementMethod = commentsLinkMovementMethod;
    this.votingManager = votingManager;
    this.userSession = userSession;
    this.replyDraftStore = replyDraftStore;
    this.swipeActionsProvider = swipeActionsProvider;
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<CommentClickEvent> streamCommentCollapseExpandEvents() {
    return commentClickStream;
  }

  @CheckResult
  public Observable<LoadMoreCommentsClickEvent> streamLoadMoreCommentsClicks() {
    return loadMoreCommentsClickStream;
  }

  /**
   * OP of a submission, highlighted in comments. This is being set manually instead of {@link Comment#getSubmissionAuthor()},
   * because that's always null. Not sure where I'm going wrong.
   */
  public void updateSubmissionAuthor(String submissionAuthor) {
    this.submissionAuthor = submissionAuthor;
  }

  public void setReplyActionsListener(ReplyActionsListener listener) {
    replyActionsListener = listener;
  }

  @Override
  public int getItemViewType(int position) {
    SubmissionCommentRow commentItem = getItem(position);
    switch (commentItem.type()) {
      case USER_COMMENT:
        return VIEW_TYPE_USER_COMMENT;

      case LOAD_MORE_COMMENTS:
        return VIEW_TYPE_LOAD_MORE;

      case REPLY:
        return VIEW_TYPE_REPLY;

      case PENDING_SYNC_REPLY:
        return VIEW_TYPE_PENDING_SYNC_REPLY;

      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    switch (viewType) {
      case VIEW_TYPE_USER_COMMENT:
        UserCommentViewHolder commentHolder = UserCommentViewHolder.create(inflater, parent, linkMovementMethod);
        commentHolder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider.getSwipeActionIconProvider());
        return commentHolder;

      case VIEW_TYPE_LOAD_MORE:
        return LoadMoreCommentViewHolder.create(inflater, parent);

      case VIEW_TYPE_REPLY:
        return InlineReplyViewHolder.create(inflater, parent);

      case VIEW_TYPE_PENDING_SYNC_REPLY:
        PendingSyncReplyViewHolder pendingReplyHolder = PendingSyncReplyViewHolder.create(inflater, parent, linkMovementMethod);
        pendingReplyHolder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider.getSwipeActionIconProvider());
        pendingReplyHolder.getSwipeableLayout().setSwipeEnabled(false);
        return pendingReplyHolder;

      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    SubmissionCommentRow commentItem = getItem(position);

    switch (commentItem.type()) {
      case USER_COMMENT:
        DankCommentNode dankCommentNode = (DankCommentNode) commentItem;
        CommentNode commentNode = dankCommentNode.commentNode();
        Comment comment = commentNode.getComment();

        VoteDirection pendingOrDefaultVoteDirection = votingManager.getPendingOrDefaultVote(comment, comment.getVote());
        int commentScore = votingManager.getScoreAfterAdjustingPendingVote(comment);
        boolean isAuthorOP = commentNode.getComment().getAuthor().equalsIgnoreCase(submissionAuthor);

        UserCommentViewHolder commentViewHolder = (UserCommentViewHolder) holder;
        commentViewHolder.bind(dankCommentNode, pendingOrDefaultVoteDirection, commentScore, isAuthorOP);

        // Collapse on click.
        commentViewHolder.itemView.setOnClickListener(v -> {
          commentClickStream.accept(CommentClickEvent.create(commentItem, commentViewHolder.itemView));
        });

        // Gestures.
        SwipeableLayout swipeableLayout = commentViewHolder.getSwipeableLayout();
        swipeableLayout.setSwipeActions(swipeActionsProvider.getSwipeActions());
        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          swipeActionsProvider.performSwipeAction(action, commentNode, swipeableLayout);

          // We should ideally only be updating the backing data-set and let onBind() handle the
          // changes, but RecyclerView's item animator reset's the View's x-translation which we
          // don't want. So we manually update the Views here.
          onBindViewHolder(holder, holder.getAdapterPosition() - 1 /* -1 for parent adapter's offset for header item. */);
        });
        break;

      case LOAD_MORE_COMMENTS:
        LoadMoreCommentItem loadMoreItem = ((LoadMoreCommentItem) commentItem);
        ((LoadMoreCommentViewHolder) holder).bind(loadMoreItem);

        holder.itemView.setOnClickListener(__ -> {
          loadMoreCommentsClickStream.accept(LoadMoreCommentsClickEvent.create(loadMoreItem.parentCommentNode(), holder.itemView));
        });
        break;

      case REPLY:
        CommentInlineReplyItem commentInlineReplyItem = (CommentInlineReplyItem) commentItem;
        ((InlineReplyViewHolder) holder).bind(commentInlineReplyItem, replyActionsListener, userSession, replyDraftStore);
        break;

      case PENDING_SYNC_REPLY:
        CommentPendingSyncReplyItem commentPendingSyncReplyItem = (CommentPendingSyncReplyItem) commentItem;
        PendingSyncReplyViewHolder pendingSyncReplyViewHolder = (PendingSyncReplyViewHolder) holder;

        // Collapse on click.
        pendingSyncReplyViewHolder.bind(commentPendingSyncReplyItem);
        pendingSyncReplyViewHolder.itemView.setOnClickListener(v -> {
          PendingSyncReply pendingSyncReply = commentPendingSyncReplyItem.pendingSyncReply();
          if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
            // "Tap to retry".
            replyActionsListener.onClickRetrySendingReply(commentPendingSyncReplyItem.parentCommentNode(), pendingSyncReply);
          } else {
            commentClickStream.accept(CommentClickEvent.create(commentItem, pendingSyncReplyViewHolder.itemView));
          }
        });
        break;

      default:
        throw new UnsupportedOperationException();
    }
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).fullName().hashCode();
  }

  @Override
  public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof InlineReplyViewHolder) {
      ((InlineReplyViewHolder) holder).handleOnRecycle(replyDraftStore);
    }
    super.onViewRecycled(holder);
  }

  public static class UserCommentViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    @BindView(R.id.item_comment_indented_container) IndentedLayout indentedContainer;
    @BindView(R.id.item_comment_byline) TextView bylineView;
    @BindView(R.id.item_comment_body) TextView commentBodyView;
    @BindView(R.id.item_comment_separator) View separatorView;

    @BindColor(R.color.submission_comment_byline_author) int bylineAuthorNameColor;
    @BindColor(R.color.submission_comment_byline_author_op) int bylineAuthorNameColorForOP;
    @BindColor(R.color.submission_comment_body_expanded) int expandedBodyColor;
    @BindColor(R.color.submission_comment_body_collapsed) int collapsedBodyColor;
    @BindColor(R.color.submission_comment_byline_default_color) int bylineDefaultColor;
    @BindString(R.string.submission_comment_byline_item_separator) String bylineItemSeparator;
    @BindString(R.string.submission_comment_byline_item_score) String bylineItemScoreString;

    private BetterLinkMovementMethod linkMovementMethod;
    private boolean isCollapsed;

    public static UserCommentViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
      return new UserCommentViewHolder(inflater.inflate(R.layout.list_item_comment, parent, false), linkMovementMethod);
    }

    public UserCommentViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod) {
      super(itemView);
      ButterKnife.bind(this, itemView);
      this.linkMovementMethod = linkMovementMethod;

      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      commentBodyView.setOnTouchListener((__, event) -> {
        if (isCollapsed) {
          return false;
        }
        boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(commentBodyView, (Spannable) commentBodyView.getText(), event);
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
    }

    public void bind(CharSequence byline, String bodyHtml, int commentNodeDepth, boolean isCollapsed) {
      indentedContainer.setIndentationDepth(commentNodeDepth - 1);    // TODO: Why are we subtracting 1 here?
      this.isCollapsed = isCollapsed;

      bylineView.setTextColor(isCollapsed ? collapsedBodyColor : bylineDefaultColor);
      bylineView.setText(byline);

      // Body.
      if (isCollapsed) {
        commentBodyView.setText(Markdown.stripMarkdown(bodyHtml));
        commentBodyView.setMovementMethod(null);
      } else {
        commentBodyView.setText(Markdown.parseRedditMarkdownHtml(bodyHtml, commentBodyView.getPaint()));
        commentBodyView.setMovementMethod(linkMovementMethod);
      }
      commentBodyView.setMaxLines(isCollapsed ? 1 : Integer.MAX_VALUE);
      commentBodyView.setEllipsize(isCollapsed ? TextUtils.TruncateAt.END : null);
      commentBodyView.setTextColor(isCollapsed ? collapsedBodyColor : expandedBodyColor);
    }

    protected CharSequence constructCommentByline(String author, String authorFlairText, boolean isAuthorOP, long createdTimeMillis,
        VoteDirection voteDirection, int commentScore, int childCommentsCount, boolean isCollapsed)
    {
      Truss bylineBuilder = new Truss();
      if (isCollapsed) {
        bylineBuilder.append(author);
        bylineBuilder.append(bylineItemSeparator);

        int hiddenCommentsCount = childCommentsCount + 1;   // +1 for the parent comment itself.
        String hiddenCommentsString = itemView.getResources().getQuantityString(R.plurals.submission_comment_hidden_comments, hiddenCommentsCount);
        bylineBuilder.append(String.format(hiddenCommentsString, hiddenCommentsCount));

      } else {
        bylineBuilder.pushSpan(new ForegroundColorSpan(isAuthorOP ? bylineAuthorNameColorForOP : bylineAuthorNameColor));
        bylineBuilder.append(author);
        bylineBuilder.popSpan();
        if (authorFlairText != null) {
          bylineBuilder.append(bylineItemSeparator);
          bylineBuilder.append(authorFlairText);
        }
        bylineBuilder.append(bylineItemSeparator);
        bylineBuilder.pushSpan(new ForegroundColorSpan(ContextCompat.getColor(itemView.getContext(), Commons.voteColor(voteDirection))));
        bylineBuilder.append(String.format(bylineItemScoreString, Strings.abbreviateScore(commentScore)));
        bylineBuilder.popSpan();
        bylineBuilder.append(bylineItemSeparator);
        bylineBuilder.append(Dates.createTimestamp(itemView.getResources(), createdTimeMillis));
      }
      return bylineBuilder.build();
    }

    public void bind(DankCommentNode dankCommentNode, VoteDirection voteDirection, int commentScore, boolean isAuthorOP) {
      Comment comment = dankCommentNode.commentNode().getComment();
      String authorFlairText = comment.getAuthorFlair() != null ? comment.getAuthorFlair().getText() : null;
      long createdTimeMillis = JrawUtils.createdTimeUtc(comment);
      String commentBodyHtml = comment.getDataNode().get("body_html").asText();

      // TODO: getTotalSize() is buggy. See: https://github.com/thatJavaNerd/JRAW/issues/189
      int childCommentsCount = dankCommentNode.commentNode().getTotalSize();

      CharSequence byline = constructCommentByline(
          comment.getAuthor(),
          authorFlairText,
          isAuthorOP,
          createdTimeMillis,
          voteDirection,
          commentScore,
          childCommentsCount,
          dankCommentNode.isCollapsed()
      );
      bind(byline, commentBodyHtml, dankCommentNode.commentNode().getDepth(), dankCommentNode.isCollapsed());
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  // TODO: Show post indicator.
  public static class PendingSyncReplyViewHolder extends UserCommentViewHolder {
    @BindColor(R.color.submission_comment_byline_failed_to_post) int bylineCommentPostErrorColor;

    public static PendingSyncReplyViewHolder create(LayoutInflater inflater, ViewGroup parent, BetterLinkMovementMethod linkMovementMethod) {
      return new PendingSyncReplyViewHolder(inflater.inflate(R.layout.list_item_comment, parent, false), linkMovementMethod);
    }

    public PendingSyncReplyViewHolder(View itemView, BetterLinkMovementMethod linkMovementMethod) {
      super(itemView, linkMovementMethod);
      ButterKnife.bind(this, itemView);
    }

    public void bind(CommentPendingSyncReplyItem commentPendingSyncReplyItem) {
      PendingSyncReply pendingSyncReply = commentPendingSyncReplyItem.pendingSyncReply();
      CharSequence byline;

      if (pendingSyncReply.state() == PendingSyncReply.State.POSTED) {
        byline = constructCommentByline(
            pendingSyncReply.author(),
            null /* authorFlairText */,
            true /* isAuthorOP */,
            pendingSyncReply.createdTimeMillis(),
            VoteDirection.UPVOTE /* voteDirection */,
            1 /* commentScore */,
            0 /* childCommentsCount */,
            commentPendingSyncReplyItem.isCollapsed()
        );

      } else {
        Truss bylineBuilder = new Truss();
        if (commentPendingSyncReplyItem.isCollapsed()) {
          bylineBuilder.append(pendingSyncReply.author());
        } else {
          bylineBuilder.pushSpan(new ForegroundColorSpan(bylineAuthorNameColorForOP));
          bylineBuilder.append(pendingSyncReply.author());
          bylineBuilder.popSpan();
        }
        bylineBuilder.append(bylineItemSeparator);

        if (pendingSyncReply.state() == PendingSyncReply.State.POSTING) {
          bylineBuilder.append(itemView.getResources().getString(R.string.submission_comment_reply_byline_posting_status));
        } else if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
          bylineBuilder.pushSpan(new ForegroundColorSpan(bylineCommentPostErrorColor));
          bylineBuilder.append(itemView.getResources().getString(R.string.submission_comment_reply_byline_failed_status));
          bylineBuilder.popSpan();
        } else {
          throw new AssertionError();
        }

        byline = bylineBuilder.build();
      }

      int replyDepth = commentPendingSyncReplyItem.parentCommentNode().getDepth() + 1;
      bind(byline, pendingSyncReply.body(), replyDepth, commentPendingSyncReplyItem.isCollapsed());
    }
  }

  public static class InlineReplyViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_comment_reply_indented_container) IndentedLayout indentedLayout;
    @BindView(R.id.item_comment_reply_discard) ImageButton discardButton;
    @BindView(R.id.item_comment_reply_author_hint) TextView authorUsernameHintView;
    @BindView(R.id.item_comment_reply_go_fullscreen) ImageButton goFullscreenButton;
    @BindView(R.id.item_comment_reply_send) ImageButton sendButton;
    @BindView(R.id.item_comment_reply_message) EditText replyMessageField;

    private Disposable draftDisposable = Disposables.disposed();
    private CommentNode parentCommentNode;

    public static InlineReplyViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new InlineReplyViewHolder(inflater.inflate(R.layout.list_item_comment_reply, parent, false));
    }

    public InlineReplyViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(CommentInlineReplyItem commentInlineReplyItem, ReplyActionsListener replyActionsListener, UserSession userSession,
        ReplyDraftStore replyDraftStore)
    {
      parentCommentNode = commentInlineReplyItem.parentCommentNode();
      indentedLayout.setIndentationDepth(parentCommentNode.getDepth());
      authorUsernameHintView.setText(authorUsernameHintView.getResources().getString(
          R.string.submission_comment_reply_author_hint,
          userSession.loggedInUserName()
      ));

      discardButton.setOnClickListener(o -> replyActionsListener.onClickDiscardReply(parentCommentNode));
      goFullscreenButton.setOnClickListener(o -> replyActionsListener.onClickEditReplyInFullscreenMode(parentCommentNode));
      sendButton.setOnClickListener(o -> replyActionsListener.onClickSendReply(parentCommentNode, replyMessageField.getText().toString()));

      draftDisposable = replyDraftStore.getDraft(parentCommentNode.getComment())
          .compose(applySchedulersSingle())
          .subscribe(replyDraft -> {
            replyMessageField.setText(replyDraft);
            if (replyDraft != null) {
              replyMessageField.setSelection(replyDraft.length());
            }
          });
    }

    public void handleOnRecycle(ReplyDraftStore replyDraftStore) {
      // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
      replyDraftStore
          .saveDraft(parentCommentNode.getComment(), replyMessageField.getText().toString())
          .subscribeOn(Schedulers.io())
          .subscribe();

      draftDisposable.dispose();
      parentCommentNode = null;
    }
  }

  public static class LoadMoreCommentViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_loadmorecomments_load_more) TextView loadMoreView;
    @BindView(R.id.item_loadmorecomments_indented_container) IndentedLayout indentedContainer;

    public static LoadMoreCommentViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new LoadMoreCommentViewHolder(inflater.inflate(R.layout.list_item_comment_load_more, parent, false));
    }

    public LoadMoreCommentViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(LoadMoreCommentItem loadMoreCommentsItem) {
      CommentNode parentCommentNode = loadMoreCommentsItem.parentCommentNode();

      indentedContainer.setIndentationDepth(parentCommentNode.getDepth());

      if (loadMoreCommentsItem.progressVisible()) {
        loadMoreView.setText(R.string.submission_loading_more_comments);
      } else {
        if (parentCommentNode.isThreadContinuation()) {
          loadMoreView.setText(R.string.submission_continue_this_thread);
        } else {
          loadMoreView.setText(itemView.getResources().getString(
              R.string.submission_load_more_comments,
              parentCommentNode.getMoreChildren().getCount()
          ));
        }
      }
      Views.setCompoundDrawableEnd(loadMoreView, parentCommentNode.isThreadContinuation() ? R.drawable.ic_arrow_forward_12dp : 0);
    }
  }
}
