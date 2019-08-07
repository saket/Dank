package me.saket.dank.ui.submission.adapter;

import android.annotation.SuppressLint;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Comment;

import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import me.saket.dank.R;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.CommentSwipeActionsProvider;
import me.saket.dank.ui.submission.events.CommentClicked;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.widgets.IndentedLayout;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionRemoteComment {

  enum PartialChange {
    BYLINE, // Also includes vote count changes.
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract SpannableWithTextEquality byline();

    public abstract SpannableWithTextEquality body();

    @ColorInt
    public abstract int bylineTextColor();

    @ColorInt
    public abstract int bodyTextColor();

    public abstract int bodyMaxLines();

    public abstract int indentationDepth();

    public abstract Comment comment();

    @ColorRes
    public abstract int backgroundColorRes();

    public abstract boolean isCollapsed();

    public abstract boolean isFocused();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.REMOTE_USER_COMMENT;
    }

    public static UiModel.Builder builder() {
      return new AutoValue_SubmissionRemoteComment_UiModel.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder adapterId(long id);

      public Builder byline(CharSequence byline, Object voteScore) {
        return byline(SpannableWithTextEquality.wrap(byline, voteScore));
      }

      abstract Builder byline(SpannableWithTextEquality byline);

      public Builder body(CharSequence body) {
        return body(SpannableWithTextEquality.wrap(body));
      }

      abstract Builder body(SpannableWithTextEquality body);

      public abstract Builder bylineTextColor(@ColorInt int color);

      public abstract Builder bodyTextColor(@ColorInt int color);

      public abstract Builder indentationDepth(int indentationDepth);

      public abstract Builder bodyMaxLines(int maxLines);

      //public abstract Builder optionalPendingSyncReply(Optional<PendingSyncReply> optionalReply);

      public abstract Builder isCollapsed(boolean isCollapsed);

      public abstract Builder backgroundColorRes(@ColorRes int backgroundColorRes);

      public abstract Builder isFocused(boolean focused);

      public abstract Builder comment(Comment comment);

      public abstract UiModel build();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final IndentedLayout indentedLayout;
    private final TextView bylineView;
    private final TextView bodyView;
    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comment, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      bylineView = itemView.findViewById(R.id.item_comment_byline);
      bodyView = itemView.findViewById(R.id.item_comment_body);
      indentedLayout = itemView.findViewById(R.id.item_comment_indented_container);
    }

    public void setBodyLinkMovementMethod(DankLinkMovementMethod movementMethod) {
      bodyView.setMovementMethod(movementMethod);
    }

    public void setupGestures(CommentSwipeActionsProvider commentSwipeActionsProvider) {
      getSwipeableLayout().setSwipeActionIconProvider(commentSwipeActionsProvider.iconProvider());
      getSwipeableLayout().setSwipeActions(commentSwipeActionsProvider.actions());
      getSwipeableLayout().setOnPerformSwipeActionListener((action, swipeDirection) ->
          commentSwipeActionsProvider.performSwipeAction(action, uiModel.comment(), getSwipeableLayout(), swipeDirection)
      );
    }

    public void setupCollapseOnClick(Relay<UiEvent> uiEvents) {
      itemView.setOnClickListener(o -> {
        boolean willCollapse = !uiModel.isCollapsed();
        uiEvents.accept(CommentClicked.create(uiModel.comment(), getAdapterPosition(), itemView, willCollapse));
      });
    }

    @SuppressLint("ClickableViewAccessibility")
    public void forwardTouchEventsToBackground(BetterLinkMovementMethod linkMovementMethod) {
      // Bug workaround: TextView with clickable spans consume all touch events. Manually
      // transfer them to the parent so that the background touch indicator shows up +
      // click listener works.
      bodyView.setOnTouchListener((o, event) -> {
        boolean handledByMovementMethod = linkMovementMethod.onTouchEvent(bodyView, (Spannable) bodyView.getText(), event);
        return handledByMovementMethod || itemView.onTouchEvent(event);
      });
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      itemView.setBackgroundResource(uiModel.backgroundColorRes());
      indentedLayout.setIndentationDepth(uiModel.indentationDepth());
      bylineView.setText(uiModel.byline());
      bylineView.setTextColor(uiModel.bylineTextColor());
      bodyView.setText(uiModel.body());
      bodyView.setTextColor(uiModel.bodyTextColor());
      bodyView.setMaxLines(uiModel.bodyMaxLines());
    }

    public void renderPartialChanges(List<Object> payloads) {
      for (Object payload : payloads) {
        //noinspection unchecked
        for (PartialChange partialChange : (List<PartialChange>) payload) {
          switch (partialChange) {
            case BYLINE:
              bylineView.setText(uiModel.byline());
              break;

            default:
              throw new AssertionError();
          }
        }
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    private final DankLinkMovementMethod linkMovementMethod;
    private final CommentSwipeActionsProvider swipeActionsProvider;
    final PublishRelay<UiEvent> uiEvents = PublishRelay.create();

    @Inject
    public Adapter(DankLinkMovementMethod linkMovementMethod, CommentSwipeActionsProvider swipeActionsProvider) {
      this.linkMovementMethod = linkMovementMethod;
      this.swipeActionsProvider = swipeActionsProvider;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.setBodyLinkMovementMethod(linkMovementMethod);
      holder.setupGestures(swipeActionsProvider);
      holder.setupCollapseOnClick(uiEvents);
      holder.forwardTouchEventsToBackground(linkMovementMethod);
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.render();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      holder.setUiModel(uiModel);
      holder.renderPartialChanges(payloads);
    }

    @Override
    public Observable<? extends UiEvent> uiEvents() {
      return uiEvents;
    }

    @CheckResult
    public PublishRelay<SwipeEvent> swipeEvents() {
      return swipeActionsProvider.swipeEvents;
    }
  }
}
