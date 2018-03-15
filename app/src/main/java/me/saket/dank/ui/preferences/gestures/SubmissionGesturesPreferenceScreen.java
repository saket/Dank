package me.saket.dank.ui.preferences.gestures;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import me.saket.dank.R;
import me.saket.dank.ui.preferences.UserPreferenceNestedScreen;

public class SubmissionGesturesPreferenceScreen extends RelativeLayout implements UserPreferenceNestedScreen {

  private Toolbar toolbar;

  public SubmissionGesturesPreferenceScreen(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    toolbar = findViewById(R.id.userpreferences_gestures_toolbar);
    toolbar.setTitle(R.string.userprefs_customize_submission_gestures);
  }

  @Override
  public void setNavigationOnClickListener(OnClickListener listener) {
    toolbar.setNavigationOnClickListener(listener);
  }
}
