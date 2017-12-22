package me.saket.dank.ui.submission.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.Relay;

import me.saket.dank.R;
import me.saket.dank.utils.DankLinkMovementMethod;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.lifecycle.LifecycleStreams;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public interface SubmissionCommentsHeader {

  static int getWidthForAlbumContentLinkThumbnail(Context context) {
    return context.getResources().getDimensionPixelSize(R.dimen.submission_link_thumbnail_width_album);
  }

  @AutoValue
  abstract class UiModel implements SubmissionScreenUiModel {
    @Override
    public abstract long adapterId();

    public abstract CharSequence title();

    public abstract CharSequence byline();

    public abstract Optional<CharSequence> selfText();

    public abstract Optional<SubmissionContentLinkUiModel> contentLink();

    @Override
    public SubmissionCommentRowType type() {
      return SubmissionCommentRowType.SUBMISSION_HEADER;
    }

    public static UiModel create(
        long adapterId,
        CharSequence title,
        CharSequence byline,
        Optional<CharSequence> selfText,
        Optional<SubmissionContentLinkUiModel> contentLink)
    {
      return new AutoValue_SubmissionCommentsHeader_UiModel(adapterId, title, byline, selfText, contentLink);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private TextView titleView;
    private TextView bylineView;
    private final TextView selfTextView;
    private final ViewGroup contentLinkView;
    private final ImageView contentLinkIconView;
    private final ImageView contentLinkThumbnailView;
    private final TextView contentLinkTitleView;
    private final TextView contentLinkBylineView;
    private final ProgressBar contentLinkProgressView;

    public static ViewHolder create(LayoutInflater inflater, ViewGroup parent, Relay<Object> headerClickStream) {
      ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.list_item_submission_comments_header, parent, false));
      holder.itemView.setOnClickListener(v -> headerClickStream.accept(LifecycleStreams.NOTHING));
      return holder;
    }

    public ViewHolder(View itemView) {
      super(itemView);
      titleView = itemView.findViewById(R.id.submission_title);
      bylineView = itemView.findViewById(R.id.submission_byline);
      selfTextView = itemView.findViewById(R.id.submission_selfpost_text);
      contentLinkView = itemView.findViewById(R.id.submission_link_container);
      contentLinkIconView = itemView.findViewById(R.id.submission_link_icon);
      contentLinkThumbnailView = itemView.findViewById(R.id.submission_link_thumbnail);
      contentLinkTitleView = itemView.findViewById(R.id.submission_link_title);
      contentLinkBylineView = itemView.findViewById(R.id.submission_link_byline);
      contentLinkProgressView = itemView.findViewById(R.id.submission_link_progress);
    }

    public void bind(UiModel uiModel, DankLinkMovementMethod movementMethod) {
      titleView.setText(uiModel.title());
      bylineView.setText(uiModel.byline());

      // TODO.
      selfTextView.setVisibility(View.GONE);
      selfTextView.setMovementMethod(movementMethod);

      if (uiModel.contentLink().isPresent()) {
        SubmissionContentLinkUiModel contentLinkUiModel = uiModel.contentLink().get();
        contentLinkView.setVisibility(View.VISIBLE);

        contentLinkTitleView.setText(contentLinkUiModel.title());
        contentLinkBylineView.setText(contentLinkUiModel.byline());
        contentLinkProgressView.setVisibility(contentLinkUiModel.progressVisible() ? View.VISIBLE : View.GONE);
        if (contentLinkUiModel.icon().isPresent()) {
          contentLinkIconView.setImageBitmap(contentLinkUiModel.icon().get());
        }
        if (contentLinkUiModel.thumbnail().isPresent()) {
          contentLinkThumbnailView.setImageBitmap(contentLinkUiModel.thumbnail().get());
        }

      } else {
        contentLinkView.setVisibility(View.GONE);
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return (SwipeableLayout) itemView;
    }
  }
}
