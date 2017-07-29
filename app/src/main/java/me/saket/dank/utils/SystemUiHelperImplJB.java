package me.saket.dank.utils;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class SystemUiHelperImplJB extends SystemUiHelper.SystemUiHelperImpl implements View.OnSystemUiVisibilityChangeListener {

  protected final View mDecorView;

  SystemUiHelperImplJB(Activity activity, int level, int flags, SystemUiHelper.OnSystemUiVisibilityChangeListener onSystemUiVisibilityChangeListener) {
    super(activity, level, flags, onSystemUiVisibilityChangeListener);
    mDecorView = activity.getWindow().getDecorView();
    mDecorView.setOnSystemUiVisibilityChangeListener(this);
  }

  protected int createShowFlags() {
    int flag = View.SYSTEM_UI_FLAG_VISIBLE;
    if (mLevel >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
      flag |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

      if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
        flag |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
      }
    }
    return flag;
  }

  protected int createHideFlags() {
    int flag = View.SYSTEM_UI_FLAG_LOW_PROFILE;
    if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
      flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }

    if (mLevel >= SystemUiHelper.LEVEL_HIDE_STATUS_BAR) {
      flag |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
          | View.SYSTEM_UI_FLAG_FULLSCREEN;

      if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
        flag |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
      }
    }
    return flag;
  }

  protected void onSystemUiShown() {
    if (mLevel == SystemUiHelper.LEVEL_LOW_PROFILE) {
      // Manually show the action bar when in low profile mode.
      ActionBar ab = mActivity.getActionBar();
      if (ab != null) {
        ab.show();
      }
    }
    setIsShowing(true);
  }

  protected void onSystemUiHidden() {
    if (mLevel == SystemUiHelper.LEVEL_LOW_PROFILE) {
      // Manually hide the action bar when in low profile mode.
      ActionBar ab = mActivity.getActionBar();
      if (ab != null) {
        ab.hide();
      }
    }
    setIsShowing(false);
  }

  protected int createTestFlags() {
    if (mLevel >= SystemUiHelper.LEVEL_LEAN_BACK) {
      // Intentionally override test flags.
      return View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
    }
    return View.SYSTEM_UI_FLAG_LOW_PROFILE;
  }

  @Override
  void show() {
    mDecorView.setSystemUiVisibility(createShowFlags());
  }

  @Override
  void hide() {
    mDecorView.setSystemUiVisibility(createHideFlags());
  }

  @Override
  public final void onSystemUiVisibilityChange(int visibility) {
    if ((visibility & createTestFlags()) != 0) {
      onSystemUiHidden();
    } else {
      onSystemUiShown();
    }
  }
}
