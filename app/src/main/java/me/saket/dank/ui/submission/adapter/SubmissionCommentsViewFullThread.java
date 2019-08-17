package me.saket.dank.ui.submission.adapter;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import java.util.List;
import javax.inject.Inject;

import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.UiEvent;
import me.saket.dank.ui.submission.events.SubmissionViewFullCommentsClicked;
import me.saket.dank.utils.DankSubmissionRequest;
import me.saket.dank.utils.Truss;

public interface SubmissionCommentsViewFullThread {

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public long adapterId() {
      return SubmissionCommentsAdapter.ID_VIEW_FULL_THREAD;
    }

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.VIEW_FULL_THREAD;
    }

    public abstract DankSubmissionRequest submissionRequest();

    public static UiModel create(DankSubmissionRequest submissionRequest) {
      return new AutoValue_SubmissionCommentsViewFullThread_UiModel(submissionRequest);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private TextView messageView;
    private UiModel uiModel;

    public ViewHolder(View itemView) {
      super(itemView);
      messageView = itemView.findViewById(R.id.item_comment_viewfullthread_message);
      messageView.setText(new Truss()
          .append(messageView.getContext().getString(R.string.submission_comments_viewing_single_comments_thread))
          .append("\n")
          .pushSpan(new ForegroundColorSpan(ContextCompat.getColor(messageView.getContext(), R.color.cyan_400)))
          .append(messageView.getContext().getString(R.string.submission_comments_view_all_comments))
          .popSpan()
          .build());
    }

    public void setUiModel(UiModel uiModel) {
      this.uiModel = uiModel;
    }
  }

  class Adapter implements SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {
    private PublishRelay<UiEvent> viewAllCommentsClicks = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.list_item_submission_comment_view_full_thread, parent, false));
      holder.messageView.setOnClickListener(o -> viewAllCommentsClicks.accept(SubmissionViewFullCommentsClicked.create()));
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.setUiModel(uiModel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Observable<? extends UiEvent> uiEvents() {
      return viewAllCommentsClicks;
    }
  }
}
