package me.saket.dank.ui.appshortcuts;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RecyclerViewArrayAdapter;
import me.saket.dank.utils.lifecycle.LifecycleStreams;

public class AppShortcutsAdapter extends RecyclerViewArrayAdapter<AppShortcutScreenUiModel, RecyclerView.ViewHolder>
    implements Consumer<Pair<List<AppShortcutScreenUiModel>, DiffUtil.DiffResult>>
{

  private static final Object NOTHING = LifecycleStreams.NOTHING;
  public static final long ID_ADD_NEW = -99L;
  private static final int VIEW_TYPE_APP_SHORTCUT = 0;
  private static final int VIEW_TYPE_PLACEHOLDER = 1;

  private final Relay<AppShortcut> deleteClicks = PublishRelay.create();
  private final Relay<Object> addClicks = PublishRelay.create();

  @Inject
  public AppShortcutsAdapter() {
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<AppShortcut> streamDeleteClicks() {
    return deleteClicks;
  }

  @Override
  protected RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    if (viewType == VIEW_TYPE_APP_SHORTCUT) {
      AppShortcutViewHolder holder = AppShortcutViewHolder.create(inflater, parent);
      holder.deleteButton.setOnClickListener(o -> deleteClicks.accept(holder.shortcut));
      return holder;

    } else if (viewType == VIEW_TYPE_PLACEHOLDER) {
      PlaceholderViewHolder holder = PlaceholderViewHolder.create(inflater, parent);
      holder.addButton.setOnClickListener(o -> addClicks.accept(NOTHING));
      return holder;

    } else {
      throw new AssertionError();
    }
  }

  @Override
  public long getItemId(int position) {
    AppShortcutScreenUiModel uiModel = getItem(position);
    return uiModel.adapterId();
  }

  @Override
  public int getItemViewType(int position) {
    AppShortcutScreenUiModel uiModel = getItem(position);

    if (uiModel instanceof AppShortcut) {
      return VIEW_TYPE_APP_SHORTCUT;

    } else if (uiModel instanceof AppShortcutPlaceholderUiModel) {
      return VIEW_TYPE_PLACEHOLDER;

    } else {
      throw new AssertionError();
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof AppShortcutViewHolder) {
      ((AppShortcutViewHolder) holder).set((AppShortcut) getItem(position));
      ((AppShortcutViewHolder) holder).render();
    }
  }

  @Override
  public void accept(Pair<List<AppShortcutScreenUiModel>, DiffUtil.DiffResult> pair) {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }

  public Observable<Object> streamAddClicks() {
    return addClicks;
  }

  static class AppShortcutViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.appshortcut_label) TextView labelView;
    @BindView(R.id.appshortcut_delete) ImageButton deleteButton;
    private AppShortcut shortcut;

    public AppShortcutViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public static AppShortcutViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new AppShortcutViewHolder(inflater.inflate(R.layout.list_item_app_shortcut, parent, false));
    }

    public void set(AppShortcut shortcut) {
      this.shortcut = shortcut;
    }

    public void render() {
      labelView.setText(labelView.getResources().getString(R.string.subreddit_name_r_prefix, shortcut.label()));
    }
  }

  static class PlaceholderViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.appshortcut_placeholder_add) Button addButton;

    static PlaceholderViewHolder create(LayoutInflater inflater, ViewGroup parent) {
      return new PlaceholderViewHolder(inflater.inflate(R.layout.list_item_app_shortcut_placeholder, parent, false));
    }

    public PlaceholderViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
