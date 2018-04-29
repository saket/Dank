package me.saket.dank.ui.preferences;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;

import me.saket.dank.urlparser.Link;

public interface PreferenceButtonClickHandler {

  void expandNestedPage(@LayoutRes int nestedLayoutRes, RecyclerView.ViewHolder viewHolderToExpand);

  void show(MultiOptionPreferencePopup.Builder popupBuilder, RecyclerView.ViewHolder viewHolder);

  void openIntent(Intent intent);

  void openLink(Link link);
}
