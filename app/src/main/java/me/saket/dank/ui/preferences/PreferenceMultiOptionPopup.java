package me.saket.dank.ui.preferences;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.StringRes;
import android.view.View;
import android.view.ViewGroup;
import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import me.saket.dank.ui.user.PopupWindowWithMaterialTransition;
import me.saket.dank.utils.NestedOptionsPopupMenu;

public class PreferenceMultiOptionPopup<T> extends PopupWindowWithMaterialTransition {

  private Preference<T> preference;
  private List<Option> options;

  public static <T> Builder<T> builder(Preference<T> preference) {
    return new Builder<>(preference);
  }

  public PreferenceMultiOptionPopup(Context context, Preference<T> preference, List<Option<T>> options) {
    super(context);

    // TODO: Use NestedOptionsPopup here.
//    for (int index = 0; index < options.size(); index++) {
//      getMenu().add(Menu.NONE, index, Menu.NONE, options.get(index).titleRes());
//    }
//
//    setOnMenuItemClickListener(item -> {
//      Option<T> clickedOption = options.get(item.getItemId());
//      preference.set(clickedOption.preferenceValue());
//      return true;
//    });
  }

  @Override
  protected Rect calculateTransitionEpicenter(View anchor, ViewGroup popupDecorView, Point showLocation) {
    return NestedOptionsPopupMenu.transitionEpicenter(anchor, popupDecorView);
  }

  @AutoValue
  public abstract static class Option<T> {

    public abstract T preferenceValue();

    public abstract @StringRes
    int titleRes();

    public static <T> Option<T> create(T preferenceValue, @StringRes int titleRes) {
      return new AutoValue_PreferenceMultiOptionPopup_Option<>(preferenceValue, titleRes);
    }
  }

  public static class Builder<T> {

    private Preference<T> preference;
    private List<Option<T>> options;

    public Builder(Preference<T> preference) {
      this.preference = preference;
    }

    public Builder<T> addOption(T preferenceValue, @StringRes int titleRes) {
      if (options == null) {
        options = new ArrayList<>();
      }
      options.add(Option.create(preferenceValue, titleRes));
      return this;
    }

    public PreferenceMultiOptionPopup<T> build(Context context) {
      return new PreferenceMultiOptionPopup<>(context, preference, options);
    }
  }
}
