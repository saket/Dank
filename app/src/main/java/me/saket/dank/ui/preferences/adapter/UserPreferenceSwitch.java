package me.saket.dank.ui.preferences.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.events.UserPreferenceSwitchToggleEvent;
import me.saket.dank.utils.Optional;

public interface UserPreferenceSwitch {

  @AutoValue
  abstract class UiModel implements UserPreferencesScreenUiModel {

    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.SWITCH;
    }

    public abstract String title();

    public abstract Optional<String> summary();

    public abstract Preference<Boolean> preference();

    public static UiModel create(String title, String summary, Preference<Boolean> preference) {
      return new AutoValue_UserPreferenceSwitch_UiModel(preference.key().hashCode(), title, Optional.of(summary), preference);
    }

    public static UiModel create(String title, Preference<Boolean> preference) {
      return new AutoValue_UserPreferenceSwitch_UiModel(preference.key().hashCode(), title, Optional.empty(), preference);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_preferences_switch_title) Switch titleSwitchView;
    @BindView(R.id.item_preferences_switch_summary) TextView summaryView;

    private UiModel uiModel;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);

      itemView.setOnClickListener(o -> titleSwitchView.performClick());
    }

    public void set(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      titleSwitchView.setText(uiModel.title());
      uiModel.summary().ifPresent(summary -> summaryView.setText(summary));

      titleSwitchView.setGravity(uiModel.summary().isPresent() ? Gravity.TOP : Gravity.CENTER_VERTICAL);
      summaryView.setVisibility(uiModel.summary().isPresent() ? View.VISIBLE : View.GONE);
    }
  }

  class Adapter implements UserPreferencesScreenUiModel.ChildAdapter<UiModel, ViewHolder> {
    public PublishRelay<UserPreferenceSwitchToggleEvent> itemClicks = PublishRelay.create();

    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      ViewHolder holder = new ViewHolder(inflater.inflate(R.layout.list_item_preference_switch, parent, false));
      //noinspection CodeBlock2Expr
      holder.titleSwitchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
        itemClicks.accept(UserPreferenceSwitchToggleEvent.create(holder.uiModel.preference(), isChecked));
      });
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.set(uiModel);
      holder.render();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
