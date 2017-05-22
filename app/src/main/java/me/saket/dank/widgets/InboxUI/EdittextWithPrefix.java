package me.saket.dank.widgets.InboxUI;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

import me.saket.dank.R;
import me.saket.dank.widgets.TextDrawable;

public class EdittextWithPrefix extends AppCompatEditText {

  public EdittextWithPrefix(Context context, AttributeSet attrs) {
    super(context, attrs);

    TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.EdittextWithPrefix);
    String prefix = attributes.getString(R.styleable.EdittextWithPrefix_prefix);
    attributes.recycle();

    if (prefix == null || prefix.isEmpty()) {
      throw new AssertionError("Prefix cannot be empty");
    }

    setCompoundDrawablesRelative(new TextDrawable(prefix, this), null, null, null);
  }

}
