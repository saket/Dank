package me.saket.dank.ui.preferences;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class PreferencesAdapter extends RecyclerViewArrayAdapter<DankPreferenceGroup, PreferencesAdapter.PreferenceGroupViewHolder> {

    private OnClickPreferenceGroupListener clickListener;

    interface OnClickPreferenceGroupListener {
        void onClickPreferenceGroup(DankPreferenceGroup preferenceGroup, View submissionItemView, long submissionId);
    }

    public PreferencesAdapter(@Nullable List<DankPreferenceGroup> items) {
        super(items);
        setHasStableIds(true);
    }

    public void setOnPreferenceGroupClickListener(OnClickPreferenceGroupListener listener) {
        clickListener = listener;
    }

    @Override
    protected PreferenceGroupViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        return PreferenceGroupViewHolder.create(inflater, parent);
    }

    @Override
    public void onBindViewHolder(PreferenceGroupViewHolder holder, int position) {
        DankPreferenceGroup preferenceGroup = getItem(position);
        holder.bind(preferenceGroup);
        holder.itemView.setOnClickListener(__ -> clickListener.onClickPreferenceGroup(preferenceGroup, holder.itemView, getItemId(position)));
    }

    static class PreferenceGroupViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.item_preferencegroup_icon) ImageView iconView;
        @BindView(R.id.item_preferencegroup_title) TextView titleView;
        @BindView(R.id.item_preferencegroup_subtitle) TextView subtitleView;

        public static PreferenceGroupViewHolder create(LayoutInflater inflater, ViewGroup parent) {
            return new PreferenceGroupViewHolder(inflater.inflate(R.layout.list_item_preference_group, parent, false));
        }

        public PreferenceGroupViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(DankPreferenceGroup preferenceGroup) {
            iconView.setImageResource(preferenceGroup.iconResId());
            iconView.setContentDescription(iconView.getResources().getText(preferenceGroup.titleResId()));
            titleView.setText(preferenceGroup.titleResId());
            subtitleView.setText(preferenceGroup.subtitleResId());
        }

    }

}
