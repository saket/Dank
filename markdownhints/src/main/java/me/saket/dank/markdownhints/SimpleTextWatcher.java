package me.saket.dank.markdownhints;

import android.text.TextWatcher;

abstract class SimpleTextWatcher implements TextWatcher {
  @Override
  public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

  @Override
  public void onTextChanged(CharSequence s, int start, int before, int count) {}
}
