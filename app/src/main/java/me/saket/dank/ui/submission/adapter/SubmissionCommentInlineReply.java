package me.saket.dank.ui.submission.adapter;

import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Identifiable;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.markdownhints.MarkdownHintOptions;
import me.saket.dank.markdownhints.MarkdownHints;
import me.saket.dank.markdownhints.MarkdownSpanPool;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.events.ReplyDiscardClickEvent;
import me.saket.dank.ui.submission.events.ReplyFullscreenClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.SimpleTextWatcher;
import me.saket.dank.widgets.IndentedLayout;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

/**
 * Inline reply for comments.
 */
public interface SubmissionCommentInlineReply {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract SpannableWithTextEquality authorHint();

    public abstract Identifiable parent();

    public abstract String parentContributionAuthor();

    public abstract int indentationDepth();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.INLINE_REPLY;
    }

    public static UiModel create(
        long adapterId,
        CharSequence authorHint,
        Identifiable parent,
        String parentContributionAuthor,
        int indentationDepth)
    {
      return new AutoValue_SubmissionCommentInlineReply_UiModel(
          adapterId,
          SpannableWithTextEquality.wrap(authorHint),
          parent,
          parentContributionAuthor,
          indentationDepth
      );
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final IndentedLayout indentedLayout;
    private final ImageButton discardButton;
    private final ImageButton insertGifButton;
    private final ImageButton goFullscreenButton;
    private final ImageButton sendButton;
    private final TextView authorHintView;
    private final EditText replyField;

    private Disposable draftDisposable = Disposables.disposed();
    private boolean savingDraftsAllowed;
    private UiModel uiModel;
    private DraftStore draftStore;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent, DraftStore draftStore) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_inline_reply, parent, false), draftStore);
    }

    public ViewHolder(View itemView, DraftStore draftStore) {
      super(itemView);
      indentedLayout = itemView.findViewById(R.id.item_comment_reply_indented_container);
      discardButton = itemView.findViewById(R.id.item_comment_reply_discard);
      insertGifButton = itemView.findViewById(R.id.item_comment_reply_insert_gif);
      goFullscreenButton = itemView.findViewById(R.id.item_comment_reply_go_fullscreen);
      sendButton = itemView.findViewById(R.id.item_comment_reply_send);
      authorHintView = itemView.findViewById(R.id.item_comment_reply_author_hint);
      replyField = itemView.findViewById(R.id.item_comment_reply_message);

      this.draftStore = draftStore;

      sendButton.setEnabled(false);
      replyField.addTextChangedListener(new SimpleTextWatcher() {
        @Override
        public void afterTextChanged(Editable text) {
          boolean hasValidReply = text.toString().trim().length() > 0;
          sendButton.setEnabled(hasValidReply);
        }
      });
    }

    public void setupClicks(
        Relay<ReplyInsertGifClickEvent> replyGifClickRelay,
        Relay<ReplyDiscardClickEvent> replyDiscardEventRelay,
        Relay<ReplyFullscreenClickEvent> replyFullscreenClickRelay,
        Relay<ReplySendClickEvent> replySendClickRelay)
    {
      // Draft is saved in the reply field's focus change listener.
      discardButton.setOnClickListener(o -> {
        replyDiscardEventRelay.accept(ReplyDiscardClickEvent.create(uiModel.parent()));
      });

      insertGifButton.setOnClickListener(o ->
          replyGifClickRelay.accept(ReplyInsertGifClickEvent.create(getItemId()))
      );

      goFullscreenButton.setOnClickListener(o -> {
        saveDraftAsynchronouslyIfAllowed();

        String authorNameIfComment = uiModel.parentContributionAuthor();
        Identifiable parentContribution = uiModel.parent();
        replyFullscreenClickRelay.accept(ReplyFullscreenClickEvent.create(getItemId(), parentContribution, authorNameIfComment));
      });

      sendButton.setOnClickListener(o -> {
        setSavingDraftsAllowed(false);
        String replyMessage = replyField.getText().toString().trim();
        Identifiable parentContribution = uiModel.parent();
        replySendClickRelay.accept(ReplySendClickEvent.create(parentContribution, replyMessage));
      });
    }

    public void setupSavingOfDraftOnFocusLost() {
      replyField.setOnFocusChangeListener((v, hasFocus) -> {
        if (!hasFocus) {
          saveDraftAsynchronouslyIfAllowed();
        }
      });
    }

    private void saveDraftAsynchronouslyIfAllowed() {
      if (savingDraftsAllowed) {
        // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
        // WARNING: DON'T REFERENCE VH FIELDS IN THIS CHAIN TO AVOID LEAKING MEMORY.
        draftStore.saveDraft(uiModel.parent(), replyField.getText().toString())
            .subscribeOn(Schedulers.io())
            .observeOn(mainThread())
            .subscribe();
      }
    }

    public void setupMarkdownHints(MarkdownHintOptions markdownHintOptions, MarkdownSpanPool markdownSpanPool) {
      replyField.addTextChangedListener(new MarkdownHints(replyField, markdownHintOptions, markdownSpanPool));
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    @CheckResult
    public Disposable render(DraftStore draftStore) {
      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      authorHintView.setText(uiModel.authorHint());

      setSavingDraftsAllowed(true);
      draftDisposable.dispose();

      draftDisposable = draftStore.streamDrafts(uiModel.parent())
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

    public void setSavingDraftsAllowed(boolean allowed) {
      savingDraftsAllowed = allowed;
    }

    public void handleOnRecycle() {
      draftDisposable.dispose();
    }

    public void handlePickedGiphyGif(String title, GiphyGif giphyGif) {
      int selectionStart = Math.min(replyField.getSelectionStart(), replyField.getSelectionEnd());
      int selectionEnd = Math.max(replyField.getSelectionStart(), replyField.getSelectionEnd());

      String linkMarkdown = title.isEmpty()
          ? giphyGif.url()
          : String.format("[%s](%s)", title, giphyGif.url());
      replyField.getText().replace(selectionStart, selectionEnd, linkMarkdown);

      // Keyboard might have gotten dismissed while the GIF list was being scrolled.
      // Works only if called delayed. Posting to reply field's message queue works.
      replyField.post(() -> Keyboards.show(replyField));
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    private final MarkdownHintOptions markdownHintOptions;
    private final MarkdownSpanPool markdownSpanPool;
    private final DraftStore draftStore;
    final PublishRelay<ReplyItemViewBindEvent> replyViewBindStream = PublishRelay.create();
    final PublishRelay<ReplyInsertGifClickEvent> replyGifClickStream = PublishRelay.create();
    final PublishRelay<ReplyDiscardClickEvent> replyDiscardClickStream = PublishRelay.create();
    final PublishRelay<ReplySendClickEvent> replySendClickStream = PublishRelay.create();
    final PublishRelay<ReplyFullscreenClickEvent> replyFullscreenClickStream = PublishRelay.create();
    private final CompositeDisposable inlineReplyDraftsDisposables = new CompositeDisposable();

    @Inject
    public Adapter(MarkdownHintOptions markdownHintOptions, MarkdownSpanPool markdownSpanPool, DraftStore draftStore) {
      this.markdownHintOptions = markdownHintOptions;
      this.markdownSpanPool = markdownSpanPool;
      this.draftStore = draftStore;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      SubmissionCommentInlineReply.ViewHolder holder = SubmissionCommentInlineReply.ViewHolder.create(inflater, parent, draftStore);
      holder.setupClicks(
          replyGifClickStream,
          replyDiscardClickStream,
          replyFullscreenClickStream,
          replySendClickStream
      );
      // Note: We'll have to remove MarkdownHintOptions from Dagger graph when we introduce a light theme.
      holder.setupMarkdownHints(markdownHintOptions, markdownSpanPool);
      holder.setupSavingOfDraftOnFocusLost();
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      inlineReplyDraftsDisposables.add(
          holder.render(draftStore)
      );
      replyViewBindStream.accept(ReplyItemViewBindEvent.create(uiModel, holder.replyField));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
      holder.handleOnRecycle();
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
  }
}
