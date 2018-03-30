package me.saket.dank.ui.appshortcuts;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class AppShortcutsAdapter extends RecyclerViewArrayAdapter<AppShortcut, AppShortcutsAdapter.AppShortcutViewHolder>
    implements Consumer<Pair<List<AppShortcut>, DiffUtil.DiffResult>>
{

  private final Relay<AppShortcut> deleteClicks = PublishRelay.create();

  @Inject
  public AppShortcutsAdapter() {
    setHasStableIds(true);
  }

  @CheckResult
  public Observable<AppShortcut> streamDeleteClicks() {
    return deleteClicks;
  }

  @Override
  protected AppShortcutViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
    AppShortcutViewHolder holder = AppShortcutViewHolder.create(inflater, parent);
    holder.deleteButton.setOnClickListener(o -> deleteClicks.accept(holder.shortcut));
    return holder;
  }

  @Override
  public void onBindViewHolder(@NonNull AppShortcutViewHolder holder, int position) {
    holder.render(getItem(position));
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).label().hashCode();
  }

  @Override
  public void accept(Pair<List<AppShortcut>, DiffUtil.DiffResult> pair) {
    updateData(pair.first());
    pair.second().dispatchUpdatesTo(this);
  }

  public static class AppShortcutViewHolder extends RecyclerView.ViewHolder {
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

    public void render(AppShortcut shortcut) {
      this.shortcut = shortcut;
      labelView.setText(this.shortcut.label());
    }
  }
}
