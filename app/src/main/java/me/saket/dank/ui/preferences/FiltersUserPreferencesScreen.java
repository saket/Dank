package me.saket.dank.ui.preferences;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.f2prateek.rx.preferences2.Preference;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.prefs.UserPreferenceSwitchView;

public class FiltersUserPreferencesScreen extends LinearLayout {

  @Inject @Named("show_nsfw_content") Preference<Boolean> showNsfwContentPref;

  @BindView(R.id.filters_nsfw_thumbnails) UserPreferenceSwitchView nsfwThumbnailsSwitch;

  public FiltersUserPreferencesScreen(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.view_user_preferences_filters, this, true);
    setOrientation(VERTICAL);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this, this);
    Dank.dependencyInjector().inject(this);

    nsfwThumbnailsSwitch.applyFrom(showNsfwContentPref);
  }
}
