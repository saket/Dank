package me.saket.dank.ui.submission.adapter;

import android.support.annotation.CheckResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.ui.submission.CommentSwipeActionsProvider;
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
      CommentSwipeActionsProvider commentSwipeActionsProvider)
  {
    this.linkMovementMethod = linkMovementMethod;
    this.submissionSwipeActionsProvider = submissionSwipeActionsProvider;
    this.commentSwipeActionsProvider = commentSwipeActionsProvider;
    setHasStableIds(true);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    switch (VIEW_TYPES[viewType]) {
      case SUBMISSION_HEADER:
        return SubmissionCommentsHeader.ViewHolder.create(inflater, parent, headerClickStream);

      case COMMENTS_LOADING_PROGRESS:
        return SubmissionCommentsLoadingProgress.ViewHolder.create(inflater, parent);

      case USER_COMMENT:
        return SubmissionComment.ViewHolder.create(inflater, parent);

      case INLINE_REPLY:
        return SubmissionInlineReply.ViewModel.create(inflater, parent);

      case LOAD_MORE_COMMENTS:
        return SubmissionCommentsLoadMore.ViewHolder.create(inflater, parent);

      default:
        throw new UnsupportedOperationException("Unknown view type: " + VIEW_TYPES[viewType]);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (VIEW_TYPES[getItemViewType(position)]) {
      case SUBMISSION_HEADER:
        SubmissionCommentsHeader.ViewHolder headerVH = (SubmissionCommentsHeader.ViewHolder) holder;
        SubmissionCommentsHeader.UiModel headerUiModel = (SubmissionCommentsHeader.UiModel) getItem(position);
        headerVH.bind(headerUiModel, linkMovementMethod);
        break;

      case COMMENTS_LOADING_PROGRESS:
        ((SubmissionCommentsLoadingProgress.ViewHolder) holder).bind((SubmissionCommentsLoadingProgress.UiModel) getItem(position));
        break;

      case USER_COMMENT:
        ((SubmissionComment.ViewHolder) holder).bind((SubmissionComment.UiModel) getItem(position));
        break;

      case INLINE_REPLY:
        ((SubmissionInlineReply.ViewModel) holder).bind((SubmissionInlineReply.UiModel) getItem(position));
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
