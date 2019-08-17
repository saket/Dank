package me.saket.dank.ui.splash;

import android.os.Bundle;
import androidx.annotation.Nullable;

import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.subreddit.SubredditActivity;

public class SplashActivity extends DankActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startActivity(SubredditActivity.intent(this));
    finish();
  }
}
