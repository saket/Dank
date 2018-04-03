package me.saket.dank.ui.preferences;

import android.support.annotation.LayoutRes;

public interface PreferenceButtonClickHandler {

  void expandNestedPage(@LayoutRes int nestedLayoutRes, int positionOfItemToExpand, long idOfItemToExpand);
}
