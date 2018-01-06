package me.saket.dank.ui.submission.adapter;

import android.support.annotation.CheckResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import me.saket.dank.ui.submission.CommentSwipeActionsProvider;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.events.CommentClickEvent;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.ui.submission.events.ReplyDiscardClickEvent;
import me.saket.dank.ui.submission.events.ReplyFullscreenClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.ui.subreddits.SubmissionSwipeActionsProvider;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class SubmissionCommentsAdapter extends RecyclerViewArrayAdapter<SubmissionScreenUiModel, RecyclerView.ViewHolder> {

  private static final SubmissionCommentRowType[] VIEW_TYPES = SubmissionCommentRowType.values();

  private final DankLinkMovementMethod linkMovementMethod;
  private final SubmissionSwipeActionsProvider submissionSwipeActionsProvider;
  private final CommentSwipeActionsProvider commentSwipeActionsProvider;
  private final DraftStore draftStore;
  private final CompositeDisposable inlineReplyDraftsDisposables = new CompositeDisposable();

  // Click event streams.
  private final PublishRelay<CommentClickEvent> commentClickStream = PublishRelay.create();
  private final PublishRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClickStream = PublishRelay.create();
  private final Relay<ReplyItemViewBindEvent> replyViewBindStream = PublishRelay.create();
  private final Relay<ReplyInsertGifClickEvent> replyGifClickStream = PublishRelay.create();
  private final Relay<ReplyDiscardClickEvent> replyDiscardClickStream = PublishRelay.create();
  private final Relay<ReplySendClickEvent> replySendClickStream = PublishRelay.create();
  private final Relay<ReplyRetrySendClickEvent> replyRetrySendClickStream = PublishRelay.create();
  private final Relay<ReplyFullscreenClickEvent> replyFullscreenClickStream = PublishRelay.create();
  private final Relay<Object> headerClickStream = PublishRelay.create();

  @Inject
  public SubmissionCommentsAdapter(
      DankLinkMovementMethod linkMovementMethod,
      SubmissionSwipeActionsProvider submissionSwipeActionsProvider,
      CommentSwipeActionsProvider commentSwipeActionsProvider,
      DraftStore draftStore)
  {
    this.linkMovementMethod = linkMovementMethod;
    this.submissionSwipeActionsProvider = submissionSwipeActionsProvider;
    this.commentSwipeActionsProvider = commentSwipeActionsProvider;
    this.draftStore = draftStore;
    setHasStableIds(true);
  }

  /**
   * We try to automatically dispose drafts subscribers in {@link SubmissionCommentInlineReply} when the
   * ViewHolder is getting recycled (in {@link #onViewRecycled(RecyclerView.ViewHolder)} or re-bound
   * to data. But onViewRecycled() doesn't get called on Activity destroy so this has to be manually
   * called.
   */
  public void forceDisposeDraftSubscribers() {
    inlineReplyDraftsDisposables.clear();
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    switch (VIEW_TYPES[viewType]) {
      case SUBMISSION_HEADER:
        SubmissionCommentsHeader.ViewHolder headerHolder = SubmissionCommentsHeader.ViewHolder.create(
            inflater,
            parent,
            headerClickStream,
            linkMovementMethod
        );
        headerHolder.setupGestures(this, submissionSwipeActionsProvider);
        return headerHolder;

      case COMMENTS_LOADING_PROGRESS:
        return SubmissionCommentsLoadingProgress.ViewHolder.create(inflater, parent);

      case USER_COMMENT:
        SubmissionComment.ViewHolder commentHolder = SubmissionComment.ViewHolder.create(inflater, parent);
        commentHolder.setBodyLinkMovementMethod(linkMovementMethod);
        commentHolder.setupGestures(this, commentSwipeActionsProvider);
        commentHolder.setupCollapseOnClick(this, commentClickStream);
        commentHolder.setupTapToRetrySending(this, replyRetrySendClickStream);
        commentHolder.forwardTouchEventsToBackground(linkMovementMethod);
        return commentHolder;

      case INLINE_REPLY:
        SubmissionCommentInlineReply.ViewHolder inlineReplyHolder = SubmissionCommentInlineReply.ViewHolder.create(inflater, parent);
        inlineReplyHolder.setupClickStreams(
            this,
            replyGifClickStream,
            replyDiscardClickStream,
            replyFullscreenClickStream,
            replySendClickStream
        );
        inlineReplyHolder.setupSavingOfDraftOnFocusLost(draftStore);
        return inlineReplyHolder;

      case LOAD_MORE_COMMENTS:
        SubmissionCommentsLoadMore.ViewHolder loadMoreViewHolder = SubmissionCommentsLoadMore.ViewHolder.create(inflater, parent);
        loadMoreViewHolder.setupClickStream(this, loadMoreCommentsClickStream);
        return loadMoreViewHolder;

      default:
        throw new UnsupportedOperationException("Unknown view type: " + VIEW_TYPES[viewType]);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    onBindViewHolder(holder, position);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (VIEW_TYPES[getItemViewType(position)]) {
      case SUBMISSION_HEADER:
        ((SubmissionCommentsHeader.ViewHolder) holder).bind((SubmissionCommentsHeader.UiModel) getItem(position), submissionSwipeActionsProvider);
        break;

      case COMMENTS_LOADING_PROGRESS:
        ((SubmissionCommentsLoadingProgress.ViewHolder) holder).bind((SubmissionCommentsLoadingProgress.UiModel) getItem(position));
        break;

      case USER_COMMENT:
        ((SubmissionComment.ViewHolder) holder).bind((SubmissionComment.UiModel) getItem(position));
        break;

      case INLINE_REPLY:
        SubmissionCommentInlineReply.UiModel replyUiModel = (SubmissionCommentInlineReply.UiModel) getItem(position);
        SubmissionCommentInlineReply.ViewHolder replyViewHolder = (SubmissionCommentInlineReply.ViewHolder) holder;
        inlineReplyDraftsDisposables.add(
            replyViewHolder.bind(replyUiModel, draftStore)
        );
        replyViewHolder.emitBindEvent(replyUiModel, replyViewBindStream);
        break;

      case LOAD_MORE_COMMENTS:
        ((SubmissionCommentsLoadMore.ViewHolder) holder).bind((SubmissionCommentsLoadMore.UiModel) getItem(position));
        break;

      default:
        throw new UnsupportedOperationException("Unknown view type: " + VIEW_TYPES[getItemViewType(position)]);
    }
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  public void onViewRecycled(RecyclerView.ViewHolder holder) {
    if (holder instanceof SubmissionCommentInlineReply.ViewHolder) {
      ((SubmissionCommentInlineReply.ViewHolder) holder).handleOnRecycle();
    }
    super.onViewRecycled(holder);
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

  public CommentSwipeActionsProvider commentSwipeActionsProvider() {
    return commentSwipeActionsProvider;
  }

  public Observable<Object> streamHeaderClicks() {
    return headerClickStream;
  }
}
