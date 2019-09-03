package me.saket.dank.ui.submission;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import me.saket.dank.R;
import me.saket.dank.utils.Pair;

/**
 * This exists as an Activity just to do shared element transitions.
 */
public class LockedSubmissionDialogActivity extends ArchivedSubmissionDialogActivity {

  @BindView(R.id.archivedsubmission_dialog_icon) ImageView iconView;
  @BindView(R.id.archivedsubmission_dialog_title) TextView titleView;
  @BindView(R.id.archivedsubmission_dialog_description) TextView descriptionView;

  public static Intent intent(Context context) {
    return new Intent(context, LockedSubmissionDialogActivity.class);
  }

  public static Pair<Intent, ActivityOptions> intentWithFabTransform(
      Activity activity,
      FloatingActionButton fab,
      @ColorRes int fabColorRes,
      @DrawableRes int fabIconRes)
  {
    Intent intent = intent(activity);
    return addFabTransformAndActivityOptions(intent, activity, fab, fabColorRes, fabIconRes);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    iconView.setImageResource(R.drawable.ic_lock_32dp);
    iconView.setContentDescription(getString(R.string.locked_submission_icon_cd));
    titleView.setText(R.string.locked_submission_title);
    descriptionView.setText(R.string.locked_submission_description);
  }
}
