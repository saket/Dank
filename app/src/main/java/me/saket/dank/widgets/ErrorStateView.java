package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.saket.dank.R;
import me.saket.dank.data.ResolvedError;

public class ErrorStateView extends LinearLayout {

  @BindView(R.id.errorstate_emoji) TextView emojiView;
  @BindView(R.id.errorstate_message) TextView messageView;

  private View.OnClickListener onClickRetryListener;

  public ErrorStateView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);

    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER);
  }

  private void init(AttributeSet attrs) {
    LayoutInflater.from(getContext()).inflate(R.layout.custom_error_state, this, true);
    ButterKnife.bind(this, this);

    TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.ErrorStateView);

    if (attributes.hasValue(R.styleable.ErrorStateView_emojiVisible)) {
      boolean emojiVisible = attributes.getBoolean(R.styleable.ErrorStateView_emojiVisible, true);
      emojiView.setVisibility(emojiVisible ? VISIBLE : GONE);
    }
    attributes.recycle();
  }

  public void applyFrom(ResolvedError error) {
    emojiView.setText(error.errorEmojiRes());
    messageView.setText(error.errorMessageRes());
  }

  public void setOnRetryClickListener(View.OnClickListener retryClickListener) {
    onClickRetryListener = retryClickListener;
  }

  @OnClick(R.id.errorstate_retry)
  void onClickRetry(View button) {
    if (onClickRetryListener == null) {
      throw new AssertionError("No retry listener present");
    }
    onClickRetryListener.onClick(button);
  }
}
