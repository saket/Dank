package me.saket.dank.ui;

/**
 * @param <T> Type of UI.
 */
public interface UiChange<T> {

  void render(T ui);
}
