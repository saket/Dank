package me.saket.dank.ui.submission.adapter;

import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.CommentNode;

import me.saket.dank.R;
import me.saket.dank.data.CommentNodeEqualsBandAid;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.ui.submission.events.LoadMoreCommentsClickEvent;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.IndentedLayout;

/** For loading more replies of a comment. */
public interface SubmissionCommentsLoadMore {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract SpannableWithTextEquality label();

    @DrawableRes
    public abstract int iconRes();

    public abstract boolean clickEnabled();

    public abstract int indentationDepth();

    public abstract CommentNodeEqualsBandAid parentCommentNodeEqualsBandAid();

    public CommentNode parentCommentNode() {
      return parentCommentNodeEqualsBandAid().get();
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.LOAD_MORE_COMMENTS;
    }

    public static UiModel.Builder builder() {
      return new AutoValue_SubmissionCommentsLoadMore_UiModel.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      public abstract Builder adapterId(long id);

      public Builder label(CharSequence label) {
        return label(SpannableWithTextEquality.wrap(label));
      }

      abstract Builder label(SpannableWithTextEquality label);

      public abstract Builder iconRes(@DrawableRes int iconRes);

      public abstract Builder clickEnabled(boolean enabled);

      public abstract Builder indentationDepth(int depth);

      public abstract Builder parentCommentNodeEqualsBandAid(CommentNodeEqualsBandAid parentCommentNode);

      public Builder parentCommentNode(CommentNode parentCommentNode) {
        return parentCommentNodeEqualsBandAid(CommentNodeEqualsBandAid.create(parentCommentNode));
      }

      public abstract UiModel build();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final TextView loadMoreButton;
    private final IndentedLayout indentedContainer;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_comments_load_more, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      loadMoreButton = itemView.findViewById(R.id.item_loadmorecomments_load_more);
      indentedContainer = itemView.findViewById(R.id.item_loadmorecomments_indented_container);
    }

    public void setupClicks(SubmissionCommentsAdapter adapter, Relay<LoadMoreCommentsClickEvent> clickStream) {
      itemView.setOnClickListener(o -> loadMoreButton.performClick());
      itemView.setOnTouchListener((o, event) -> loadMoreButton.onTouchEvent(event));

      loadMoreButton.setOnClickListener(o -> {
        if (getAdapterPosition() == -1) {
          // Is being removed.
          return;
        }
        UiModel uiModel = (UiModel) adapter.getItem(getAdapterPosition());
        clickStream.accept(LoadMoreCommentsClickEvent.create(itemView, uiModel.parentCommentNode()));
      });
    }

    public void render(UiModel uiModel) {
      indentedContainer.setIndentationDepth(uiModel.indentationDepth());
      loadMoreButton.setEnabled(uiModel.clickEnabled());
      loadMoreButton.setText(uiModel.label());
      Views.setCompoundDrawableEnd(loadMoreButton, uiModel.iconRes());
    }
  }
}
