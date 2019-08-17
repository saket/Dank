package me.saket.dank.ui.submission.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.auto.value.AutoValue;

import java.util.List;
import javax.inject.Inject;

import me.saket.dank.R;

public interface SubmissionCommentsLoadProgress {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public long adapterId() {
      return SubmissionCommentsAdapter.ID_COMMENTS_LOAD_PROGRESS;
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.COMMENTS_LOAD_PROGRESS;
    }

    public static UiModel create() {
      return new AutoValue_SubmissionCommentsLoadProgress_UiModel();
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_load_progress, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      return ViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      // Nothing to do here.
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
