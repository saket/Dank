package me.saket.dank.ui.submission.adapter;

import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import me.saket.dank.R;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.AnimatedProgressBar;

public interface SubmissionMediaContentLoadError {

  Object NOTHING = LifecycleStreams.NOTHING;
  long ADAPTER_ID = -99L;

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    public abstract String title();

    public abstract String byline();

    @DrawableRes
    public abstract int errorIconRes();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.MEDIA_CONTENT_LOAD_ERROR;
    }

    public static UiModel create(String title, String byline, int errorIconRes) {
      return new AutoValue_SubmissionMediaContentLoadError_UiModel(ADAPTER_ID, title, byline, errorIconRes);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private final ViewGroup containerView;
    private final TextView titleView;
    private final TextView bylineView;
    private final ImageView iconView;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_header_link, parent, false));
    }

    public ViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_link_title);
      bylineView = itemView.findViewById(R.id.submission_link_byline);
      iconView = itemView.findViewById(R.id.submission_link_icon);
      containerView = itemView.findViewById(R.id.submission_link_container);
      containerView.setClipToOutline(true);

      ((AnimatedProgressBar) itemView.findViewById(R.id.submission_link_progress)).setVisibilityWithoutAnimation(View.GONE);
    }

    public void setupClickStream(Relay<Object> clickStream) {
      itemView.setOnClickListener(o -> clickStream.accept(NOTHING));
    }

    void bind(UiModel uiModel) {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());
      iconView.setImageResource(uiModel.errorIconRes());
    }
  }
}
