package me.saket.dank.ui.submission.adapter;

import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.ui.submission.CommentSwipeActionsProvider;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.submission.events.CommentClickEvent;
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent;
import me.saket.dank.utils.Optional;
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

    public abstract PostedOrInFlightContribution originalComment();

    /** Present only for locally posted replies. */
    public abstract Optional<PendingSyncReply> optionalPendingSyncReply();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.USER_COMMENT;
    }

    public abstract boolean isCollapsed();

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

      public abstract Builder bodyMaxLines(int maxLines);

      /**
       * The original data model from which this Ui model was created.
       */
      public abstract Builder originalComment(PostedOrInFlightContribution comment);

      public abstract Builder isCollapsed(boolean isCollapsed);

      public abstract Builder optionalPendingSyncReply(Optional<PendingSyncReply> optionalReply);

      public abstract UiModel build();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final IndentedLayout indentedLayout;
    private final TextView bylineView;
    private final TextView bodyView;
    private View.OnClickListener collapseOnClickListener;
    private View.OnClickListener tapToRetryClickListener;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comment, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      bylineView = itemView.findViewById(R.id.item_comment_byline);
      bodyView = itemView.findViewById(R.id.item_comment_body);
      indentedLayout = itemView.findViewById(R.id.item_comment_indented_container);
    }

    public void setupGestures(SubmissionCommentsAdapter adapter, CommentSwipeActionsProvider commentSwipeActionsProvider) {
      getSwipeableLayout().setSwipeActionIconProvider(commentSwipeActionsProvider.iconProvider());
      getSwipeableLayout().setSwipeActions(commentSwipeActionsProvider.actions());
      getSwipeableLayout().setOnPerformSwipeActionListener(action -> {
        UiModel commentUiModel = (UiModel) adapter.getItem(getAdapterPosition());
        commentSwipeActionsProvider.performSwipeAction(action, commentUiModel.originalComment(), getSwipeableLayout());

        // TODO.
        // We should ideally only be updating the backing data-set and let onBind() handle the
        // changes, but RecyclerView's item animator reset's the View's x-translation which we
        // don't want. So we manually update the Views here.
        //onBindViewHolder(holder, holder.getAdapterPosition());
      });
    }

    public void setupCollapseOnClick(SubmissionCommentsAdapter adapter, Relay<CommentClickEvent> clickStream) {
      collapseOnClickListener = o -> {
        UiModel commentUiModel = (UiModel) adapter.getItem(getAdapterPosition());
        boolean willCollapse = !commentUiModel.isCollapsed();
        CommentClickEvent event = CommentClickEvent.create(
            commentUiModel.originalComment(),
            getAdapterPosition(),
            itemView,
            willCollapse
        );
        clickStream.accept(event);
      };
    }

    public void setupTapToRetrySending(SubmissionCommentsAdapter adapter, Relay<ReplyRetrySendClickEvent> retrySendClickStream) {
      tapToRetryClickListener = o -> {
        UiModel commentUiModel = (UiModel) adapter.getItem(getAdapterPosition());
        PendingSyncReply failedPendingSyncReply = commentUiModel.optionalPendingSyncReply().get();
        retrySendClickStream.accept(ReplyRetrySendClickEvent.create(failedPendingSyncReply));
      };
    }

    public void forwardTouchEventsToBackground(SubmissionCommentsAdapter adapter, BetterLinkMovementMethod linkMovementMethod) {
      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      bodyView.setOnTouchListener((o, event) -> {
        UiModel uiModel = (UiModel) adapter.getItem(getAdapterPosition());
        if (uiModel.isCollapsed()) {
          return false;
        }
        boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(bodyView, (Spannable) bodyView.getText(), event);
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
    }

    public void bind(UiModel uiModel) {
      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      bylineView.setText(uiModel.byline());
      bylineView.setTextColor(uiModel.bylineTextColor());
      bodyView.setText(uiModel.body());
      bodyView.setTextColor(uiModel.bodyTextColor());
      bodyView.setMaxLines(uiModel.bodyMaxLines());

      // Enable gestures only if it's a posted comment.
      getSwipeableLayout().setSwipeEnabled(uiModel.originalComment().isPosted());

      Optional<PendingSyncReply> optionalReply = uiModel.optionalPendingSyncReply();
      boolean isFailedReply = optionalReply.isPresent() && optionalReply.get().state() == PendingSyncReply.State.FAILED;
      if (isFailedReply) {
        itemView.setOnClickListener(tapToRetryClickListener);
      } else {
        itemView.setOnClickListener(collapseOnClickListener);
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
