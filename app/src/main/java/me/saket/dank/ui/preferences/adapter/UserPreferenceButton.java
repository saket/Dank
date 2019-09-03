package me.saket.dank.ui.preferences.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.events.UserPreferenceButtonClickEvent;
import me.saket.dank.ui.preferences.events.UserPreferenceClickListener;
import me.saket.dank.utils.Optional;

public interface UserPreferenceButton {

  @AutoValue
  abstract class UiModel implements UserPreferencesScreenUiModel {

    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.BUTTON;
    }

    public abstract String title();

    public abstract Optional<String> summary();

    public abstract UserPreferenceClickListener clickListener();

    public static UiModel create(String title, String summary, UserPreferenceClickListener clickListener) {
      return new AutoValue_UserPreferenceButton_UiModel(title.hashCode(), title, Optional.of(summary), clickListener);
    }

    public static UiModel create(String title, UserPreferenceClickListener clickListener) {
      return new AutoValue_UserPreferenceButton_UiModel(title.hashCode(), title, Optional.empty(), clickListener);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.item_preferences_button_title) TextView titleView;
    @BindView(R.id.item_preferences_button_summary) TextView summaryView;

    private UiModel uiModel;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void set(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      titleView.setText(uiModel.title());

      summaryView.setVisibility(uiModel.summary()
          .map(o -> View.VISIBLE)
          .orElse(View.GONE));
      uiModel.summary().ifPresent(summary -> summaryView.setText(summary));
    }
  }

  class Adapter implements UserPreferencesScreenUiModel.ChildAdapter<UiModel, ViewHolder> {

    PublishRelay<UserPreferenceButtonClickEvent> itemClicks = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.list_item_preference_button, parent, false));
      holder.itemView.setOnClickListener(o -> itemClicks.accept(UserPreferenceButtonClickEvent.create(
          holder.uiModel.clickListener(),
          holder)));
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.set(uiModel);
      holder.render();
    }
  }
}
