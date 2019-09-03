package me.saket.dank.ui.submission.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.events.SubmissionChangeCommentSortClicked;
import me.saket.dank.ui.submission.events.SubmissionCommentsRefreshClicked;
import me.saket.dank.utils.Animations;

public interface SubmissionCommentOptions {

  enum PartialChange {
    COMMENT_COUNT,
    SORTING_MODE
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {

    @Override
    public long adapterId() {
      return SubmissionCommentsAdapter.ID_COMMENT_OPTIONS;
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.COMMENT_OPTIONS;
    }

    public abstract String abbreviatedCount();

    @StringRes
    public abstract Integer sortRes();

    public static UiModel create(String abbreviatedCount, @StringRes int sortTextRes) {
      return new AutoValue_SubmissionCommentOptions_UiModel(abbreviatedCount, sortTextRes);
    }
  }

  // TODO: Add support for partial changes?
  class ViewHolder extends RecyclerView.ViewHolder {
    private final View commentOptionsContainerView;
    private final TextView commentCountView;
    private final Button commentSortingButton;
    private final Button commentRefreshButton;

    private UiModel uiModel;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comment_options, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      commentCountView = itemView.findViewById(R.id.submission_comment_count);
      commentSortingButton = itemView.findViewById(R.id.submission_comment_sorting);
      commentRefreshButton = itemView.findViewById(R.id.submission_comment_manual_refresh);
      commentOptionsContainerView = (View) commentSortingButton.getParent();
    }

    public void setupCommentOptionClicks(PublishRelay<UiEvent> events) {
      commentSortingButton.setOnClickListener(o -> events.accept(SubmissionChangeCommentSortClicked.create(commentOptionsContainerView)));
      commentRefreshButton.setOnClickListener(o -> events.accept(SubmissionCommentsRefreshClicked.create()));
    }

    public void set(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      commentCountView.setText(uiModel.abbreviatedCount());
      commentSortingButton.setText(uiModel.sortRes());
    }

    public void renderPartialChanges(List<Object> payloads) {
      for (Object payload : payloads) {
        //noinspection unchecked
        for (PartialChange partialChange : (List<PartialChange>) payload) {
          switch (partialChange) {
            case COMMENT_COUNT:
              commentCountView.setText(uiModel.abbreviatedCount());
              break;

            case SORTING_MODE:
              commentSortingButton.setText(uiModel.sortRes());
              break;

            default:
              throw new AssertionError();
          }
        }
      }
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    private final PublishRelay<UiEvent> uiEvents = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public Observable<? extends UiEvent> uiEvents() {
      return uiEvents;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = ViewHolder.create(inflater, parent);
      holder.setupCommentOptionClicks(uiEvents);
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.set(uiModel);
      holder.render();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      holder.set(uiModel);
      holder.renderPartialChanges(payloads);

      Transition transition = new ChangeBounds()
          .setInterpolator(Animations.INTERPOLATOR)
          .setDuration(300);
      TransitionManager.beginDelayedTransition(((ViewGroup) holder.itemView), transition);
    }
  }
}
