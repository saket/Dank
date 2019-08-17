package me.saket.dank.ui.preferences.adapter;

import androidx.annotation.CheckResult;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.ui.preferences.events.UserPreferenceButtonClickEvent;
import me.saket.dank.ui.preferences.events.UserPreferenceSwitchToggleEvent;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;

public class UserPreferencesAdapter extends RecyclerViewArrayAdapter<UserPreferencesScreenUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<UserPreferencesScreenUiModel>, DiffUtil.DiffResult>>
{

  private static final UserPreferencesScreenUiModel.Type[] VIEW_TYPES = UserPreferencesScreenUiModel.Type.values();
  private final Map<UserPreferencesScreenUiModel.Type, UserPreferencesScreenUiModel.ChildAdapter> childAdapters;
  private final UserPreferenceButton.Adapter buttonAdapter;
  private final UserPreferenceSwitch.Adapter switchAdapter;

  @Inject
  public UserPreferencesAdapter(
      UserPreferenceSectionHeader.Adapter headerAdapter,
      UserPreferenceSwitch.Adapter switchAdapter,
      UserPreferenceButton.Adapter buttonAdapter)
  {
    this.buttonAdapter = buttonAdapter;
    this.switchAdapter = switchAdapter;

    childAdapters = new HashMap<>();
    childAdapters.put(UserPreferencesScreenUiModel.Type.SECTION_HEADER, headerAdapter);
    childAdapters.put(UserPreferencesScreenUiModel.Type.SWITCH, switchAdapter);
    childAdapters.put(UserPreferencesScreenUiModel.Type.BUTTON, buttonAdapter);

    setHasStableIds(true);
  }

  @Override
  public int getItemViewType(int position) {
    return getItem(position).type().ordinal();
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).adapterId();
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    return childAdapters.get(VIEW_TYPES[viewType]).onCreateViewHolder(inflater, parent);
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    //noinspection unchecked
    childAdapters.get(VIEW_TYPES[holder.getItemViewType()]).onBindViewHolder(holder, getItem(position));
  }

  @Override
  public void accept(Pair<List<UserPreferencesScreenUiModel>, DiffUtil.DiffResult> pair) throws Exception {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }

  @CheckResult
  public Observable<UserPreferenceButtonClickEvent> streamButtonClicks() {
    return buttonAdapter.itemClicks;
  }

  @CheckResult
  public Observable<UserPreferenceSwitchToggleEvent> streamSwitchToggles() {
    return switchAdapter.getItemClicks();
  }
}
