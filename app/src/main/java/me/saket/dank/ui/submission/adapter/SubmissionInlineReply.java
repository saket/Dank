package me.saket.dank.ui.submission.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import me.saket.dank.R;
import me.saket.dank.widgets.IndentedLayout;

/**
 * Inline reply for comments.
 */
public interface SubmissionInlineReply {

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
      return new AutoValue_SubmissionInlineReply_UiModel(adapterId, authorHint, parentContributionFullName, indentationDepth);
    }
  }

  class ViewModel extends RecyclerView.ViewHolder {
    private final IndentedLayout indentedLayout;
    private final ImageButton discardButton;
    private final ImageButton insertGifButton;
    private final ImageButton goFullscreenButton;
    private final ImageButton sendButton;
    private final TextView authorHintView;
    private final EditText replyField;

    public static ViewModel create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewModel(inflater.inflate(R.layout.list_item_submission_comments_inline_reply, parent, false));
    }

    public ViewModel(View itemView) {
      super(itemView);
      indentedLayout = itemView.findViewById(R.id.item_comment_reply_indented_container);
      discardButton = itemView.findViewById(R.id.item_comment_reply_discard);
      insertGifButton = itemView.findViewById(R.id.item_comment_reply_insert_gif);
      goFullscreenButton = itemView.findViewById(R.id.item_comment_reply_go_fullscreen);
      sendButton = itemView.findViewById(R.id.item_comment_reply_send);
      authorHintView = itemView.findViewById(R.id.item_comment_reply_author_hint);
      replyField = itemView.findViewById(R.id.item_comment_reply_message);
    }

    public void bind(UiModel uiModel) {
      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      authorHintView.setText(uiModel.authorHint());
    }
  }
}
