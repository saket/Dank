package me.saket.dank.utils;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.CheckResult;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import io.reactivex.Observable;
import me.saket.dank.widgets.KeyboardVisibilityDetector;
import me.saket.dank.widgets.KeyboardVisibilityDetector.KeyboardVisibilityChangeEvent;

/**
 * Utility methods for the soft-keyboard.
 */
public class Keyboards {

  /**
   * Show the keyboard on <var>editText</var>.
   */
  public static boolean show(EditText editText) {
    editText.requestFocus();
    InputMethodManager inputMethodManager = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    //noinspection ConstantConditions
    return inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
  }

  /**
   * Hide the keyboard.
   */
  public static void hide(Context context, View anyViewInLayout) {
    hide(anyViewInLayout);
  }

  /**
   * Hide the keyboard.
   */
  public static void hide(View anyViewInLayout) {
    InputMethodManager inputManager = (InputMethodManager) anyViewInLayout.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    //noinspection ConstantConditions
    inputManager.hideSoftInputFromWindow(anyViewInLayout.getWindowToken(), 0);
  }

  @CheckResult
  public static Observable<KeyboardVisibilityChangeEvent> streamKeyboardVisibilityChanges(Activity activity, int statusBarHeight) {
    return new KeyboardVisibilityDetector(activity, statusBarHeight).streamKeyboardVisibilityChanges();
  }
}
