package me.saket.dank.ui.preferences.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Optional;

public interface UserPreferenceSectionHeader  {

  @AutoValue
  abstract class UiModel implements UserPreferencesScreenUiModel {
    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.SECTION_HEADER;
    }

    public abstract String label();

    public abstract Optional<String> description();

    public static UiModel create(String label, String description) {
      return new AutoValue_UserPreferenceSectionHeader_UiModel(label.hashCode(), label, Optional.of(description));
    }

    public static UiModel create(String label) {
      return new AutoValue_UserPreferenceSectionHeader_UiModel(label.hashCode(), label, Optional.empty());
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_preferences_sectionheader_label) TextView labelView;
    @BindView(R.id.item_preferences_sectionheader_description) TextView descriptionView;

    private UiModel uiModel;

    protected ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }

    public void set(UiModel uiModel) {
      this.uiModel = uiModel;
    }

    public void render() {
      labelView.setText(uiModel.label());
      uiModel.description().ifPresent(description -> descriptionView.setText(description));
      descriptionView.setVisibility(uiModel.description().isPresent() ? View.VISIBLE : View.GONE);
    }
  }

  class Adapter implements UserPreferencesScreenUiModel.ChildAdapter<UiModel, ViewHolder> {
    @Inject
    public Adapter() {
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_preference_section_header, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.set(uiModel);
      holder.render();
    }
  }
}
