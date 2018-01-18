package me.saket.dank.ui.subreddits.models;

import android.support.annotation.CheckResult;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Submission;
import net.dean.jraw.models.VoteDirection;

import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.PostedOrInFlightContribution;
import me.saket.dank.data.SpannableWithValueEquality;
import me.saket.dank.ui.subreddits.SubmissionSwipeActionsProvider;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.glide.GlideCircularTransformation;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubredditSubmission {

  @AutoValue
  abstract class UiModel implements SubredditScreenUiModel.SubmissionRowUiModel {
    @Override
    public Type type() {
      return Type.SUBMISSION;
    }

    @Override
    public abstract long adapterId();

    public abstract Optional<UiModel.Thumbnail> thumbnail();

    public abstract SpannableWithValueEquality title();

    public abstract SpannableWithValueEquality byline();

    public abstract Submission submission();

    public abstract PostedOrInFlightContribution submissionInfo();

    abstract ExtraInfoForEquality extraInfoForEquality();

    public static Builder builder() {
      return new AutoValue_SubredditSubmission_UiModel.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder adapterId(long id);

      public abstract Builder thumbnail(Optional<UiModel.Thumbnail> thumbnail);

      abstract Builder title(SpannableWithValueEquality title);

      public Builder title(CharSequence title) {
        return title(SpannableWithValueEquality.wrap(title));
      }

      abstract Builder byline(SpannableWithValueEquality byline);

      public Builder byline(CharSequence byline) {
        return byline(SpannableWithValueEquality.wrap(byline));
      }

      public abstract Builder submission(Submission submission);

      public abstract Builder submissionInfo(PostedOrInFlightContribution info);

      abstract Builder extraInfoForEquality(ExtraInfoForEquality extraInfo);

      public abstract UiModel build();
    }

    @AutoValue
    public abstract static class Thumbnail {
      public abstract Optional<Integer> staticRes();

      public abstract Optional<String> remoteUrl();

      public abstract Optional<Integer> backgroundRes();

      public abstract ImageView.ScaleType scaleType();

      public abstract Optional<Integer> tintColor();

      public abstract String contentDescription();

      public static Builder builder() {
        return new AutoValue_SubredditSubmission_UiModel_Thumbnail.Builder();
      }

      @AutoValue.Builder
      public abstract static class Builder {
        public abstract Builder staticRes(Optional<Integer> resId);

        public abstract Builder remoteUrl(Optional<String> url);

        public abstract Builder backgroundRes(Optional<Integer> backgroundRes);

        public abstract Builder scaleType(ImageView.ScaleType scaleType);

        public abstract Builder tintColor(Optional<Integer> tintColor);

        public abstract Builder contentDescription(String description);

        public abstract Thumbnail build();
      }
    }

    /**
     * Triggers a change because {@link SpannableWithValueEquality} otherwise won't as it only compares text and not spans.
     */
    @AutoValue
    abstract static class ExtraInfoForEquality {
      public abstract Pair<Integer, VoteDirection> votes();

      public abstract Integer commentsCount();

      public static ExtraInfoForEquality create(Pair<Integer, VoteDirection> votes, Integer commentsCount) {
        return new AutoValue_SubredditSubmission_UiModel_ExtraInfoForEquality(votes, commentsCount);
      }
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final ImageView thumbnailView;
    private final TextView titleView;
    private final TextView bylineView;
    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      thumbnailView = itemView.findViewById(R.id.submission_item_icon);
      titleView = itemView.findViewById(R.id.submission_item_title);
      bylineView = itemView.findViewById(R.id.submission_item_byline);
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());
      thumbnailView.setVisibility(uiModel.thumbnail().isPresent() ? View.VISIBLE : View.GONE);

      if (uiModel.thumbnail().isPresent()) {
        UiModel.Thumbnail thumbnailUiModel = uiModel.thumbnail().get();
        thumbnailView.setBackgroundResource(thumbnailUiModel.backgroundRes().orElse(0));
        thumbnailView.setScaleType(thumbnailUiModel.scaleType());
        thumbnailView.setContentDescription(thumbnailUiModel.contentDescription());

        if (thumbnailUiModel.tintColor().isPresent()) {
          thumbnailView.setColorFilter(thumbnailUiModel.tintColor().get());
        } else {
          thumbnailView.setColorFilter(null);
        }

        if (thumbnailUiModel.staticRes().isPresent()) {
          thumbnailView.setImageResource(thumbnailUiModel.staticRes().get());
        } else {
          Glide.with(itemView)
              .load(thumbnailUiModel.remoteUrl().get())
              .apply(RequestOptions.bitmapTransform(GlideCircularTransformation.INSTANCE))
              .transition(DrawableTransitionOptions.withCrossFade())
              .into(thumbnailView);
        }
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }

  class Adapter implements SubredditScreenUiModel.SubmissionRowUiChildAdapter<UiModel, ViewHolder> {
    private final Relay<SubredditSubmissionClickEvent> submissionClicks = PublishRelay.create();
    private final SubmissionSwipeActionsProvider swipeActionsProvider;

    @Inject
    public Adapter(SubmissionSwipeActionsProvider swipeActionsProvider) {
      this.swipeActionsProvider = swipeActionsProvider;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.itemView.setOnClickListener(o ->
          submissionClicks.accept(SubredditSubmissionClickEvent.create(holder.uiModel.submission(), holder.itemView, holder.getItemId()))
      );

      SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
      swipeableLayout.setSwipeActionIconProvider(swipeActionsProvider);
      swipeableLayout.setOnPerformSwipeActionListener(action ->
          swipeActionsProvider.performSwipeAction(action, holder.uiModel.submissionInfo(), swipeableLayout)
      );
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
      holder.render();

      holder.getSwipeableLayout().setSwipeActions(swipeActionsProvider.actionsFor(uiModel.submissionInfo()));
    }

    @CheckResult
    public Observable<SubredditSubmissionClickEvent> submissionClicks() {
      return submissionClicks;
    }
  }
}
