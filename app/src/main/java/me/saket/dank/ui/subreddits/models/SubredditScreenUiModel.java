package me.saket.dank.ui.subreddits.models;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.auto.value.AutoValue;

import java.util.List;

import me.saket.dank.ui.submission.CachedSubmissionFolder;

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
  }

  @AutoValue
  public abstract static class ToolbarRefreshUiModel {
    enum State {
      HIDDEN(false),
      IDLE(true),
      IN_FLIGHT(false),
      FAILED(true);

      private boolean isClickable;

      State(boolean isClickable) {
        this.isClickable = isClickable;
      }
    }

    public abstract State iconState();

    public boolean isClickable() {
      return iconState().isClickable;
    }

    public static ToolbarRefreshUiModel create(State state) {
      return new AutoValue_SubredditScreenUiModel_ToolbarRefreshUiModel(state);
    }
  }

  /**
   * Note: Initial load when DB is empty for a {@link CachedSubmissionFolder} is also treated as pagination.
   */
  public abstract boolean fullscreenProgressVisible();

  /**
   * Toolbar refresh is used for overriding existing submissions.
   */
  public abstract ToolbarRefreshUiModel toolbarRefresh();

  public abstract List<SubmissionRowUiModel> rowUiModels();

  public static Builder builder() {
    return new AutoValue_SubredditScreenUiModel.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fullscreenProgressVisible(boolean visible);

    public abstract Builder toolbarRefresh(ToolbarRefreshUiModel icon);

    public abstract Builder rowUiModels(List<SubmissionRowUiModel> uiModels);

    public abstract SubredditScreenUiModel build();
  }
}
