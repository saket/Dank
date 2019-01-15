package me.saket.dank.ui.post

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.saket.dank.R
import me.saket.dank.ui.DankActivity
import me.saket.dank.utils.Pair
import me.saket.dank.widgets.FabTransform

class CreateNewPostActivity : DankActivity() {

  private val dialogContainer by bindView<ViewGroup>(R.id.createnewpost_dialog_container)
  private val backgroundView by bindView<View>(R.id.createnewpost_background)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create_new_post)

    playEntryTransition()
    backgroundView.setOnClickListener { dismiss() }
    dialogContainer.clipToOutline = true
  }

  private fun playEntryTransition() {
    if (FabTransform.hasActivityTransition(this)) {
      dialogContainer.transitionName = SHARED_ELEMENT_TRANSITION_NAME
      FabTransform.setupActivityTransition(this, dialogContainer)
    } else {
      overridePendingTransition(R.anim.dialog_fade_in, 0)
    }
  }

  fun dismiss() {
    if (FabTransform.hasActivityTransition(this)) {
      finishAfterTransition()
    } else {
      super.finish()
      overridePendingTransition(0, R.anim.dialog_fade_out)
    }
  }

  override fun onBackPressed() {
    dismiss()
  }

  companion object {

    private const val SHARED_ELEMENT_TRANSITION_NAME = "sharedElement:CreateNewPostActivity"

    @JvmStatic
    fun intent(context: Context): Intent {
      return Intent(context, CreateNewPostActivity::class.java)
    }

    @JvmStatic
    fun intentWithFabTransform(
        activity: Activity,
        fab: FloatingActionButton,
        @ColorRes fabColorRes: Int,
        @DrawableRes fabIconRes: Int
    ): Pair<Intent, ActivityOptions> {
      val intent = intent(activity)

      FabTransform.addExtras(intent, ContextCompat.getColor(activity, fabColorRes), fabIconRes)
      val options = ActivityOptions.makeSceneTransitionAnimation(activity, fab, SHARED_ELEMENT_TRANSITION_NAME)
      return Pair.create(intent, options)
    }
  }
}
