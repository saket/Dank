package me.saket.dank.ui.preferences.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

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

    public abstract String summary();

    public static UiModel create(String title, String summary) {
      return new AutoValue_UserPreferenceButton_UiModel(title.hashCode(), title, summary);
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
      summaryView.setText(uiModel.summary());
    }
  }

  class Adapter implements UserPreferencesScreenUiModel.ChildAdapter<UiModel, ViewHolder> {
    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_preference_button, parent, false));
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
