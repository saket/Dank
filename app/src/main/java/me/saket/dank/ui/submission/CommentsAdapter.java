package me.saket.dank.ui.submission;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.support.annotation.CheckResult;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.CommentNode;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.VotingManager;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownHints;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.submission.events.CommentClickEvent;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.ui.submission.events.ReplyDiscardClickEvent;
import me.saket.dank.ui.submission.events.ReplyFullscreenClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.ui.user.UserSession;
import me.saket.dank.utils.Commons;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Dates;
import me.saket.dank.utils.JrawUtils;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Markdown;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.SimpleTextWatcher;
import me.saket.dank.utils.Strings;
import me.saket.dank.utils.Truss;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.IndentedLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public class CommentsAdapter extends RecyclerViewArrayAdapter<SubmissionCommentRow, RecyclerView.ViewHolder> {

  private static final int VIEW_TYPE_USER_COMMENT = 100;
  private static final int VIEW_TYPE_LOAD_MORE_COMMENTS = 101;
  private static final int VIEW_TYPE_REPLY = 102;
  private static final int VIEW_TYPE_PENDING_SYNC_REPLY = 103;

  // Injected by Dagger.
  private final DankLinkMovementMethod linkMovementMethod;
  private final VotingManager votingManager;
  private final UserSession userSession;
  private final DraftStore draftStore;
  private final MarkdownHintOptions markdownHintOptions;
  private final MarkdownSpanPool markdownSpanPool;
  private final BehaviorRelay<CommentClickEvent> commentClickStream = BehaviorRelay.create();
  private final BehaviorRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClickStream = BehaviorRelay.create();

  // Manually set by SubmissionFragment.
  private CommentSwipeActionsProvider swipeActionsProvider;
  private String submissionAuthor;

  // Click event streams.
  private final Relay<ReplyItemViewBindEvent> replyViewBindStream = PublishRelay.create();
  private final Relay<ReplyInsertGifClickEvent> replyGifClickStream = PublishRelay.create();
  private final Relay<ReplyDiscardClickEvent> replyDiscardClickStream = PublishRelay.create();
  private final Relay<ReplySendClickEvent> replySendClickStream = PublishRelay.create();
  private final Relay<ReplyRetrySendClickEvent> replyRetrySendClickStream = PublishRelay.create();
  private final Relay<ReplyFullscreenClickEvent> replyFullscreenClickStream = PublishRelay.create();

  private CompositeDisposable inlineReplyDraftsDisposables = new CompositeDisposable();

  @Inject
  public CommentsAdapter(DankLinkMovementMethod commentsLinkMovementMethod, VotingManager votingManager, UserSession userSession,
      DraftStore draftStore, MarkdownHintOptions markdownHintOptions, MarkdownSpanPool markdownSpanPool)
  {
    this.linkMovementMethod = commentsLinkMovementMethod;
    this.votingManager = votingManager;
    this.userSession = userSession;
    this.draftStore = draftStore;
    this.markdownHintOptions = markdownHintOptions;
    this.markdownSpanPool = markdownSpanPool;
    setHasStableIds(true);
  }

  /**
   * OP of a submission, highlighted in comments. This is being set manually instead of
   * {@link Comment#getSubmissionAuthor()}, because that's always null. Not sure where I'm going wrong.
   */
  public void setSubmissionAuthor(String author) {
    submissionAuthor = author;
  }

  public void setSwipeActionsProvider(CommentSwipeActionsProvider provider) {
    swipeActionsProvider = provider;
  }

  /**
   * We try to automatically dispose drafts subscribers in {@link InlineReplyViewHolder} when the
   * ViewHolder is getting recycled (in {@link #onViewRecycled(RecyclerView.ViewHolder)} or re-bound
   * to data. But onViewRecycled() doesn't get called on Activity destroy so this has to be manually
   * called.
   */
  public void forceDisposeDraftSubscribers() {
    inlineReplyDraftsDisposables.clear();
  }

  @CheckResult
  public Observable<CommentClickEvent> streamCommentCollapseExpandEvents() {
    return commentClickStream;
  }

  @CheckResult
  public Observable<LoadMoreCommentsClickEvent> streamLoadMoreCommentsClicks() {
    return loadMoreCommentsClickStream;
  }

  @CheckResult
  public Observable<ReplyDiscardClickEvent> streamReplyDiscardClicks() {
    return replyDiscardClickStream;
  }

  @CheckResult
  public Observable<ReplySendClickEvent> streamReplySendClicks() {
    return replySendClickStream;
  }

  @CheckResult
  public Observable<ReplyRetrySendClickEvent> streamReplyRetrySendClicks() {
    return replyRetrySendClickStream;
  }

  @CheckResult
  public Observable<ReplyInsertGifClickEvent> streamReplyGifClicks() {
    return replyGifClickStream;
  }

  @CheckResult
  public Observable<ReplyFullscreenClickEvent> streamReplyFullscreenClicks() {
    return replyFullscreenClickStream;
  }

  @CheckResult
  public Relay<ReplyItemViewBindEvent> streamReplyItemViewBinds() {
    return replyViewBindStream;
  }

  @Override
  public int getItemViewType(int position) {
    SubmissionCommentRow commentItem = getItem(position);
    switch (commentItem.type()) {
      case USER_COMMENT:
        return VIEW_TYPE_USER_COMMENT;

      case LOAD_MORE_COMMENTS:
        return VIEW_TYPE_LOAD_MORE_COMMENTS;

      case INLINE_REPLY:
        return VIEW_TYPE_REPLY;

      case PENDING_SYNC_REPLY:
        return VIEW_TYPE_PENDING_SYNC_REPLY;

      default:
        throw new UnsupportedOperationException();
    }
  }

  public SubmissionCommentRow getItemWithHeaderOffset(int position) {
    return getItem(position - SubmissionAdapterWithHeader.HEADER_COUNT);
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    switch (viewType) {
      case VIEW_TYPE_USER_COMMENT:
        UserCommentViewHolder commentHolder = UserCommentViewHolder.create(inflater, parent, linkMovementMethod);
        commentHolder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider.getSwipeActionIconProvider());

        // Collapse on click.
        commentHolder.itemView.setOnClickListener(v -> {
          DankCommentNode dankCommentNode = (DankCommentNode) getItemWithHeaderOffset(commentHolder.getAdapterPosition());
          boolean willCollapse = !dankCommentNode.isCollapsed();
          CommentClickEvent event = CommentClickEvent.create(
              dankCommentNode,
              commentHolder.getAdapterPosition() - SubmissionAdapterWithHeader.HEADER_COUNT,
              commentHolder.itemView,
              willCollapse
          );
          commentClickStream.accept(event);
        });

        // Gestures.
        SwipeableLayout swipeableLayout = commentHolder.getSwipeableLayout();
        swipeableLayout.setSwipeActions(swipeActionsProvider.getSwipeActions());
        swipeableLayout.setOnPerformSwipeActionListener(action -> {
          DankCommentNode dankCommentNode = (DankCommentNode) getItemWithHeaderOffset(commentHolder.getAdapterPosition());
          CommentNode commentNode = dankCommentNode.commentNode();
          swipeActionsProvider.performSwipeAction(action, commentNode, swipeableLayout);

          // We should ideally only be updating the backing data-set and let onBind() handle the
          // changes, but RecyclerView's item animator reset's the View's x-translation which we
          // don't want. So we manually update the Views here.
          onBindViewHolder(commentHolder, commentHolder.getAdapterPosition() - SubmissionAdapterWithHeader.HEADER_COUNT);
        });
        return commentHolder;

      case VIEW_TYPE_LOAD_MORE_COMMENTS:
        LoadMoreCommentViewHolder loadMoreViewHolder = LoadMoreCommentViewHolder.create(inflater, parent);
        loadMoreViewHolder.itemView.setOnClickListener(o -> {
          if (loadMoreViewHolder.getAdapterPosition() == -1) {
            // Is being removed.
            return;
          }
          LoadMoreCommentItem loadMoreCommentItem = (LoadMoreCommentItem) getItemWithHeaderOffset(loadMoreViewHolder.getAdapterPosition());
          loadMoreCommentsClickStream.accept(LoadMoreCommentsClickEvent.create(loadMoreCommentItem.parentCommentNode(), loadMoreViewHolder.itemView));
        });
        return loadMoreViewHolder;

      case VIEW_TYPE_REPLY:
        return InlineReplyViewHolder.create(inflater, parent, markdownHintOptions, markdownSpanPool);

      case VIEW_TYPE_PENDING_SYNC_REPLY:
        PendingSyncReplyViewHolder pendingReplyHolder = PendingSyncReplyViewHolder.create(inflater, parent, linkMovementMethod);
        pendingReplyHolder.getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider.getSwipeActionIconProvider());
        pendingReplyHolder.getSwipeableLayout().setSwipeEnabled(false);

        pendingReplyHolder.itemView.setOnClickListener(v -> {
          CommentPendingSyncReplyItem pendingSyncReplyItem = (CommentPendingSyncReplyItem) getItemWithHeaderOffset(pendingReplyHolder.getAdapterPosition());
          PendingSyncReply pendingSyncReply = pendingSyncReplyItem.pendingSyncReply();
          if (pendingSyncReply.state() == PendingSyncReply.State.FAILED) {
            // "Tap to retry".
            replyRetrySendClickStream.accept(ReplyRetrySendClickEvent.create(pendingSyncReply));

          } else {
            // Collapse on click.
            boolean willCollapse = !pendingSyncReplyItem.isCollapsed();
            commentClickStream.accept(CommentClickEvent.create(
                getItemWithHeaderOffset(pendingReplyHolder.getAdapterPosition()),
                pendingReplyHolder.getAdapterPosition() - SubmissionAdapterWithHeader.HEADER_COUNT,
                pendingReplyHolder.itemView,
                willCollapse
            ));
          }
        });

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
        break;

      case LOAD_MORE_COMMENTS:
        LoadMoreCommentItem loadMoreItem = ((LoadMoreCommentItem) commentItem);
        ((LoadMoreCommentViewHolder) holder).bind(loadMoreItem);
        break;

      case INLINE_REPLY:
        CommentInlineReplyItem commentInlineReplyItem = (CommentInlineReplyItem) commentItem;
        Disposable draftsDisposable = ((InlineReplyViewHolder) holder).bind(
            commentInlineReplyItem,
            userSession,
            draftStore,
            replyGifClickStream,
            replyDiscardClickStream,
            replyFullscreenClickStream,
            replySendClickStream
        );
        inlineReplyDraftsDisposables.add(draftsDisposable);
        replyViewBindStream.accept(ReplyItemViewBindEvent.create(commentInlineReplyItem, ((InlineReplyViewHolder) holder).replyField));
        break;

      case PENDING_SYNC_REPLY:
        CommentPendingSyncReplyItem commentPendingSyncReplyItem = (CommentPendingSyncReplyItem) commentItem;
        PendingSyncReplyViewHolder pendingSyncReplyViewHolder = (PendingSyncReplyViewHolder) holder;
        pendingSyncReplyViewHolder.bind(commentPendingSyncReplyItem);
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
      ((InlineReplyViewHolder) holder).handleOnRecycle();
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

    protected void bind(CharSequence byline, String bodyHtml, int commentNodeDepth, boolean isCollapsed) {
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

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

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

      int replyDepth = commentPendingSyncReplyItem.depth();
      bind(byline, pendingSyncReply.body(), replyDepth, commentPendingSyncReplyItem.isCollapsed());
    }
  }

  public static class InlineReplyViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_comment_reply_indented_container) IndentedLayout indentedLayout;
    @BindView(R.id.item_comment_reply_discard) ImageButton discardButton;
    @BindView(R.id.item_comment_reply_author_hint) TextView authorUsernameHintView;
    @BindView(R.id.item_comment_reply_insert_gif) ImageButton gifButton;
    @BindView(R.id.item_comment_reply_go_fullscreen) ImageButton goFullscreenButton;
    @BindView(R.id.item_comment_reply_send) ImageButton sendButton;
    @BindView(R.id.item_comment_reply_message) EditText replyField;

    private Disposable draftDisposable = Disposables.disposed();
    private boolean savingDraftsAllowed;

    public static InlineReplyViewHolder create(LayoutInflater inflater, ViewGroup parent, MarkdownHintOptions markdownHintOptions,
        MarkdownSpanPool markdownSpanPool)
    {
      View itemView = inflater.inflate(R.layout.list_item_inline_comment_reply, parent, false);
      return new InlineReplyViewHolder(itemView, markdownHintOptions, markdownSpanPool);
    }

    public InlineReplyViewHolder(View itemView, MarkdownHintOptions markdownHintOptions, MarkdownSpanPool markdownSpanPool) {
      super(itemView);
      ButterKnife.bind(this, itemView);

      sendButton.setEnabled(false);
      replyField.addTextChangedListener(new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable text) {
          boolean hasValidReply = text.toString().trim().length() > 0;
          sendButton.setEnabled(hasValidReply);
        }
      });

      // Highlight markdown syntax.
      // Note: We'll have to remove MarkdownHintOptions from Dagger graph when we introduce a light theme.
      replyField.addTextChangedListener(new MarkdownHints(replyField, markdownHintOptions, markdownSpanPool));
    }

    /**
     * @return For disposing drafts subscriber.
     */
    @CheckResult
    public Disposable bind(CommentInlineReplyItem commentInlineReplyItem, UserSession userSession, DraftStore draftStore,
        Relay<ReplyInsertGifClickEvent> replyGifClickRelay, Relay<ReplyDiscardClickEvent> replyDiscardEventRelay,
        Relay<ReplyFullscreenClickEvent> replyFullscreenClickRelay, Relay<ReplySendClickEvent> replySendClickRelay)
    {
      PublicContribution parentContribution = commentInlineReplyItem.parentContribution();
      indentedLayout.setIndentationDepth(commentInlineReplyItem.depth());
      authorUsernameHintView.setText(authorUsernameHintView.getResources().getString(
          R.string.submission_comment_reply_author_hint,
          userSession.loggedInUserName()
      ));

      discardButton.setOnClickListener(o ->
          replyDiscardEventRelay.accept(ReplyDiscardClickEvent.create(parentContribution))
      );

      gifButton.setOnClickListener(o ->
          replyGifClickRelay.accept(ReplyInsertGifClickEvent.create(getItemId()))
      );

      goFullscreenButton.setOnClickListener(o -> {
        String replyMessage = replyField.getText().toString().trim();
        String authorNameIfComment = parentContribution instanceof Comment ? ((Comment) parentContribution).getAuthor() : null;
        replyFullscreenClickRelay.accept(ReplyFullscreenClickEvent.create(getItemId(), parentContribution, replyMessage, authorNameIfComment));
      });

      savingDraftsAllowed = true;
      sendButton.setOnClickListener(o -> {
        savingDraftsAllowed = false;
        String replyMessage = replyField.getText().toString().trim();
        replySendClickRelay.accept(ReplySendClickEvent.create(parentContribution, replyMessage));
      });

      replyField.setOnFocusChangeListener((v, hasFocus) -> {
        if (!hasFocus && savingDraftsAllowed) {
          // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
          draftStore.saveDraft(parentContribution, replyField.getText().toString())
              .subscribeOn(Schedulers.io())
              .subscribe();
        }
      });

      draftDisposable.dispose();
      draftDisposable = draftStore.streamDrafts(parentContribution)
          .subscribeOn(io())
          .observeOn(mainThread())
          .subscribe(replyDraft -> {
            boolean isReplyCurrentlyEmpty = replyField.getText().length() == 0;

            // Using replace() instead of setText() to preserve cursor position.
            replyField.getText().replace(0, replyField.getText().length(), replyDraft);

            // Avoid moving the cursor around unless the text was empty.
            if (isReplyCurrentlyEmpty) {
              replyField.setSelection(replyDraft.length());
            }
          });

      return draftDisposable;
    }

    public void disableSavingOfDraft() {
      savingDraftsAllowed = false;
    }

    public void handleOnRecycle() {
      draftDisposable.dispose();
    }

    public void handlePickedGiphyGif(GiphyGif giphyGif) {
      int selectionStart = Math.min(replyField.getSelectionStart(), replyField.getSelectionEnd());
      int selectionEnd = Math.max(replyField.getSelectionStart(), replyField.getSelectionEnd());

      String selectedText = replyField.getText().subSequence(selectionStart, selectionEnd).toString();
      String linkMarkdown = selectedText.isEmpty()
          ? giphyGif.url()
          : String.format("[%s](%s)", selectedText, giphyGif.url());
      replyField.getText().replace(selectionStart, selectionEnd, linkMarkdown);

      // Keyboard might have gotten dismissed while the GIF list was being scrolled.
      // Works only if called delayed. Posting to reply field's message queue works.
      replyField.post(() -> Keyboards.show(replyField));
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
        loadMoreView.setEnabled(false);

      } else {
        if (parentCommentNode.isThreadContinuation()) {
          loadMoreView.setText(R.string.submission_continue_this_thread);
        } else {
          loadMoreView.setText(itemView.getResources().getString(
              R.string.submission_load_more_comments,
              parentCommentNode.getMoreChildren().getCount()
          ));
        }
        loadMoreView.setEnabled(true);
      }
      Views.setCompoundDrawableEnd(loadMoreView, parentCommentNode.isThreadContinuation() ? R.drawable.ic_arrow_forward_12dp : 0);
    }
  }
}
