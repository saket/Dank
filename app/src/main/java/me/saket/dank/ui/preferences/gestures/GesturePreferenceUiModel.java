package me.saket.dank.ui.preferences.gestures;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

public interface GesturePreferenceUiModel {

  enum Type {
    SUBMISSION_PREVIEW,
    SECTION_HEADER,
    SWIPE_ACTION,
  }

  long adapterId();

  Type type();

  interface ChildAdapter<T extends GesturePreferenceUiModel, VH extends RecyclerView.ViewHolder> {
    VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);

    void onBindViewHolder(VH holder, T uiModel);

    void onBindViewHolder(VH holder, T uiModel, List<Object> payloads);
  }
}
