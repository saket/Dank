package me.saket.dank.widgets

import android.content.Context
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.widget.EditText
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Copied from:
 * https://github.com/simpledotorg/simple-android
 *
 * Offers a listener for backspace clicks, *before* the text is updated.
 *
 * In comparison, [EditText.setKeyListener] emits callbacks for backspaces only
 * from the hardware keyboard and *only* after the text has been changed, making
 * it difficult to read the text before backspace.
 */
class EditTextWithBackspaceListener(context: Context, attrs: AttributeSet?) : AppCompatEditText(context, attrs) {

  private val backspaceClicksSubject = PublishSubject.create<Any>()
  val backspaceClicks: Observable<Any> = backspaceClicksSubject.hide()

  override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
    return SoftKeyboardBackspaceListener(
        delegate = super.onCreateInputConnection(outAttrs),
        subject = backspaceClicksSubject)
  }

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
      // Backspace clicks from the hardware keyboard.
      backspaceClicksSubject.onNext(Any())
    }
    return super.dispatchKeyEvent(event)
  }

  private class SoftKeyboardBackspaceListener(
      delegate: InputConnection?,
      val subject: PublishSubject<Any>
  ) : InputConnectionWrapper(delegate, true) {

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
      if (beforeLength == 1 && afterLength == 0) {
        subject.onNext(Any())
      }
      return super.deleteSurroundingText(beforeLength, afterLength)
    }
  }

  override fun getText(): Editable {
    // AppCompatEditText returns a nullable Editable if the text isn't
    // editable -- which shouldn't be the case for this View.
    return super.getText()!!
  }
}
