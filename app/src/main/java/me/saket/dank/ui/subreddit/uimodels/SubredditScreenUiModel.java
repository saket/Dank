package me.saket.dank.ui.subreddit.uimodels;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;

import java.util.List;

import me.saket.dank.data.EmptyState;
import me.saket.dank.data.ErrorState;
import me.saket.dank.utils.Optional;

@AutoValue
public abstract class SubredditScreenUiModel {

  public interface SubmissionRowUiModel {
    enum Type {
      GESTURES_WALKTHROUGH,
      SUBMISSION,
      PAGINATION_FOOTER
    }

    Type type();

    long adapterId();
  }

  public interface SubmissionRowUiChildAdapter<T extends SubmissionRowUiModel, VH extends RecyclerView.ViewHolder> {
    VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);

    void onBindViewHolder(VH holder, T uiModel);

    void onBindViewHolder(VH holder, T uiModel, List<Object> payloads);
  }

  public abstract boolean fullscreenProgressVisible();

  public abstract Optional<ErrorState> fullscreenError();

  public abstract Optional<EmptyState> emptyState();

  /**
   * Toolbar refresh is used for force-refreshing all submissions.
   */
  public abstract boolean toolbarRefreshVisible();

  public abstract List<SubmissionRowUiModel> rowUiModels();

  public static Builder builder() {
    return new AutoValue_SubredditScreenUiModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fullscreenProgressVisible(boolean visible);

    public abstract Builder fullscreenError(Optional<ErrorState> resolvedError);

    public abstract Builder toolbarRefreshVisible(boolean visible);

    public abstract Builder rowUiModels(List<SubmissionRowUiModel> uiModels);

    public abstract Builder emptyState(Optional<EmptyState> emptyState);

    public abstract SubredditScreenUiModel build();
  }
}
