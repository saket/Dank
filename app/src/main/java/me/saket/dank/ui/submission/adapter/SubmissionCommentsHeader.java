package me.saket.dank.ui.submission.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.VoteDirection;

import java.util.List;

import me.saket.dank.R;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.SpannableWithValueEquality;
import me.saket.dank.ui.subreddits.SubmissionSwipeActionsProvider;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionCommentsHeader {

  int CONTENT_LINK_TRANSITION_ANIM_DURATION = 300;

  enum PartialChange {
    SUBMISSION_VOTE,
    SUBMISSION_COMMENT_COUNT,
    CONTENT_LINK,
    CONTENT_LINK_THUMBNAIL,
    CONTENT_LINK_FAVICON,
    CONTENT_LINK_TITLE_AND_BYLINE,
    CONTENT_LINK_PROGRESS_VISIBILITY,
    CONTENT_LINK_TINT,
  }

  static int getWidthForAlbumContentLinkThumbnail(Context context) {
    return context.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    abstract SpannableWithValueEquality title();

    abstract SpannableWithValueEquality byline();

    abstract Optional<SpannableWithValueEquality> optionalSelfText();

    abstract Optional<SubmissionContentLinkUiModel> optionalContentLink();

    /**
     * The original data model from which this Ui model was created.
     */
    abstract PostedOrInFlightContribution originalSubmission();

    abstract ExtraInfoForEquality extraInfoForEquality();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.SUBMISSION_HEADER;
    }

    static UiModel.Builder builder() {
      return new AutoValue_SubmissionCommentsHeader_UiModel.Builder();
    }

    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder adapterId(long id);

      abstract Builder title(SpannableWithValueEquality title);

      abstract Builder byline(SpannableWithValueEquality byline);

      Builder title(CharSequence title) {
        return title(SpannableWithValueEquality.wrap(title));
      }

      Builder byline(CharSequence byline) {
        return byline(SpannableWithValueEquality.wrap(byline));
      }

      abstract Builder optionalSelfText(Optional<SpannableWithValueEquality> text);

      Builder selfText(Optional<CharSequence> optionalText) {
        return optionalSelfText(optionalText.isPresent()
            ? Optional.of(SpannableWithValueEquality.wrap(optionalText.get()))
            : Optional.empty());
      }

      abstract Builder optionalContentLink(Optional<SubmissionContentLinkUiModel> link);

      abstract Builder originalSubmission(PostedOrInFlightContribution submission);

      abstract Builder extraInfoForEquality(ExtraInfoForEquality info);

      abstract UiModel build();
    }

    /**
     * Triggers a change because {@link SpannableWithValueEquality} otherwise won't as it only compares text and not spans.
     */
    @AutoValue
    abstract static class ExtraInfoForEquality {
      public abstract Pair<Integer, VoteDirection> votes();

      public abstract Integer commentsCount();

      public static ExtraInfoForEquality create(Pair<Integer, VoteDirection> votes, Integer commentsCount) {
        return new AutoValue_SubmissionCommentsHeader_UiModel_ExtraInfoForEquality(votes, commentsCount);
      }
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final TextView titleView;
    private final TextView bylineView;
    private final TextView selfTextView;
    private final ViewGroup contentLinkView;
    private final ImageView contentLinkIconView;
    private final ImageView contentLinkThumbnailView;
    private final TextView contentLinkTitleView;
    private final TextView contentLinkBylineView;
    private final ProgressBar contentLinkProgressView;
    private final DankLinkMovementMethod movementMethod;
    private final @ColorInt int contentLinkBackgroundColor;

    public static ViewHolder create(
        LayoutInflater inflater,
        ViewGroup parent,
        Relay<Object> headerClickStream,
        DankLinkMovementMethod movementMethod)
    {
      View itemView = inflater.inflate(R.layout.list_item_submission_comments_header, parent, false);
      ViewHolder holder = new ViewHolder(itemView, movementMethod);
      holder.itemView.setOnClickListener(v -> headerClickStream.accept(LifecycleStreams.NOTHING));
      return holder;
    }

    public void setupGestures(SubmissionCommentsAdapter adapter, SubmissionSwipeActionsProvider swipeActionsProvider) {
      getSwipeableLayout().setSwipeActionIconProvider(swipeActionsProvider);
      getSwipeableLayout().setOnPerformSwipeActionListener(action -> {
        UiModel headerUiModel = (UiModel) adapter.getItem(getAdapterPosition());
        swipeActionsProvider.performSwipeAction(action, headerUiModel.originalSubmission(), getSwipeableLayout());
      });
    }

    public ViewHolder(View itemView, DankLinkMovementMethod movementMethod) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_title);
      bylineView = itemView.findViewById(R.id.submission_byline);
      selfTextView = itemView.findViewById(R.id.submission_selfpost_text);
      contentLinkView = itemView.findViewById(R.id.submission_link_container);
      contentLinkIconView = itemView.findViewById(R.id.submission_link_icon);
      contentLinkThumbnailView = itemView.findViewById(R.id.submission_link_thumbnail);
      contentLinkTitleView = itemView.findViewById(R.id.submission_link_title);
      contentLinkBylineView = itemView.findViewById(R.id.submission_link_byline);
      contentLinkProgressView = itemView.findViewById(R.id.submission_link_progress);
      this.movementMethod = movementMethod;

      contentLinkView.setClipToOutline(true);
      contentLinkBackgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.submission_link_background_color);
    }

    public void bind(UiModel uiModel, SubmissionSwipeActionsProvider swipeActionsProvider) {
      setSubmissionTitle(uiModel);
      setSubmissionByline(uiModel);
      setContentLink(uiModel);

      // TODO.
      selfTextView.setVisibility(View.GONE);
      selfTextView.setMovementMethod(movementMethod);

      // Gestures.
      getSwipeableLayout().setSwipeActions(swipeActionsProvider.actionsFor(uiModel.originalSubmission()));
    }

    private void setSubmissionByline(UiModel uiModel) {
      bylineView.setText(uiModel.byline());
    }

    private void setSubmissionTitle(UiModel uiModel) {
      titleView.setText(uiModel.title());
    }

    @SuppressWarnings("unchecked")
    public void handlePartialChanges(List<Object> payloads, UiModel uiModel) {
      for (Object payload : payloads) {
        for (PartialChange partialChange : (List<PartialChange>) payload) {
          switch (partialChange) {
            case SUBMISSION_VOTE:
              setSubmissionTitle(uiModel);
              break;

            case SUBMISSION_COMMENT_COUNT:
              setSubmissionByline(uiModel);
              break;

            case CONTENT_LINK:
              setContentLink(uiModel);
              break;

            case CONTENT_LINK_THUMBNAIL:
              //Timber.i("for thumbnail");
              setContentLinkThumbnail(uiModel.optionalContentLink().get());
              break;

            case CONTENT_LINK_FAVICON:
              setContentLinkIcon(uiModel.optionalContentLink().get());
              break;

            case CONTENT_LINK_TITLE_AND_BYLINE:
              setContentLinkTitleAndByline(uiModel.optionalContentLink().get());
              break;

            case CONTENT_LINK_PROGRESS_VISIBILITY:
              setContentLinkProgressVisibility(uiModel.optionalContentLink().get());
              break;

            case CONTENT_LINK_TINT:
              setContentLinkTint(uiModel.optionalContentLink().get());
              break;
          }
        }
      }
    }

    private void setContentLink(UiModel uiModel) {
      contentLinkView.setVisibility(uiModel.optionalContentLink().isPresent() ? View.VISIBLE : View.GONE);

      uiModel.optionalContentLink().ifPresent(contentLinkUiModel -> {
        setContentLinkIcon(contentLinkUiModel);
        setContentLinkThumbnail(contentLinkUiModel);
        setContentLinkTitleAndByline(contentLinkUiModel);
        setContentLinkProgressVisibility(contentLinkUiModel);
        setContentLinkTint(contentLinkUiModel);
      });
    }

    private void setContentLinkTitleAndByline(SubmissionContentLinkUiModel contentLinkUiModel) {
      contentLinkTitleView.setText(contentLinkUiModel.title());
      contentLinkTitleView.setTextColor(ContextCompat.getColor(itemView.getContext(), contentLinkUiModel.titleTextColorRes()));
      contentLinkBylineView.setMaxLines(contentLinkUiModel.titleMaxLines());

      contentLinkBylineView.setText(contentLinkUiModel.byline());
      contentLinkBylineView.setTextColor(ContextCompat.getColor(itemView.getContext(), contentLinkUiModel.bylineTextColorRes()));
      // Else, the entire content Link container is hidden.
    }

    private void setContentLinkThumbnail(SubmissionContentLinkUiModel contentLinkUiModel) {
      Drawable thumbnail = contentLinkUiModel.thumbnail().isPresent() ? contentLinkUiModel.thumbnail().get() : null;
      contentLinkThumbnailView.setImageDrawable(thumbnail);

      if (contentLinkUiModel.thumbnail().isPresent()) {
        contentLinkThumbnailView.setAlpha(0f);
        contentLinkThumbnailView.animate()
            .alpha(1f)
            .setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION)
            .setInterpolator(Animations.INTERPOLATOR)
            .start();
      } else {
        contentLinkThumbnailView.animate().cancel();
      }
    }

    private void setContentLinkIcon(SubmissionContentLinkUiModel contentLinkUiModel) {
      Drawable favicon = contentLinkUiModel.icon().isPresent() ? contentLinkUiModel.icon().get() : null;
      contentLinkIconView.setImageDrawable(favicon);

      if (contentLinkUiModel.icon().isPresent()) {
        contentLinkIconView.setVisibility(View.VISIBLE);
        contentLinkIconView.setAlpha(0f);
        contentLinkIconView.animate()
            .alpha(1f)
            .setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION)
            .setInterpolator(Animations.INTERPOLATOR)
            .start();
      } else {
        contentLinkIconView.setVisibility(View.INVISIBLE);
        contentLinkIconView.animate().cancel();
      }
    }

    private void setContentLinkTint(SubmissionContentLinkUiModel contentLinkUiModel) {
      if (contentLinkUiModel.backgroundTintColor().isPresent()) {
        Drawable background = contentLinkView.getBackground().mutate();
        Integer tintColor = contentLinkUiModel.backgroundTintColor().get();

        ValueAnimator colorAnimator = ValueAnimator.ofArgb(contentLinkBackgroundColor, tintColor);
        colorAnimator.addUpdateListener(animation -> background.setTint((int) animation.getAnimatedValue()));
        colorAnimator.setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION);
        colorAnimator.setInterpolator(Animations.INTERPOLATOR);
        colorAnimator.start();

      } else {
        contentLinkView.getBackground().mutate().setTintList(null);
      }
    }

    private void setContentLinkProgressVisibility(SubmissionContentLinkUiModel contentLinkUiModel) {
      contentLinkProgressView.setVisibility(contentLinkUiModel.progressVisible() ? View.VISIBLE : View.GONE);
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
