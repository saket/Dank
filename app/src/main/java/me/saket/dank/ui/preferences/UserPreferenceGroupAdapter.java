package me.saket.dank.ui.preferences;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
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

public class UserPreferenceGroupAdapter extends RecyclerViewArrayAdapter<UserPreferenceGroup, UserPreferenceGroupAdapter.PreferenceGroupViewHolder> {

  private OnClickPreferenceGroupListener clickListener;

  interface OnClickPreferenceGroupListener {
    void onClickPreferenceGroup(UserPreferenceGroup preferenceGroup, View itemView, long groupId);
  }

  public UserPreferenceGroupAdapter(@Nullable List<UserPreferenceGroup> items) {
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
  public long getItemId(int position) {
    return position;
  }

  @Override
  public void onBindViewHolder(PreferenceGroupViewHolder holder, int position) {
    UserPreferenceGroup preferenceGroup = getItem(position);
    holder.bind(preferenceGroup);
    holder.itemView.setOnClickListener(__ -> clickListener.onClickPreferenceGroup(preferenceGroup, holder.itemView, getItemId(position)));
  }

  static class PreferenceGroupViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_preferencegroup_icon) ImageView iconView;
    @BindView(R.id.item_preferencegroup_title) TextView titleView;
    @BindView(R.id.item_preferencegroup_summary) TextView summaryView;

    public static PreferenceGroupViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new PreferenceGroupViewHolder(inflater.inflate(R.layout.list_item_preference_group, parent, false));
    }

    public PreferenceGroupViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void bind(UserPreferenceGroup preferenceGroup) {
      iconView.setImageResource(preferenceGroup.iconRes);
      iconView.setContentDescription(iconView.getResources().getText(preferenceGroup.titleRes));
      titleView.setText(preferenceGroup.titleRes);
      summaryView.setText(preferenceGroup.summaryRes);
    }
  }
}
