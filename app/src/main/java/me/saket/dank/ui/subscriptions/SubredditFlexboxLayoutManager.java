package me.saket.dank.ui.subscriptions;

import android.content.Context;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;

public class SubredditFlexboxLayoutManager extends FlexboxLayoutManager {

  public SubredditFlexboxLayoutManager(Context context) {
    super(context);
    setFlexDirection(FlexDirection.ROW);
    setFlexWrap(FlexWrap.WRAP);
    setAlignItems(AlignItems.STRETCH);
  }
}
