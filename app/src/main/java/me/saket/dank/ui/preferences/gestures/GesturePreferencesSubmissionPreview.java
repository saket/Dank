package me.saket.dank.ui.preferences.gestures;

import android.view.View;

import com.google.auto.value.AutoValue;

import java.util.List;
import javax.inject.Inject;

import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider;
import me.saket.dank.ui.subreddit.uimodels.SubredditSubmission;

public interface GesturePreferencesSubmissionPreview {

  @AutoValue
  abstract class UiModel implements GesturePreferenceUiModel {
    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.SUBMISSION_PREVIEW;
    }

    public abstract SubredditSubmission.UiModel submissionUiModel();

    public static UiModel create(SubredditSubmission.UiModel submissionUiModel) {
      return new AutoValue_GesturePreferencesSubmissionPreview_UiModel(GesturePreferencesAdapter.SUBMISSION_PREVIEW_ADAPTER_ID, submissionUiModel);
    }
  }

  class ViewHolder extends SubredditSubmission.ViewHolder {
    private ViewHolder(View itemView) {
      super(itemView);
    }
  }

  class Adapter extends SubredditSubmission.Adapter implements GesturePreferenceUiModel.ChildAdapter<UiModel, SubredditSubmission.ViewHolder> {

    @Inject
    public Adapter(SubmissionSwipeActionsProvider swipeActionsProvider) {
      super(swipeActionsProvider);
    }

    @Override
    protected int itemLayoutRes() {
      return super.itemLayoutRes();
    }

    @Override
    public void onBindViewHolder(SubredditSubmission.ViewHolder holder, GesturePreferencesSubmissionPreview.UiModel uiModel) {
      super.onBindViewHolder(holder, uiModel.submissionUiModel());
    }

    @Override
    public void onBindViewHolder(SubredditSubmission.ViewHolder holder, GesturePreferencesSubmissionPreview.UiModel uiModel, List<Object> payloads) {
      super.onBindViewHolder(holder, uiModel.submissionUiModel(), payloads);
    }
  }
}
