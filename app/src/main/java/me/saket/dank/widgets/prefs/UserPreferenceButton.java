package me.saket.dank.widgets.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

/**
 * A button that supports a title and a subtitle.
 */
public class UserPreferenceButton extends LinearLayout {

  @BindView(R.id.preferencebutton_title) TextView titleView;
  @BindView(R.id.preferencebutton_summary) TextView summaryView;

  public UserPreferenceButton(Context context, AttributeSet attrs) {
    super(context, attrs, 0, R.style.DankUserPreferenceButton);
    init(attrs);
  }

  private void init(AttributeSet attrs) {
    LayoutInflater.from(getContext()).inflate(R.layout.custom_user_preferences_button, this, true);
    ButterKnife.bind(this, this);

    TypedArray properties = getContext().obtainStyledAttributes(attrs, R.styleable.UserPreferenceButton);
    titleView.setText(properties.getString(R.styleable.UserPreferenceButton_prefbutton_title));
    summaryView.setText(properties.getString(R.styleable.UserPreferenceButton_prefbutton_summary));
    properties.recycle();

    setOrientation(VERTICAL);
  }
}
