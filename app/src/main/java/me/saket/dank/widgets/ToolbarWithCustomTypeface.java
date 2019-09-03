package me.saket.dank.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.Relay;

public class ToolbarWithCustomTypeface extends Toolbar {

  private Relay<TextView> titleView;
  private boolean titleFound;

  public ToolbarWithCustomTypeface(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    findTitle();
  }

  private void findTitle() {
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      if (view != null && view instanceof TextView) {
        if (((TextView) view).getText().equals(getTitle())) {
          titleView().accept(((TextView) view));
          titleFound = true;
          return;
        }
      }
    }
  }

  @Override
  public void addView(View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    if (!titleFound) {
      findTitle();
    }
  }

  @Override
  public void setTitle(CharSequence title) {
    super.setTitle(title);
    findTitle();
  }

  public Relay<TextView> titleView() {
    if (titleView == null) {
      titleView = BehaviorRelay.create();
    }
    return titleView;
  }
}
