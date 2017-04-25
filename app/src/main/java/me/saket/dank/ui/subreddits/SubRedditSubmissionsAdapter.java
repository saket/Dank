package me.saket.dank.ui.subreddits;

import android.support.annotation.DrawableRes;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import net.dean.jraw.models.Submission;

import java.util.List;

import butterknife.BindColor;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.utils.GlideCircularTransformation;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class SubRedditSubmissionsAdapter extends RecyclerViewArrayAdapter<Submission, SubRedditSubmissionsAdapter.SubmissionViewHolder>
        implements Consumer<List<Submission>>
{

    private OnItemClickListener clickListener;

    interface OnItemClickListener {
        /**
         * Called when a submission is clicked.
         */
        void onItemClick(Submission submission, View submissionItemView, long submissionId);
    }

    public SubRedditSubmissionsAdapter() {
        setHasStableIds(true);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        clickListener = listener;
    }

    @Override
    public void accept(List<Submission> submissions) {
        // TODO: 29/01/17 Use DiffUtils / SortedList instead of invalidating the entire list.
        updateData(submissions);
    }

    @Override
    protected SubmissionViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return SubmissionViewHolder.create(parent);
    }

    @Override
    public void onBindViewHolder(SubmissionViewHolder holder, int position) {
        Submission submission = getItem(position);
        holder.bind(submission);

        holder.itemView.setOnClickListener(v -> {
            clickListener.onItemClick(submission, holder.itemView, getItemId(position));
        });
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    public static class SubmissionViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.submission_item_icon) ImageView thumbnailView;
        @BindView(R.id.submission_item_title) TextView titleView;
        @BindView(R.id.submission_item_subtitle) TextView subTitleView;

        @BindColor(R.color.gray_100) int defaultIconsTintColor;

        public SubmissionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Submission submission) {
            //if (submission.getTitle().contains("Already drunk")) {
            //    Timber.d("-------------------------------------------");
            //    Timber.i("%s", submission.getTitle());
            //    Timber.i("Hint: %s, Thumb type: %s", submission.getPostHint(), submission.getThumbnailType());
            //    Timber.i("Thumb: %s", submission.getThumbnail());
            //    Timber.i("Thumbs: %s", submission.getThumbnails());
            //}

            switch (submission.getThumbnailType()) {
                case NSFW:
                    // TODO: 08/02/17 NSFW thumbnails
                case SELF:
                    loadStaticThumbnail(R.drawable.ic_text_fields_black_24dp);
                    thumbnailView.setVisibility(View.VISIBLE);
                    break;

                case DEFAULT:
                    // Reddit couldn't create a thumbnail. Has to be a URL submission.
                    loadStaticThumbnail(R.drawable.ic_link_black_24dp);

                    // Get the high-res image for this link.
                    // TODO: 19/02/17 Get thumbnails provided by Reddit.
                    // TODO: 19/02/17 Don't download high-res images on low-data mode.
                    if (submission.getThumbnails() != null) {
                        // Resize image.
                        loadThumbnailFromUrl(submission.getThumbnails().getSource().getUrl());
                    }
                    thumbnailView.setVisibility(View.VISIBLE);
                    break;

                case URL:
                    // TODO: 05/04/17 Reddit sometimes sends literal "image" as the thumbnail. Use thumbnail variations in that case.
                    loadThumbnailFromUrl(submission.getThumbnail());
                    thumbnailView.setVisibility(View.VISIBLE);
                    break;

                case NONE:
                    thumbnailView.setVisibility(View.GONE);
                    break;
            }

            //noinspection deprecation
            titleView.setText(Html.fromHtml(submission.getTitle()));
            subTitleView.setText(subTitleView.getResources().getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));
        }

        private void loadStaticThumbnail(@DrawableRes int iconRes) {
            thumbnailView.setBackgroundResource(R.drawable.background_submission_self_thumbnail);
            thumbnailView.setImageResource(iconRes);
            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            thumbnailView.setColorFilter(defaultIconsTintColor);
        }

        private void loadThumbnailFromUrl(String thumbnailUrl) {
            Glide.with(itemView.getContext()).load(thumbnailUrl)
                    .bitmapTransform(new GlideCircularTransformation(itemView.getContext()))
                    .into(thumbnailView);

            thumbnailView.setColorFilter(null);
            thumbnailView.setBackground(null);
            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        public static SubmissionViewHolder create(ViewGroup parent) {
            return new SubmissionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_submission, parent, false));
        }
    }

}
