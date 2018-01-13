package me.saket.dank.ui.submission.adapter;

import android.annotation.SuppressLint;
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
import me.saket.dank.data.links.Link;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownSpanPool;
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
import timber.log.Timber;

public class SubmissionCommentsAdapter extends RecyclerViewArrayAdapter<SubmissionScreenUiModel, RecyclerView.ViewHolder> {

  public static final long ID_MEDIA_CONTENT_LOAD_ERROR = -98;
  public static final long ID_COMMENTS_LOAD_INDIDACTOR = -99;
  private static final SubmissionCommentRowType[] VIEW_TYPES = SubmissionCommentRowType.values();

  // Injected by Dagger.
  private final DankLinkMovementMethod linkMovementMethod;
  private final SubmissionSwipeActionsProvider submissionSwipeActionsProvider;
  private final CommentSwipeActionsProvider commentSwipeActionsProvider;
  private final DraftStore draftStore;
  private final CompositeDisposable inlineReplyDraftsDisposables = new CompositeDisposable();
  private final MarkdownHintOptions markdownHintOptions;
  private final MarkdownSpanPool markdownSpanPool;

  // Event streams.
  private final PublishRelay<CommentClickEvent> commentClickStream = PublishRelay.create();
  private final PublishRelay<LoadMoreCommentsClickEvent> loadMoreCommentsClickStream = PublishRelay.create();
  private final Relay<ReplyItemViewBindEvent> replyViewBindStream = PublishRelay.create();
  private final Relay<ReplyInsertGifClickEvent> replyGifClickStream = PublishRelay.create();
  private final Relay<ReplyDiscardClickEvent> replyDiscardClickStream = PublishRelay.create();
  private final Relay<ReplySendClickEvent> replySendClickStream = PublishRelay.create();
  private final Relay<ReplyRetrySendClickEvent> replyRetrySendClickStream = PublishRelay.create();
  private final Relay<ReplyFullscreenClickEvent> replyFullscreenClickStream = PublishRelay.create();
  private final Relay<Object> headerClickStream = PublishRelay.create();
  private final Relay<Link> contentLinkClickStream = PublishRelay.create();
  private final Relay<SubmissionCommentsHeader.ViewHolder> headerBindStream = PublishRelay.create();
  private final Relay<SubmissionCommentsHeader.ViewHolder> headerUnbindStream = PublishRelay.create();
  private final Relay<Object> mediaContentLoadRetryClickStream = PublishRelay.create();

  @Inject
  public SubmissionCommentsAdapter(
      DankLinkMovementMethod linkMovementMethod,
      SubmissionSwipeActionsProvider submissionSwipeActionsProvider,
      CommentSwipeActionsProvider commentSwipeActionsProvider,
      DraftStore draftStore,
      MarkdownHintOptions markdownHintOptions,
      MarkdownSpanPool markdownSpanPool)
  {
    this.linkMovementMethod = linkMovementMethod;
    this.submissionSwipeActionsProvider = submissionSwipeActionsProvider;
    this.commentSwipeActionsProvider = commentSwipeActionsProvider;
    this.draftStore = draftStore;
    this.markdownHintOptions = markdownHintOptions;
    this.markdownSpanPool = markdownSpanPool;
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
        headerHolder.setupContentLinkClickStream(this, contentLinkClickStream);
        return headerHolder;

      case MEDIA_CONTENT_LOAD_ERROR:
        SubmissionMediaContentLoadError.ViewHolder mediaLoadErrorHolder = SubmissionMediaContentLoadError.ViewHolder.create(inflater, parent);
        mediaLoadErrorHolder.setupClickStream(mediaContentLoadRetryClickStream);
        return mediaLoadErrorHolder;

      case COMMENTS_LOAD_INDICATOR:
        return SubmissionCommentsLoadIndicator.ViewHolder.create(inflater, parent);

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
        // Note: We'll have to remove MarkdownHintOptions from Dagger graph when we introduce a light theme.
        inlineReplyHolder.setupMarkdownHints(markdownHintOptions, markdownSpanPool);
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

  @SuppressLint("NewApi")
  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
    if (!payloads.isEmpty()) {
      switch (getItem(position).type()) {
        case SUBMISSION_HEADER:
          SubmissionCommentsHeader.UiModel headerModel = (SubmissionCommentsHeader.UiModel) getItem(position);
          SubmissionCommentsHeader.ViewHolder headerHolder = (SubmissionCommentsHeader.ViewHolder) holder;
          headerHolder.handlePartialChanges(payloads, headerModel);
          break;

        case USER_COMMENT:
          if (((List) payloads.get(0)).get(0) instanceof SubmissionCommentsHeader.PartialChange) {
            Timber.w("Item: %s", getItem(position));
            Timber.w("Item type: %s", getItem(position).type());
            Timber.i("Payloads:");
            payloads.forEach(payload -> {
              Timber.i("payload: %s", payload);
            });

            Timber.w("WRRROONNGG PAYLOADS!");

          } else {
            SubmissionComment.UiModel commentModel = (SubmissionComment.UiModel) getItem(position);
            SubmissionComment.ViewHolder commentHolder = (SubmissionComment.ViewHolder) holder;
            commentHolder.handlePartialChanges(payloads, commentModel);
          }
          break;

        case COMMENTS_LOAD_INDICATOR:
        case INLINE_REPLY:
        case LOAD_MORE_COMMENTS:
          throw new UnsupportedOperationException("Partial change not supported yet for " + getItem(position).type() + ", payload: " + payloads);

        default:
          throw new UnsupportedOperationException("Unknown view type: " + getItem(position).type());
      }

    } else {
      super.onBindViewHolder(holder, position, payloads);
    }
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (getItem(position).type()) {
      case SUBMISSION_HEADER:
        SubmissionCommentsHeader.ViewHolder headerVH = (SubmissionCommentsHeader.ViewHolder) holder;
        headerVH.bind((SubmissionCommentsHeader.UiModel) getItem(position), submissionSwipeActionsProvider);
        headerBindStream.accept(headerVH);
        break;

      case MEDIA_CONTENT_LOAD_ERROR:
        ((SubmissionMediaContentLoadError.ViewHolder) holder).bind((SubmissionMediaContentLoadError.UiModel) getItem(position));
        break;

      case COMMENTS_LOAD_INDICATOR:
        ((SubmissionCommentsLoadIndicator.ViewHolder) holder).bind((SubmissionCommentsLoadIndicator.UiModel) getItem(position));
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
    if (holder instanceof SubmissionCommentsHeader.ViewHolder) {
      headerUnbindStream.accept(((SubmissionCommentsHeader.ViewHolder) holder));
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

  @CheckResult
  public Relay<SubmissionCommentsHeader.ViewHolder> streamHeaderBinds() {
    return headerBindStream;
  }

  @CheckResult
  public Relay<SubmissionCommentsHeader.ViewHolder> streamHeaderUnbinds() {
    return headerUnbindStream;
  }

  @CheckResult
  public Observable<Object> streamHeaderClicks() {
    return headerClickStream;
  }

  @CheckResult
  public Observable<Link> streamContentLinkClicks() {
    return contentLinkClickStream;
  }

  @CheckResult
  public Observable<Object> streamMediaContentLoadRetryClicks() {
    return mediaContentLoadRetryClickStream;
  }

  public CommentSwipeActionsProvider commentSwipeActionsProvider() {
    return commentSwipeActionsProvider;
  }
}
