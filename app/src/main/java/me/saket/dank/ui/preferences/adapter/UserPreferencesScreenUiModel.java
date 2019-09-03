package me.saket.dank.ui.preferences.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

public interface UserPreferencesScreenUiModel {

  enum Type {
    SECTION_HEADER,
    SWITCH,
    BUTTON
  }

  long adapterId();

  Type type();

  interface ChildAdapter<T extends UserPreferencesScreenUiModel, VH extends RecyclerView.ViewHolder> {
    VH onCreateViewHolder(LayoutInflater inflater, ViewGroup parent);

    void onBindViewHolder(VH holder, T uiModel);
  }
}
