package me.saket.dank.ui.submission;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.widgets.FabTransform;

public class ArchivedSubmissionDialogActivity extends DankActivity {

  @BindView(R.id.archivedsubmission_dialog_container) ViewGroup dialogContainer;

  public static Intent intent(Context context) {
    return new Intent(context, ArchivedSubmissionDialogActivity.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dialog_archived_submission);
    ButterKnife.bind(this);

    FabTransform.setupActivityTransition(this, dialogContainer);

    // For some reason, I'm unable to override the window's background color in this Activity's parent style.
    getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.dialog_like_activity_window_background)));

    // TODO: window background
    // TODO: status bar tint (get from caller)
  }

  @OnClick(R.id.archivedsubmission_background)
  public void dismiss() {
    finishAfterTransition();
  }

  @Override
  public void onBackPressed() {
    dismiss();
  }
}
