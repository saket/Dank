package me.saket.dank.ui.submission.adapter;

import android.annotation.SuppressLint;
import android.support.annotation.CheckResult;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.jakewharton.rxrelay2.Relay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.ui.submission.SubmissionContentLoadError;
import me.saket.dank.ui.submission.events.CommentClickEvent;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.ui.submission.events.ReplyDiscardClickEvent;
import me.saket.dank.ui.submission.events.ReplyFullscreenClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.ui.submission.events.SubmissionContentLinkClickEvent;
import me.saket.dank.ui.subreddit.SubmissionOptionSwipeEvent;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class SubmissionCommentsAdapter extends RecyclerViewArrayAdapter<SubmissionScreenUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<SubmissionScreenUiModel>, DiffUtil.DiffResult>>
{

  public static final long ID_VIEW_FULL_THREAD = -96;
  public static final long ID_MEDIA_CONTENT_LOAD_ERROR = -97;
  public static final long ID_COMMENTS_LOAD_PROGRESS = -98;
  public static final long ID_COMMENTS_LOAD_ERROR = -99;
  private static final SubmissionCommentRowType[] VIEW_TYPES = SubmissionCommentRowType.values();

  private final Map<SubmissionCommentRowType, SubmissionScreenUiModel.Adapter> childAdapters;
  private final SubmissionCommentsHeader.Adapter headerAdapter;
  private final SubmissionCommentsViewFullThread.Adapter viewFullThreadAdapter;
  private final SubmissionMediaContentLoadError.Adapter mediaContentLoadErrorAdapter;
  private final SubmissionCommentsLoadError.Adapter commentsLoadErrorAdapter;
  private final SubmissionComment.Adapter commentAdapter;
  private final SubmissionCommentInlineReply.Adapter inlineReplyAdapter;
  private final SubmissionCommentsLoadMore.Adapter loadMoreAdapter;

  @Inject
  public SubmissionCommentsAdapter(
      SubmissionCommentsHeader.Adapter headerAdapter,
      SubmissionMediaContentLoadError.Adapter mediaContentLoadErrorAdapter,
      SubmissionCommentsViewFullThread.Adapter viewFullThreadAdapter,
      SubmissionCommentsLoadError.Adapter commentsLoadErrorAdapter,
      SubmissionCommentsLoadProgress.Adapter commentsLoadProgressAdapter,
      SubmissionComment.Adapter commentAdapter,
      SubmissionCommentInlineReply.Adapter inlineReplyAdapter,
      SubmissionCommentsLoadMore.Adapter loadMoreAdapter)
  {
    this.viewFullThreadAdapter = viewFullThreadAdapter;
    childAdapters = new HashMap<>(11);
    childAdapters.put(SubmissionCommentRowType.SUBMISSION_HEADER, headerAdapter);
    childAdapters.put(SubmissionCommentRowType.MEDIA_CONTENT_LOAD_ERROR, mediaContentLoadErrorAdapter);
    childAdapters.put(SubmissionCommentRowType.VIEW_FULL_THREAD, viewFullThreadAdapter);
    childAdapters.put(SubmissionCommentRowType.COMMENTS_LOAD_ERROR, commentsLoadErrorAdapter);
    childAdapters.put(SubmissionCommentRowType.COMMENTS_LOAD_PROGRESS, commentsLoadProgressAdapter);
    childAdapters.put(SubmissionCommentRowType.USER_COMMENT, commentAdapter);
    childAdapters.put(SubmissionCommentRowType.INLINE_REPLY, inlineReplyAdapter);
    childAdapters.put(SubmissionCommentRowType.LOAD_MORE_COMMENTS, loadMoreAdapter);

    this.headerAdapter = headerAdapter;
    this.mediaContentLoadErrorAdapter = mediaContentLoadErrorAdapter;
    this.commentsLoadErrorAdapter = commentsLoadErrorAdapter;
    this.commentAdapter = commentAdapter;
    this.inlineReplyAdapter = inlineReplyAdapter;
    this.loadMoreAdapter = loadMoreAdapter;
    setHasStableIds(true);
  }

  public void forceDisposeDraftSubscribers() {
    inlineReplyAdapter.forceDisposeDraftSubscribers();
  }

  @Override
  public void accept(Pair<List<SubmissionScreenUiModel>, DiffUtil.DiffResult> pair) throws Exception {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return childAdapters.get(VIEW_TYPES[viewType]).onCreateViewHolder(inflater, parent);
  }

  @SuppressLint("NewApi")
  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    if (payloads.isEmpty()) {
      super.onBindViewHolder(holder, position, payloads);

    } else {
      //noinspection unchecked
      childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position), payloads);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position));
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  public void onViewRecycled(RecyclerView.ViewHolder holder) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onViewRecycled(holder);
    super.onViewRecycled(holder);
  }

  @CheckResult
  public Observable<LoadMoreCommentsClickEvent> streamLoadMoreCommentsClicks() {
    return loadMoreAdapter.loadMoreCommentsClickStream;
  }

  @CheckResult
  public Observable<ReplyDiscardClickEvent> streamReplyDiscardClicks() {
    return inlineReplyAdapter.replyDiscardClickStream;
  }

  @CheckResult
  public Observable<ReplySendClickEvent> streamReplySendClicks() {
    return inlineReplyAdapter.replySendClickStream;
  }

  @CheckResult
  public Observable<ReplyInsertGifClickEvent> streamReplyGifClicks() {
    return inlineReplyAdapter.replyGifClickStream;
  }

  @CheckResult
  public Observable<ReplyFullscreenClickEvent> streamReplyFullscreenClicks() {
    return inlineReplyAdapter.replyFullscreenClickStream;
  }

  @CheckResult
  public Relay<ReplyItemViewBindEvent> streamReplyItemViewBinds() {
    return inlineReplyAdapter.replyViewBindStream;
  }

  @CheckResult
  public Observable<CommentClickEvent> streamCommentCollapseExpandEvents() {
    return commentAdapter.commentClickStream;
  }

  @CheckResult
  public Observable<ReplyRetrySendClickEvent> streamReplyRetrySendClicks() {
    return commentAdapter.replyRetrySendClickStream;
  }

  @CheckResult
  public Observable<PostedOrInFlightContribution> streamCommentReplySwipeActions() {
    return commentAdapter.replySwipeActionStream;
  }

  @CheckResult
  public Observable<SubmissionOptionSwipeEvent> streamSubmissionOptionSwipeActions() {
    return headerAdapter.optionSwipeActionStream();
  }

  @CheckResult
  public Relay<Optional<SubmissionCommentsHeader.ViewHolder>> streamHeaderBinds() {
    return headerAdapter.headerBindStream;
  }

  @CheckResult
  public Observable<Object> streamHeaderClicks() {
    return headerAdapter.headerClickStream;
  }

  @CheckResult
  public Observable<SubmissionContentLinkClickEvent> streamContentLinkClicks() {
    return headerAdapter.contentLinkClickStream;
  }

  @CheckResult
  public Observable<SubmissionContentLoadError> streamMediaContentLoadRetryClicks() {
    return mediaContentLoadErrorAdapter.mediaContentLoadRetryClickStream;
  }

  @CheckResult
  public Observable<Object> streamCommentsLoadRetryClicks() {
    return commentsLoadErrorAdapter.commentsLoadRetryClickStream;
  }

  @CheckResult
  public Observable<DankSubmissionRequest> streamViewAllCommentsClicks() {
    return viewFullThreadAdapter.viewAllCommentsClicks;
  }
}
