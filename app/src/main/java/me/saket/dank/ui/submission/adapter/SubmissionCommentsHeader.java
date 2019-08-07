package me.saket.dank.ui.submission.adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.CheckResult;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;
import io.reactivex.Observable;
import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;
import timber.log.Timber;

import java.util.List;
import javax.inject.Inject;

import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.SpannableWithTextEquality;
import me.saket.dank.data.SwipeEvent;
import me.saket.dank.ui.submission.events.SubmissionContentLinkClickEvent;
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Colors;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.AnimatedProgressBar;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionCommentsHeader {

  float THUMBNAIL_COLOR_TINT_OPACITY = 0.4f;
  int CONTENT_LINK_TRANSITION_ANIM_DURATION = 300;

  enum PartialChange {
    SUBMISSION_TITLE,
    SUBMISSION_BYLINE,
    SUBMISSION_SAVE_STATUS,
    SUBMISSION_SWIPE_ACTIONS,
    CONTENT_LINK,
    CONTENT_LINK_THUMBNAIL,
    CONTENT_LINK_FAVICON,
    CONTENT_LINK_TITLE_AND_BYLINE,
    CONTENT_LINK_PROGRESS_VISIBILITY,
    CONTENT_LINK_TINT,
  }

  static int getWidthForAlbumContentLinkThumbnail(Context context) {
    return context.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_external_link);
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    abstract SpannableWithTextEquality title();

    abstract SpannableWithTextEquality byline();

    abstract Optional<SpannableWithTextEquality> optionalSelfText();

    abstract Optional<SubmissionContentLinkUiModel> optionalContentLinkModel();

    /**
     * The original data model from which this Ui model was created.
     */
    abstract Submission submission();

    public abstract boolean isSaved();

    public abstract SwipeActions swipeActions();

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

      abstract Builder title(SpannableWithTextEquality title);

      abstract Builder byline(SpannableWithTextEquality byline);

      Builder title(CharSequence title, Pair<Integer, VoteDirection> votes) {
        return title(SpannableWithTextEquality.wrap(title, votes));
      }

      Builder byline(CharSequence byline) {
        return byline(SpannableWithTextEquality.wrap(byline));
      }

      abstract Builder optionalSelfText(Optional<SpannableWithTextEquality> text);

      Builder selfText(Optional<CharSequence> optionalText) {
        return optionalSelfText(optionalText.map(text -> SpannableWithTextEquality.wrap(text)));
      }

      abstract Builder optionalContentLinkModel(Optional<SubmissionContentLinkUiModel> link);

      abstract Builder submission(Submission submission);

      public abstract Builder isSaved(boolean saved);

      public abstract Builder swipeActions(SwipeActions swipeActions);

      abstract UiModel build();
    }

    /**
     * Triggers a change because {@link SpannableWithTextEquality} otherwise won't as it only compares text and not spans.
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
    public final TextView bylineView;
    private final TextView selfTextView;
    private final ViewGroup selfTextViewContainer;
    private final ViewGroup contentLinkView;
    private final ImageView contentLinkIconView;
    private final ImageView contentLinkThumbnailView;
    private final TextView contentLinkTitleView;
    private final TextView contentLinkBylineView;
    private final AnimatedProgressBar contentLinkProgressView;
    private final DankLinkMovementMethod movementMethod;
    private final @ColorInt int contentLinkBackgroundColor;

    private UiModel uiModel;
    private int lastTintColor = -1;

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

    public ViewHolder(View itemView, DankLinkMovementMethod movementMethod) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_title);
      bylineView = itemView.findViewById(R.id.submission_byline);
      selfTextView = itemView.findViewById(R.id.submission_selfpost_text);
      selfTextViewContainer = itemView.findViewById(R.id.submission_selfpost_container);
      contentLinkView = itemView.findViewById(R.id.submission_link_container);
      contentLinkIconView = itemView.findViewById(R.id.submission_link_icon);
      contentLinkThumbnailView = itemView.findViewById(R.id.submission_link_thumbnail);
      contentLinkTitleView = itemView.findViewById(R.id.submission_link_title);
      contentLinkBylineView = itemView.findViewById(R.id.submission_link_byline);
      contentLinkProgressView = itemView.findViewById(R.id.submission_link_progress);

      this.movementMethod = movementMethod;

      contentLinkView.setClipToOutline(true);
      contentLinkBackgroundColor = ContextCompat.getColor(itemView.getContext(), R.color.submission_link_background);
    }

    public void setupGestures(SubmissionSwipeActionsProvider swipeActionsProvider) {
      getSwipeableLayout().setSwipeActionIconProvider((imageView, oldAction, newAction) -> {
        swipeActionsProvider.showSwipeActionIcon(imageView, oldAction, newAction, uiModel.submission());
      });
      getSwipeableLayout().setOnPerformSwipeActionListener((action, swipeDirection) -> {
        swipeActionsProvider.performSwipeAction(action, uiModel.submission(), getSwipeableLayout(), swipeDirection);
      });
    }

    public void setupContentLinkClicks(
        Relay<SubmissionContentLinkClickEvent> clicks,
        Relay<SubmissionContentLinkClickEvent> longClicks)
    {
      contentLinkView.setOnClickListener(o ->
          clicks.accept(SubmissionContentLinkClickEvent.create(
              contentLinkView,
              uiModel.optionalContentLinkModel().get().link()))
      );
      contentLinkView.setOnLongClickListener(o -> {
        longClicks.accept(SubmissionContentLinkClickEvent.create(
            contentLinkView,
            uiModel.optionalContentLinkModel().get().link()));
        return true;  // true == long click handled.
      });
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      setSubmissionTitle(uiModel);
      setSubmissionByline(uiModel);
      setContentLink(uiModel, false);

      uiModel.optionalSelfText().ifPresent(selfTextView::setText);
      selfTextViewContainer.setVisibility(uiModel.optionalSelfText().isPresent() ? View.VISIBLE : View.GONE);
      selfTextView.setMovementMethod(movementMethod);
    }

    private void setSubmissionByline(UiModel uiModel) {
      bylineView.setText(uiModel.byline());
    }

    private void setSubmissionTitle(UiModel uiModel) {
      titleView.setText(uiModel.title());
    }

    public void renderPartialChanges(List<Object> payloads) {
      for (Object payload : payloads) {
        //noinspection unchecked
        for (PartialChange partialChange : (List<PartialChange>) payload) {
          switch (partialChange) {
            case SUBMISSION_TITLE:
              setSubmissionTitle(uiModel);
              break;

            case SUBMISSION_BYLINE:
              setSubmissionByline(uiModel);
              break;

            case SUBMISSION_SAVE_STATUS:
            case SUBMISSION_SWIPE_ACTIONS:
              getSwipeableLayout().setSwipeActions(uiModel.swipeActions());
              break;

            case CONTENT_LINK:
              setContentLink(uiModel, true);
              break;

            case CONTENT_LINK_THUMBNAIL:
              setContentLinkThumbnail(uiModel.optionalContentLinkModel().get(), true);
              break;

            case CONTENT_LINK_FAVICON:
              setContentLinkIcon(uiModel.optionalContentLinkModel().get(), true);
              break;

            case CONTENT_LINK_TITLE_AND_BYLINE:
              setContentLinkTitleAndByline(uiModel.optionalContentLinkModel().get());
              break;

            case CONTENT_LINK_PROGRESS_VISIBILITY:
              setContentLinkProgressVisibility(uiModel.optionalContentLinkModel().get(), true);
              break;

            case CONTENT_LINK_TINT:
              setContentLinkTint(uiModel.optionalContentLinkModel().get(), true);
              break;

            default:
              throw new AssertionError();
          }
        }
      }
    }

    /**
     * @param animate True only when handling partial updates. Running animations during full binds is not recommended.
     *                They'll leave the VH in a transient state and RecyclerView will not be able to recycle the VH.
     */
    private void setContentLink(UiModel uiModel, boolean animate) {
      contentLinkView.setVisibility(uiModel.optionalContentLinkModel().isPresent() ? View.VISIBLE : View.GONE);

      uiModel.optionalContentLinkModel().ifPresent(contentLinkUiModel -> {
        setContentLinkIcon(contentLinkUiModel, animate);
        setContentLinkThumbnail(contentLinkUiModel, animate);
        setContentLinkTitleAndByline(contentLinkUiModel);
        setContentLinkTint(contentLinkUiModel, animate);
        setContentLinkProgressVisibility(contentLinkUiModel, animate);
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

    private void setContentLinkIcon(SubmissionContentLinkUiModel contentLinkUiModel, boolean animate) {
      contentLinkIconView.setImageDrawable(contentLinkUiModel.icon().orElse(null));
      if (contentLinkUiModel.iconBackgroundRes().isPresent()) {
        contentLinkIconView.setBackgroundResource(contentLinkUiModel.iconBackgroundRes().get());
      } else {
        contentLinkIconView.setBackground(null);
      }

      if (contentLinkUiModel.icon().isPresent()) {
        contentLinkIconView.setVisibility(View.VISIBLE);
        if (animate) {
          contentLinkIconView.setAlpha(0f);
          contentLinkIconView.animate()
              .alpha(1f)
              .setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION)
              .setInterpolator(Animations.INTERPOLATOR)
              .start();
        }
      } else {
        contentLinkIconView.setVisibility(View.INVISIBLE);
        contentLinkIconView.setAlpha(1f);
        contentLinkIconView.animate().cancel();
      }
    }

    private void setContentLinkThumbnail(SubmissionContentLinkUiModel contentLinkUiModel, boolean animate) {
      Drawable thumbnail = contentLinkUiModel.thumbnail().isPresent() ? contentLinkUiModel.thumbnail().get() : null;
      contentLinkThumbnailView.setImageDrawable(thumbnail);

      if (contentLinkUiModel.thumbnail().isPresent()) {
        if (animate) {
          contentLinkThumbnailView.setAlpha(0f);
          contentLinkThumbnailView.animate()
              .alpha(1f)
              .setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION)
              .setInterpolator(Animations.INTERPOLATOR)
              .start();
        }
      } else {
        contentLinkThumbnailView.animate().cancel();
        contentLinkThumbnailView.setAlpha(1f);
      }
    }

    private void setContentLinkTint(SubmissionContentLinkUiModel contentLinkUiModel, boolean animate) {
      if (contentLinkUiModel.backgroundTintColor().isPresent()) {
        Drawable background = contentLinkView.getBackground().mutate();
        Integer tintColor = contentLinkUiModel.backgroundTintColor().get();

        if (tintColor == lastTintColor) {
          if (BuildConfig.DEBUG) {
            Timber.w("Ignoring duplicate tint: %s", Colors.colorIntToHex(tintColor));
          }
          return;
        }

        // Not sure the same tint is received when the same submission is opened again.
        if (animate) {
          ValueAnimator colorAnimator = ValueAnimator.ofArgb(contentLinkBackgroundColor, tintColor);
          colorAnimator.addUpdateListener(animation -> {
            int animatedColor = (int) animation.getAnimatedValue();
            contentLinkThumbnailView.setColorFilter(Colors.applyAlpha(animatedColor, THUMBNAIL_COLOR_TINT_OPACITY));
            background.setTint(animatedColor);
          });
          colorAnimator.setDuration(CONTENT_LINK_TRANSITION_ANIM_DURATION);
          colorAnimator.setInterpolator(Animations.INTERPOLATOR);
          colorAnimator.start();

        } else {
          contentLinkThumbnailView.setColorFilter(Colors.applyAlpha(tintColor, THUMBNAIL_COLOR_TINT_OPACITY));
          background.setTint(tintColor);
        }
        lastTintColor = tintColor;

      } else {
        contentLinkThumbnailView.setColorFilter(null);
        contentLinkView.getBackground().mutate().setTintList(null);
      }
    }

    private void setContentLinkProgressVisibility(SubmissionContentLinkUiModel contentLinkUiModel, boolean animate) {
      if (animate) {
        contentLinkProgressView.setVisibility(contentLinkUiModel.progressVisible() ? View.VISIBLE : View.GONE);
      } else {
        contentLinkProgressView.setVisibilityWithoutAnimation(contentLinkUiModel.progressVisible() ? View.VISIBLE : View.GONE);
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    private final DankLinkMovementMethod linkMovementMethod;
    private final SubmissionSwipeActionsProvider swipeActionsProvider;

    // TODO: merge all these streams.
    final PublishRelay<Object> headerClickStream = PublishRelay.create();
    final PublishRelay<SubmissionContentLinkClickEvent> contentLinkClicks = PublishRelay.create();
    final PublishRelay<SubmissionContentLinkClickEvent> contentLinkLongClicks = PublishRelay.create();
    final PublishRelay<Optional<SubmissionCommentsHeader.ViewHolder>> headerBindStream = PublishRelay.create();

    @Inject
    public Adapter(DankLinkMovementMethod linkMovementMethod, SubmissionSwipeActionsProvider swipeActionsProvider) {
      this.linkMovementMethod = linkMovementMethod;
      this.swipeActionsProvider = swipeActionsProvider;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent, headerClickStream, linkMovementMethod);
      holder.setupGestures(swipeActionsProvider);
      holder.setupContentLinkClicks(contentLinkClicks, contentLinkLongClicks);
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.getSwipeableLayout().setSwipeActions(uiModel.swipeActions());
      holder.render();

      headerBindStream.accept(Optional.of(holder));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      holder.setUiModel(uiModel);
      holder.renderPartialChanges(payloads);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
      headerBindStream.accept(Optional.empty());
    }

    @CheckResult
    public Observable<SwipeEvent> swipeEvents() {
      return swipeActionsProvider.getSwipeEvents();
    }
  }
}
