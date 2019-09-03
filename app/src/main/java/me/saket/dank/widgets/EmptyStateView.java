package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

public class EmptyStateView extends FrameLayout {

  @BindView(R.id.emptystate_emoji) TextView emojiView;
  @BindView(R.id.emptystate_message) TextView messageView;

  public EmptyStateView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  private void init(AttributeSet attrs) {
    LayoutInflater.from(getContext()).inflate(R.layout.custom_empty_state, this, true);
    ButterKnife.bind(this, this);

    TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.EmptyStateView);
    if (attributes.hasValue(R.styleable.EmptyStateView_emptyState_emoji)) {
      emojiView.setText(attributes.getText(R.styleable.EmptyStateView_emptyState_emoji));
    }
    if (attributes.hasValue(R.styleable.EmptyStateView_emptyState_message)) {
      messageView.setText(attributes.getText(R.styleable.EmptyStateView_emptyState_message));
    }
    attributes.recycle();
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
