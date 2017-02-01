package me.saket.dank.ui.subreddits;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.dean.jraw.models.Submission;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
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
        @BindView(R.id.submission_item_title) TextView titleView;
        @BindView(R.id.submission_item_subtitle) TextView subTitleView;

        public SubmissionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Submission submission) {
            titleView.setText(submission.getTitle());
            subTitleView.setText(subTitleView.getResources().getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));
        }

        public static SubmissionViewHolder create(ViewGroup parent) {
            return new SubmissionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_submission, parent, false));
        }

    }

}
