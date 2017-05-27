package me.saket.dank.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import butterknife.ButterKnife;
import me.saket.dank.R;

public class PlaygroundActivity extends DankPullCollapsibleActivity {

  public static void start(Context context) {
    context.startActivity(new Intent(context, PlaygroundActivity.class));
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_playground);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    setupContentExpandablePage(ButterKnife.findById(this, R.id.playground_root));
    expandFromBelowToolbar();
  }
}
