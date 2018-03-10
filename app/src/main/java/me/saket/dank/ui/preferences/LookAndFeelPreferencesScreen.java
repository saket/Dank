package me.saket.dank.ui.preferences;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;

public class LookAndFeelPreferencesScreen extends LinearLayout {

  public LookAndFeelPreferencesScreen(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    LayoutInflater.from(context).inflate(R.layout.view_user_preferences_look_and_feel, this, true);
    setOrientation(VERTICAL);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();

    ButterKnife.bind(this, this);
    Dank.dependencyInjector().inject(this);
  }
}
