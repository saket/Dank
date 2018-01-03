package me.saket.dank.ui.submission.adapter;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.support.annotation.CheckResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.R;
import me.saket.dank.data.ContributionFullNameWrapper;
import me.saket.dank.ui.giphy.GiphyGif;
import me.saket.dank.ui.submission.DraftStore;
import me.saket.dank.ui.submission.events.ReplyDiscardClickEvent;
import me.saket.dank.ui.submission.events.ReplyFullscreenClickEvent;
import me.saket.dank.ui.submission.events.ReplyInsertGifClickEvent;
import me.saket.dank.ui.submission.events.ReplyItemViewBindEvent;
import me.saket.dank.ui.submission.events.ReplySendClickEvent;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.widgets.IndentedLayout;

/**
 * Inline reply for comments.
 */
public interface SubmissionCommentInlineReply {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract CharSequence authorHint();

    public abstract String parentContributionFullName();

    public abstract int indentationDepth();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.INLINE_REPLY;
    }

    public static UiModel create(long adapterId, CharSequence authorHint, String parentContributionFullName, int indentationDepth) {
      return new AutoValue_SubmissionCommentInlineReply_UiModel(adapterId, authorHint, parentContributionFullName, indentationDepth);
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
    private String parentContributionFullName;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_inline_reply, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      indentedLayout = itemView.findViewById(R.id.item_comment_reply_indented_container);
      discardButton = itemView.findViewById(R.id.item_comment_reply_discard);
      insertGifButton = itemView.findViewById(R.id.item_comment_reply_insert_gif);
      goFullscreenButton = itemView.findViewById(R.id.item_comment_reply_go_fullscreen);
      sendButton = itemView.findViewById(R.id.item_comment_reply_send);
      authorHintView = itemView.findViewById(R.id.item_comment_reply_author_hint);
      replyField = itemView.findViewById(R.id.item_comment_reply_message);
    }

    public void setupClickStreams(
        SubmissionCommentsAdapter adapter,
        Relay<ReplyInsertGifClickEvent> replyGifClickRelay,
        Relay<ReplyDiscardClickEvent> replyDiscardEventRelay,
        Relay<ReplyFullscreenClickEvent> replyFullscreenClickRelay,
        Relay<ReplySendClickEvent> replySendClickRelay)
    {
      discardButton.setOnClickListener(o -> {
        UiModel uiModel = (UiModel) adapter.getItem(getAdapterPosition());
        ContributionFullNameWrapper parentContribution = ContributionFullNameWrapper.create(uiModel.parentContributionFullName());
        replyDiscardEventRelay.accept(ReplyDiscardClickEvent.create(parentContribution));
      });

      insertGifButton.setOnClickListener(o ->
          replyGifClickRelay.accept(ReplyInsertGifClickEvent.create(getItemId()))
      );

      goFullscreenButton.setOnClickListener(o -> {
        CharSequence replyMessage = replyField.getText();
        UiModel uiModel = (UiModel) adapter.getItem(getAdapterPosition());
        String authorNameIfComment = uiModel.authorHint().toString();
        ContributionFullNameWrapper parentContribution = ContributionFullNameWrapper.create(uiModel.parentContributionFullName());
        replyFullscreenClickRelay.accept(ReplyFullscreenClickEvent.create(getItemId(), parentContribution, replyMessage, authorNameIfComment));
      });

      sendButton.setOnClickListener(o -> {
        setSavingDraftsAllowed(false);
        String replyMessage = replyField.getText().toString().trim();
        UiModel uiModel = (UiModel) adapter.getItem(getAdapterPosition());
        ContributionFullNameWrapper parentContribution = ContributionFullNameWrapper.create(uiModel.parentContributionFullName());
        replySendClickRelay.accept(ReplySendClickEvent.create(parentContribution, replyMessage));
      });
    }

    public void setupSavingOfDraftOnFocusLost(DraftStore draftStore) {
      replyField.setOnFocusChangeListener((v, hasFocus) -> {
        if (!hasFocus && savingDraftsAllowed) {
          ContributionFullNameWrapper parentContribution = ContributionFullNameWrapper.create(parentContributionFullName);

          // Fire-and-forget call. No need to dispose this since we're making no memory references to this VH.
          // WARNING: DON'T REFERENCE VH FIELDS IN THIS CHAIN TO AVOID LEAKING MEMORY.
          draftStore.saveDraft(parentContribution, replyField.getText().toString())
              .subscribeOn(Schedulers.io())
              .subscribe();
        }
      });
    }

    @CheckResult
    public Disposable bind(UiModel uiModel, DraftStore draftStore) {
      // Saving this field instead of getting it from adapter later because this holder's position
      // becomes -1 when a focus-lost callback is received when this holder is being removed.
      this.parentContributionFullName = uiModel.parentContributionFullName();

      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      authorHintView.setText(uiModel.authorHint());

      setSavingDraftsAllowed(true);
      draftDisposable.dispose();

      draftDisposable = draftStore.streamDrafts(ContributionFullNameWrapper.create(uiModel.parentContributionFullName()))
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

    public void emitBindEvent(UiModel uiModel, Relay<ReplyItemViewBindEvent> stream) {
      stream.accept(ReplyItemViewBindEvent.create(uiModel, replyField));
    }

    public void setSavingDraftsAllowed(boolean allowed) {
      savingDraftsAllowed = allowed;
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
}
