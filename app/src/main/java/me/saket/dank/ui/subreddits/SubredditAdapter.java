package me.saket.dank.ui.subreddits;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.data.SubredditSubscription;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import rx.functions.Action1;

/**
 * Adapter for displaying a list of subreddits.
 */
public class SubredditAdapter extends RecyclerViewArrayAdapter<SubredditSubscription, SubredditAdapter.SubredditViewHolder> implements Action1<List<SubredditSubscription>> {

    interface OnSubredditClickListener {
        void onClickSubreddit(String subredditName, View subredditItemView);
    }

    private OnSubredditClickListener clickListener;

    public SubredditAdapter() {
        setHasStableIds(true);
    }

    public void setOnSubredditClickListener(OnSubredditClickListener listener) {
        clickListener = listener;
    }

    @Override
    public void call(List<SubredditSubscription> subscriptions) {
        updateData(subscriptions);
    }

    @Override
    protected SubredditViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return SubredditViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(SubredditViewHolder holder, int position) {
        SubredditSubscription subSubscription = getItem(position);
        holder.bind(subSubscription);

        holder.itemView.setOnClickListener(itemView -> {
            clickListener.onClickSubreddit(subSubscription.name(), itemView);
        });
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    static class SubredditViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_subreddit_name) Button subredditNameView;

        public static SubredditViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new SubredditViewHolder(inflater.inflate(R.layout.list_item_subreddit, parent, false));
        }

        public SubredditViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(SubredditSubscription subreddit) {
            subredditNameView.setText(subreddit.name());
        }

    }

}
