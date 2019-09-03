package me.saket.dank.ui.submission;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.Pair;
import me.saket.dank.widgets.FabTransform;

/**
 * This exists as an Activity just to do shared element transitions.
 */
public class ArchivedSubmissionDialogActivity extends DankActivity {

  private static final String SHARED_ELEMENT_TRANSITION_NAME = "sharedElement:ArchivedSubmissionDialogActivity";

  @BindView(R.id.archivedsubmission_dialog_container) ViewGroup dialogContainer;

  public static Intent intent(Context context) {
    return new Intent(context, ArchivedSubmissionDialogActivity.class);
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

  public static Pair<Intent, ActivityOptions> addFabTransformAndActivityOptions(
      Intent intent,
      Activity activity,
      FloatingActionButton fab,
      @ColorRes int fabColorRes,
      @DrawableRes int fabIconRes)
  {
    FabTransform.addExtras(intent, ContextCompat.getColor(activity, fabColorRes), fabIconRes);
    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, fab, SHARED_ELEMENT_TRANSITION_NAME);
    return Pair.create(intent, options);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dialog_archived_submission);
    ButterKnife.bind(this);

    // For some reason, I'm unable to override the window's background color in this Activity's parent Xml style.
    int windowBackgroundColor = ContextCompat.getColor(this, R.color.dialog_like_activity_window_background);
    ColorDrawable windowBackground = new ColorDrawable(windowBackgroundColor);
    getWindow().setBackgroundDrawable(windowBackground);

    if (FabTransform.hasActivityTransition(this)) {
      dialogContainer.setTransitionName(SHARED_ELEMENT_TRANSITION_NAME);
      FabTransform.setupActivityTransition(this, dialogContainer);
    } else {
      overridePendingTransition(R.anim.dialog_fade_in, 0);
    }

    dialogContainer.setClipToOutline(true);
  }

  @OnClick({ R.id.archivedsubmission_background, R.id.archivedsubmission_dismiss_button })
  public void dismiss() {
    if (FabTransform.hasActivityTransition(this)) {
      finishAfterTransition();
    } else {
      finish();
      overridePendingTransition(0, R.anim.dialog_fade_out);
    }
  }

  @Override
  public void onBackPressed() {
    dismiss();
  }
}
