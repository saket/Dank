package me.saket.dank.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.CheckResult;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.EmptyState;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.data.ErrorState;

public class ErrorStateView extends LinearLayout {

  @BindView(R.id.errorstate_emoji) TextView emojiView;
  @BindView(R.id.errorstate_message) TextView messageView;
  @BindView(R.id.errorstate_retry) Button retryButton;

  public ErrorStateView(Context context, AttributeSet attrs) {
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

    retryButton.setOnClickListener(v -> {
      throw new AssertionError("No retry listener present");
    });
  }

  public void applyFrom(ResolvedError error) {
    emojiView.setText(error.errorEmojiRes());
    messageView.setText(error.errorMessageRes());
    retryButton.setText(R.string.common_error_retry);
  }

  public void applyFrom(ErrorState error) {
    emojiView.setText(error.emojiRes());
    messageView.setText(error.messageRes());
    retryButton.setText(R.string.common_error_retry);
  }

  public void applyFrom(EmptyState emptyState) {
    emojiView.setText(emptyState.emojiRes());
    messageView.setText(emptyState.messageRes());
    retryButton.setText(R.string.common_emptystate_reload);
  }

  public void setOnRetryClickListener(View.OnClickListener listener) {
    retryButton.setOnClickListener(listener);
  }

  @CheckResult
  public Observable<Object> retryClicks() {
    return RxView.clicks(retryButton);
  }

  public Button getRetryButton() {
    return retryButton;
  }
}
