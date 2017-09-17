package me.saket.dank.widgets.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

/**
 * A button that supports a title and a subtitle.
 */
public class UserPreferenceSwitch extends LinearLayout {

  @BindView(R.id.preferencebutton_title) Switch titleView;
  @BindView(R.id.preferencebutton_summary) TextView summaryView;

  public UserPreferenceSwitch(Context context, AttributeSet attrs) {
    super(context, attrs, 0, R.style.DankUserPreferenceButton);
    init(attrs);
  }

  private void init(AttributeSet attrs) {
    LayoutInflater.from(getContext()).inflate(R.layout.custom_user_preferences_switch, this, true);
    ButterKnife.bind(this, this);

    TypedArray properties = getContext().obtainStyledAttributes(attrs, R.styleable.UserPreferenceSwitch);
    titleView.setText(properties.getString(R.styleable.UserPreferenceSwitch_prefswitch_title));
    summaryView.setText(properties.getString(R.styleable.UserPreferenceSwitch_prefswitch_summary));
    properties.recycle();

    if (summaryView.getText().length() == 0) {
      titleView.setGravity(Gravity.CENTER_VERTICAL);
      summaryView.setVisibility(GONE);
    }

    setOrientation(VERTICAL);
    setOnClickListener(o -> titleView.performClick());
  }
}
