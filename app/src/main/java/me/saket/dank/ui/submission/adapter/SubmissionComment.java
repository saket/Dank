package me.saket.dank.ui.submission.adapter;

import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import me.saket.dank.R;
import me.saket.dank.widgets.IndentedLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionComment {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract CharSequence byline();

    public abstract CharSequence body();

    @ColorInt
    public abstract int bylineTextColor();

    @ColorInt
    public abstract int bodyTextColor();

    public abstract int bodyMaxLines();

    public abstract int indentationDepth();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.USER_COMMENT;
    }

    public static UiModel.Builder builder() {
      return new AutoValue_SubmissionComment_UiModel.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder adapterId(long id);

      public abstract Builder byline(CharSequence byline);

      public abstract Builder body(CharSequence body);

      public abstract Builder bylineTextColor(@ColorInt int color);

      public abstract Builder bodyTextColor(@ColorInt int color);

      public abstract Builder indentationDepth(int indentationDepth);

      public abstract UiModel build();

      public abstract Builder bodyMaxLines(int maxLines);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final IndentedLayout indentedLayout;
    private final TextView bylineView;
    private final TextView bodyView;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comment, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      bylineView = itemView.findViewById(R.id.item_comment_byline);
      bodyView = itemView.findViewById(R.id.item_comment_body);
      indentedLayout = itemView.findViewById(R.id.item_comment_indented_container);
    }

    public void bind(UiModel uiModel) {
      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      bylineView.setText(uiModel.byline());
      bylineView.setTextColor(uiModel.bylineTextColor());
      bodyView.setText(uiModel.body());
      bodyView.setTextColor(uiModel.bodyTextColor());
      bodyView.setMaxLines(uiModel.bodyMaxLines());
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
