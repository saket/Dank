package me.saket.dank.ui.preferences;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DankPreferenceGroup {

    @DrawableRes
    public abstract int iconResId();

    @StringRes
    public abstract int titleResId();

    @StringRes
    public abstract int subtitleResId();

    public static DankPreferenceGroup create(int iconResId, int titleResId, int subtitleResId) {
        return new AutoValue_DankPreferenceGroup(iconResId, titleResId, subtitleResId);
    }

}
