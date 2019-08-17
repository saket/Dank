package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;

import me.saket.dank.R;

public class EditTextWithPrefix extends AppCompatEditText {

  public EditTextWithPrefix(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.EditTextWithPrefix);
    String prefix = attributes.getString(R.styleable.EditTextWithPrefix_prefix);
    attributes.recycle();

    if (prefix == null || prefix.isEmpty()) {
      throw new AssertionError("Prefix cannot be empty");
    }

    setCompoundDrawablesRelative(new TextDrawable(prefix, this), null, null, null);
  }
}
