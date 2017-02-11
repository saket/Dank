package me.saket.dank.ui.subreddits;

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
import me.saket.dank.R;
import me.saket.dank.utils.GlideCircularTransformation;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import rx.functions.Action1;

public class SubRedditSubmissionsAdapter extends RecyclerViewArrayAdapter<Submission, SubRedditSubmissionsAdapter.SubmissionViewHolder>
        implements Action1<List<Submission>>
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
    public void call(List<Submission> submissions) {
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
        @BindView(R.id.submission_item_icon) ImageView iconView;
        @BindView(R.id.submission_item_title) TextView titleView;
        @BindView(R.id.submission_item_subtitle) TextView subTitleView;

        @BindColor(R.color.gray_100) int defaultIconsTintColor;

        public SubmissionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Submission submission) {
            switch (submission.getThumbnailType()) {
                case NSFW:
                    // TODO: 08/02/17 NSFW thumbnails
                case SELF:
                    iconView.setBackgroundResource(R.drawable.background_submission_self_thumbnail);
                    iconView.setImageResource(R.drawable.ic_text_fields_black_24dp);
                    iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    iconView.setColorFilter(defaultIconsTintColor);
                    iconView.setVisibility(View.VISIBLE);
                    break;

                case DEFAULT:
                    // Reddit couldn't create a thumbnail. Has to be a URL submission.
                    iconView.setBackgroundResource(R.drawable.background_submission_self_thumbnail);
                    iconView.setImageResource(R.drawable.ic_link_black_24dp);
                    iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    iconView.setColorFilter(defaultIconsTintColor);
                    iconView.setVisibility(View.VISIBLE);
                    break;

                case URL:
                    Glide.with(itemView.getContext())
                            .load(submission.getThumbnail())
                            .bitmapTransform(new GlideCircularTransformation(itemView.getContext()))
                            .into(iconView);

                    iconView.setColorFilter(null);
                    iconView.setBackground(null);
                    iconView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iconView.setVisibility(View.VISIBLE);
                    break;

                case NONE:
                    iconView.setVisibility(View.GONE);
                    break;
            }

            //noinspection deprecation
            titleView.setText(Html.fromHtml(submission.getTitle()));
            subTitleView.setText(subTitleView.getResources().getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));
        }

        public static SubmissionViewHolder create(ViewGroup parent) {
            return new SubmissionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_submission, parent, false));
        }
    }

}
