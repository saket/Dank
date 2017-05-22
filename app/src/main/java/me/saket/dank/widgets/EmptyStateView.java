package me.saket.dank.widgets;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

public class EmptyStateView extends FrameLayout {

  @BindView(R.id.emptystate_emoji) TextView emojiView;
  @BindView(R.id.emptystate_message) TextView messageView;

  public EmptyStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    LayoutInflater.from(getContext()).inflate(R.layout.custom_empty_state, this, true);
    ButterKnife.bind(this, this);
  }

  public void setEmojiAndMessage(@StringRes int emojiRes, @StringRes int messageRes) {
    emojiView.setText(emojiRes);
    messageView.setText(messageRes);
  }

  public void setEmoji(@StringRes int emojiRes) {
    emojiView.setText(emojiRes);
  }

  public void setMessage(@StringRes int messageRes) {
    messageView.setText(messageRes);
  }
}
