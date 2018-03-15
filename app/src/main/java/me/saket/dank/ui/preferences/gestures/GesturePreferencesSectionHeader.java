package me.saket.dank.ui.preferences.gestures;

import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.auto.value.AutoValue;

import java.util.List;

import me.saket.dank.R;

public interface GesturePreferencesSectionHeader {

  @AutoValue
  abstract class UiModel implements GesturePreferenceUiModel {
    @Override
    public abstract long adapterId();

    @Override
    public Type type() {
      return Type.SECTION_HEADER;
    }

    @StringRes
    public abstract int labelRes();

    public static UiModel create(@StringRes int labelRes) {
      return new AutoValue_GesturePreferencesSectionHeader_UiModel(labelRes, labelRes);
    }
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    private TextView labelView;

    public ViewHolder(View itemView) {
      super(itemView);
      labelView = itemView.findViewById(R.id.item_gesturepreferences_sectionheader_label);
    }

    public void render(UiModel uiModel) {
      labelView.setText(uiModel.labelRes());
    }
  }

  class Adapter implements GesturePreferenceUiModel.ChildAdapter<UiModel, ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      return new ViewHolder(inflater.inflate(R.layout.list_item_gesture_preference_section_header, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.render(uiModel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      throw new UnsupportedOperationException();
    }
  }
}
