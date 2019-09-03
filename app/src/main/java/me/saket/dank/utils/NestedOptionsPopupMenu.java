package me.saket.dank.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.ViewFlipper;

import androidx.annotation.AnimRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;

import com.google.auto.value.AutoValue;

import java.util.List;

import javax.inject.Inject;

import dagger.Lazy;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.preferences.TypefaceInflationInterceptor;
import me.saket.dank.ui.user.PopupWindowWithMaterialTransition;
import me.saket.dank.widgets.TintableCompoundDrawableTextView;

public abstract class NestedOptionsPopupMenu extends PopupWindowWithMaterialTransition {

  @Inject Lazy<TypefaceInflationInterceptor> typefaceInflationInterceptor;

  private ViewFlipper viewFlipper;

  @AutoValue
  public abstract static class MenuStructure {
    public abstract Optional<CharSequence> optionalTitle();

    public abstract List<SingleLineItem> items();

    public static MenuStructure create(Optional<CharSequence> title, List<SingleLineItem> items) {
      return new AutoValue_NestedOptionsPopupMenu_MenuStructure(title, items);
    }

    public static MenuStructure create(CharSequence title, List<SingleLineItem> items) {
      return new AutoValue_NestedOptionsPopupMenu_MenuStructure(Optional.of(title), items);
    }

    @AutoValue
    public abstract static class SingleLineItem {
      public abstract int id();

      public abstract String label();

      /**
       * 20dp is recommended.
       */
      @DrawableRes
      public abstract int iconRes();

      @Nullable
      public abstract List<ThreeLineItem> subItems();

      /**
       * @param iconRes 20dp is recommended.
       */
      public static SingleLineItem create(int id, String label, @DrawableRes int iconRes) {
        return new AutoValue_NestedOptionsPopupMenu_MenuStructure_SingleLineItem(id, label, iconRes, null);
      }

      /**
       * @param iconRes 20dp is recommended.
       * @deprecated
       */
      public static SingleLineItem create(int id, String label, @DrawableRes int iconRes, @Nullable List<ThreeLineItem> subItems) {
        return new AutoValue_NestedOptionsPopupMenu_MenuStructure_SingleLineItem(id, label, iconRes, subItems);
      }

      public static SingleLineItem create(String label, @DrawableRes int iconRes, @Nullable List<ThreeLineItem> subItems) {
        return new AutoValue_NestedOptionsPopupMenu_MenuStructure_SingleLineItem(-1, label, iconRes, subItems);
      }
    }

    @AutoValue
    public abstract static class ThreeLineItem {
      public abstract int id();

      public abstract CharSequence label();

      /**
       * Useful when the content-description should be different
       * from {@link #label()} on this item's button.
       */
      public abstract Optional<Integer> contentDescriptionRes();

      /**
       * @param contentDescriptionRes See {@link #contentDescriptionRes()}.
       */
      public static ThreeLineItem create(int id, CharSequence label, Integer contentDescriptionRes) {
        return new AutoValue_NestedOptionsPopupMenu_MenuStructure_ThreeLineItem(id, label, Optional.of(contentDescriptionRes));
      }

      public static ThreeLineItem create(int id, CharSequence label) {
        return new AutoValue_NestedOptionsPopupMenu_MenuStructure_ThreeLineItem(id, label, Optional.empty());
      }
    }
  }

  public NestedOptionsPopupMenu(Context c) {
    super(c);
    Dank.dependencyInjector().inject(this);
  }

  protected abstract void handleAction(Context c, int actionId);

  protected void createMenuLayout(Context c, MenuStructure menuStructure) {
    int spacing2 = c.getResources().getDimensionPixelSize(R.dimen.spacing2);
    int spacing16 = c.getResources().getDimensionPixelSize(R.dimen.spacing16);
    int spacing12 = c.getResources().getDimensionPixelSize(R.dimen.spacing12);
    int spacing24 = c.getResources().getDimensionPixelSize(R.dimen.spacing24);
    int deviceDisplayHeight = c.getResources().getDisplayMetrics().heightPixels;

    viewFlipper = new ViewFlipper(c);
    viewFlipper.setBackground(c.getDrawable(R.drawable.background_popup_window));
    LayoutParams viewFlipperParams = new LayoutParams(c.getResources().getDimensionPixelSize(R.dimen.popupwindow_width), LayoutParams.WRAP_CONTENT);
    viewFlipper.setLayoutParams(viewFlipperParams);
    viewFlipper.setClipToOutline(true);
    setContentView(viewFlipper);

    ScrollView mainMenuScrollableContainer = new ScrollView(c);
    viewFlipper.addView(mainMenuScrollableContainer, viewFlipperParams.width, LayoutParams.WRAP_CONTENT);
    Views.executeOnMeasure(mainMenuScrollableContainer, () -> {
      int height = mainMenuScrollableContainer.getHeight();
      int maxHeight = deviceDisplayHeight / 2;
      if (height > maxHeight) {
        Views.setHeight(mainMenuScrollableContainer, maxHeight);
      }
    });

    LinearLayout mainMenuContainer = new LinearLayout(c);
    mainMenuContainer.setOrientation(LinearLayout.VERTICAL);
    mainMenuScrollableContainer.addView(mainMenuContainer, viewFlipperParams.width, LayoutParams.WRAP_CONTENT);

    menuStructure.optionalTitle().ifPresent(title -> {
      AppCompatTextView mainMenuTitle = new AppCompatTextView(c);
      mainMenuTitle.setBackgroundColor(ContextCompat.getColor(c, R.color.black_opacity_10));
      mainMenuTitle.setSingleLine();
      mainMenuTitle.setEllipsize(TextUtils.TruncateAt.END);
      mainMenuTitle.setPadding(spacing16, spacing16, spacing16, spacing16);
      mainMenuTitle.setText(title);
      typefaceInflationInterceptor.get().applyTypefaceChanges(mainMenuTitle);
      mainMenuContainer.addView(mainMenuTitle, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    });

    for (MenuStructure.SingleLineItem singleLineItem : menuStructure.items()) {
      TintableCompoundDrawableTextView menuButton = new TintableCompoundDrawableTextView(c);
      menuButton.setText(singleLineItem.label());
      menuButton.setCompoundDrawablesRelativeWithIntrinsicBounds(singleLineItem.iconRes(), 0, 0, 0);
      menuButton.setCompoundDrawablePadding(spacing24);
      menuButton.setPadding(spacing16, spacing12, spacing16, spacing12);
      menuButton.setBackground(getSelectableItemBackground(c));
      menuButton.setTextColor(ContextCompat.getColor(c, R.color.gray_200));
      typefaceInflationInterceptor.get().applyTypefaceChanges(menuButton);
      mainMenuContainer.addView(menuButton, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

      List<MenuStructure.ThreeLineItem> subMenuItems = singleLineItem.subItems();
      if (subMenuItems != null) {
        LinearLayout subMenuContainer = new LinearLayout(c);
        subMenuContainer.setOrientation(LinearLayout.VERTICAL);
        subMenuContainer.setBackground(viewFlipper.getBackground());
        subMenuContainer.setElevation(c.getResources().getDimensionPixelSize(R.dimen.elevation_nested_options_popup_menu));
        viewFlipper.addView(subMenuContainer, viewFlipperParams.width, LayoutParams.MATCH_PARENT);

        int subMenuIndexInParent = viewFlipper.getChildCount() - 1;
        menuButton.setOnClickListener(o -> {
          setupSubMenuEnterAnimation(viewFlipper);
          viewFlipper.setDisplayedChild(subMenuIndexInParent);
        });

        TintableCompoundDrawableTextView subMenuTitle = new TintableCompoundDrawableTextView(c);
        subMenuTitle.setText(singleLineItem.label());
        subMenuTitle.setBackground(new LayerDrawable(new Drawable[] {
            new ColorDrawable(ContextCompat.getColor(c, R.color.black_opacity_10)),
            getSelectableItemBackground(c)
        }));
        subMenuTitle.setCompoundDrawablePadding(spacing24);
        subMenuTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_arrow_back_20dp, 0, 0, 0);
        subMenuTitle.setSingleLine();
        subMenuTitle.setGravity(Gravity.CENTER_VERTICAL);
        subMenuTitle.setPadding(spacing16, spacing16, spacing16, spacing16);
        subMenuTitle.setOnClickListener(o -> {
          setupSubMenuExitAnimation(viewFlipper);
          viewFlipper.setDisplayedChild(0);
        });
        typefaceInflationInterceptor.get().applyTypefaceChanges(subMenuTitle);
        subMenuContainer.addView(subMenuTitle, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        for (int i = 0; i < subMenuItems.size(); i++) {
          MenuStructure.ThreeLineItem subItem = subMenuItems.get(i);
          AppCompatTextView subMenuButton = new AppCompatTextView(c);
          subMenuButton.setText(subItem.label());
          subMenuButton.setBackgroundDrawable(getSelectableItemBackground(c));
          subMenuButton.setPadding(spacing16, spacing12, spacing16, spacing12);
          subMenuButton.setTextColor(ContextCompat.getColor(c, R.color.gray_200));
          subMenuButton.setOnClickListener(o -> handleAction(c, subItem.id()));
          subMenuButton.setMaxLines(3);
          subMenuButton.setEllipsize(TextUtils.TruncateAt.END);
          subMenuButton.setLineSpacing(spacing2, 1);
          subItem.contentDescriptionRes().ifPresent(contentDescriptionRes ->
              subMenuButton.setContentDescription(c.getString(contentDescriptionRes))
          );
          subMenuContainer.addView(subMenuButton, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
          typefaceInflationInterceptor.get().applyTypefaceChanges(subMenuButton);
        }
      } else {
        menuButton.setOnClickListener(o -> handleAction(c, singleLineItem.id()));
      }
    }
  }

  protected void gotoPrimaryPage() {
    setupSubMenuExitAnimation(viewFlipper);
    viewFlipper.setDisplayedChild(0);
  }

  private Drawable getSelectableItemBackground(Context c) {
    TypedArray attributes = c.obtainStyledAttributes(new int[] { R.attr.selectableItemBackground });
    Drawable selectableItemBackgroundDrawable = attributes.getDrawable(0);
    attributes.recycle();
    return selectableItemBackgroundDrawable;
  }

  private void setupSubMenuEnterAnimation(ViewFlipper viewFlipper) {
    viewFlipper.setInAnimation(animationWithInterpolator(viewFlipper.getContext(), R.anim.submission_options_viewflipper_submenu_enter));
    viewFlipper.setOutAnimation(animationWithInterpolator(viewFlipper.getContext(), R.anim.submission_options_viewflipper_mainmenu_exit));
  }

  private void setupSubMenuExitAnimation(ViewFlipper viewFlipper) {
    viewFlipper.setInAnimation(animationWithInterpolator(viewFlipper.getContext(), R.anim.submission_options_viewflipper_mainmenu_enter));
    viewFlipper.setOutAnimation(animationWithInterpolator(viewFlipper.getContext(), R.anim.submission_options_viewflipper_submenu_exit));
  }

  private Animation animationWithInterpolator(Context c, @AnimRes int animRes) {
    Animation animation = AnimationUtils.loadAnimation(c, animRes);
    animation.setInterpolator(Animations.INTERPOLATOR);
    return animation;
  }

  /**
   * Calculation copied from {@link PopupWindow}.
   */
  @Override
  protected Rect calculateTransitionEpicenter(View anchor, ViewGroup popupDecorView, Point showLocation) {
    int[] anchorLocation = new int[2];
    int[] popupLocation = new int[2];
    anchor.getLocationOnScreen(anchorLocation);
    popupDecorView.getLocationOnScreen(popupLocation);

    final Rect bounds = new Rect();
    bounds.left = bounds.right = anchorLocation[0] - popupLocation[0];
    bounds.top = bounds.bottom = anchorLocation[1] - popupLocation[1];
    return bounds;
  }
}
