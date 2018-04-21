package me.saket.dank.ui.preferences;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

public interface PreferenceButtonClickHandler {

  void expandNestedPage(@LayoutRes int nestedLayoutRes, RecyclerView.ViewHolder viewHolderToExpand);

  void show(PreferenceMultiOptionPopup.Builder popupBuilder, RecyclerView.ViewHolder viewHolder);
}
