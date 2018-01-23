package me.saket.dank.ui.subreddits.models;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class SubredditScreenUiModel {

  public interface SubmissionRowUiModel {
    enum Type {
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

    public abstract Builder toolbarRefreshVisible(boolean visible);

    public abstract Builder rowUiModels(List<SubmissionRowUiModel> uiModels);

    public abstract SubredditScreenUiModel build();
  }
}
