package me.saket.dank.ui.subreddits;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.dean.jraw.models.Subreddit;

import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

/**
 *
 */
public class SubredditAdapter extends RecyclerViewArrayAdapter<Subreddit, SubredditAdapter.SubredditViewHolder> {

    @Override
    protected SubredditViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return SubredditViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(SubredditViewHolder holder, int position) {

    }

    static class SubredditViewHolder extends RecyclerView.ViewHolder{

        public static SubredditViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new SubredditViewHolder(inflater.inflate(R.layout.list_item_subreddit, parent, false));
        }

        public SubredditViewHolder(View itemView) {
            super(itemView);
        }

    }

}
