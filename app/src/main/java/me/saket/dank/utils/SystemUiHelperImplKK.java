package me.saket.dank.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.KITKAT)
class SystemUiHelperImplKK extends SystemUiHelperImplJB {

  SystemUiHelperImplKK(Activity activity, int level, int flags,
      SystemUiHelper.OnSystemUiVisibilityChangeListener onSystemUiVisibilityChangeListener) {
    super(activity, level, flags, onSystemUiVisibilityChangeListener);
  }

  @Override
  protected int createHideFlags() {
    int flag = super.createHideFlags();

    if (mLevel == SystemUiHelper.LEVEL_IMMERSIVE) {
      // If the client requested immersive mode, and we're on Android 4.4
      // or later, add relevant flags. Applying HIDE_NAVIGATION without
      // IMMERSIVE prevents the activity from accepting all touch events,
      // so we only do this on Android 4.4 and later (where IMMERSIVE is
      // present).
      flag |= ((mFlags & SystemUiHelper.FLAG_IMMERSIVE_STICKY) != 0)
          ? View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
          : View.SYSTEM_UI_FLAG_IMMERSIVE;
    }

    return flag;
  }
}
