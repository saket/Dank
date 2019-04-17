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
import android.widget.Button
import android.widget.EditText
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import kotterknife.bindView
import me.saket.dank.di.Dank
import me.saket.dank.ui.DankActivity
import me.saket.dank.ui.ScreenDestroyed
import me.saket.dank.utils.Keyboards
import me.saket.dank.utils.Pair
import me.saket.dank.widgets.EditTextWithBackspaceListener
import me.saket.dank.widgets.FabTransform
import timber.log.Timber
import javax.inject.Inject
import me.saket.dank.R

class CreateNewPostActivity : DankActivity() {

  @Inject
  lateinit var controller: CreateNewPostScreenController

  private val dialogContainer by bindView<ViewGroup>(R.id.createnewpost_dialog_container)
  private val backgroundView by bindView<View>(R.id.createnewpost_background)
  private val closeView by bindView<View>(R.id.createnewpost_close)
  private val titleEditText by bindView<EditText>(R.id.createnewpost_title)
  private val bodyEditText by bindView<EditTextWithBackspaceListener>(R.id.createnewpost_body)
  private val submitButton by bindView<Button>(R.id.createnewpost_send)

  override fun onCreate(savedInstanceState: Bundle?) {
    Dank.dependencyInjector().inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_create_new_post)

    playEntryTransition()
    setupScreen()
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
    Keyboards.hide(dialogContainer)
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

  private fun setupScreen() {
    dialogContainer.clipToOutline = true

    backgroundView.setOnClickListener { dismiss() }
    closeView.setOnClickListener { dismiss() }

    val screenDestroys = lifecycle()
        .onDestroy()
        .map { ScreenDestroyed }

    Observable
        .mergeArray(
            titleTextChanges(),
            bodyTextChanges(),
            bodyBackspaceClicks(),
            imageSelections(),
            screenDestroys)
        .compose(controller)
        .observeOn(mainThread())
        .takeUntil(screenDestroys)
        .subscribe { uiChange -> uiChange(this) }
  }

  private fun titleTextChanges() =
      RxTextView
          .textChanges(titleEditText)
          .map { NewPostTitleTextChanged(it.toString()) }

  private fun bodyTextChanges() =
      RxTextView
          .textChanges(bodyEditText)
          .map { NewPostBodyTextChanged(it.toString()) }

  private fun bodyBackspaceClicks() =
      bodyEditText
          .backspaceClicks
          .map { NewPostBodyBackspaceClicked }

  private fun imageSelections() = Observable.just(NewPostImageSelectionUpdated(images = emptyList()))

  fun setDetectedPostKind(kind: NewPostKind) {
    Timber.w("TODO: set post type to $kind")
  }

  fun setSubmitButtonEnabled(enabled: Boolean) {
    submitButton.isEnabled = enabled
  }

  fun requestFocusOnTitleField() {
    titleEditText.requestFocus()
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
