package me.saket.dank.ui.preferences;

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import me.saket.dank.data.NetworkStrategy;

public interface PreferenceButtonClickHandler {

  void expandNestedPage(@LayoutRes int nestedLayoutRes, RecyclerView.ViewHolder viewHolderToExpand);

  void show(PreferenceMultiOptionPopup.Builder<NetworkStrategy> popupBuilder, RecyclerView.ViewHolder viewHolder);
}
