package me.saket.dank.utils;

import android.graphics.drawable.AnimatedVectorDrawable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReversibleAnimatedVectorDrawable {

  private final AnimatedVectorDrawable drawable;
  private Method reverseMethod;

  public ReversibleAnimatedVectorDrawable(AnimatedVectorDrawable drawable) {
    this.drawable = drawable;
  }

  public void play() {
    drawable.start();
  }

  public void reverse() {
    try {
      if (reverseMethod == null) {
        reverseMethod = drawable.getClass().getMethod("reverse");
      }
      reverseMethod.invoke(drawable);
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }
}
