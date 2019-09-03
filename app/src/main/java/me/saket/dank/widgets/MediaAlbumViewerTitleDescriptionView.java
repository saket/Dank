package me.saket.dank.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.jakewharton.rxrelay2.BehaviorRelay;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;
import me.saket.dank.utils.Views;

public class MediaAlbumViewerTitleDescriptionView extends RelativeLayout {

  private static final int MAX_LINES_IN_COLLAPSED_STATE = 2;

  @BindView(R.id.mediaalbumviewer_title) public TextView titleView;
  @BindView(R.id.mediaalbumviewer_description) public TextView descriptionView;
  @BindView(R.id.mediaalbumviewer_titledescription_scrollview) ScrollView scrollView;
  @BindView(R.id.mediaalbumviewer_titledescription_scrollview_child) ViewGroup scrollableChild;
  @BindView(R.id.mediaalbumviewer_titledescription_scroll_hint) View scrollHintView;

  private boolean isDescriptionCollapsed = true;
  private final Rect tempRect = new Rect();

  private BehaviorRelay<Boolean> dimmingRequiredForTitleAndDescriptionStream = BehaviorRelay.create();
  private boolean touchStartedOnScrollableChild;

  @SuppressLint("SetTextI18n")
  public MediaAlbumViewerTitleDescriptionView(Context context, AttributeSet attrs) {
    super(context, attrs);
    LayoutInflater.from(context).inflate(R.layout.custom_mediaalbumviewer_title_and_description, this, true);
    ButterKnife.bind(this, this);

    scrollView.setClipToPadding(false);
    scrollView.setVisibility(INVISIBLE);   // Set to VISIBLE once title & description are set, and the description is clipped to 2.5 lines.
    scrollHintView.setVisibility(INVISIBLE);

    scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
      dimmingRequiredForTitleAndDescriptionStream.accept(scrollView.getScrollY() > 0 ? Boolean.TRUE : Boolean.FALSE);
    });
    dimmingRequiredForTitleAndDescriptionStream.accept(Boolean.FALSE);

    if (isInEditMode()) {
      titleView.setText("Ruslan and the Giant Illustration");
      descriptionView.setText("This is an illustration based on a Russian fairy tale called Ruslan and Liudmila in which the hero Ruslan battles "
          + "a giant head. I have been planning and executing this piece for months in 2014 with the sole purpose of submitting it (and hopefully "
          + "getting in) to a fantasy/scifi yearly book called Spectrum.");
    }

    if (getVisibility() == GONE) {
      throw new AssertionError("GONE not supported");
    }
  }

  public void setTitleAndDescription(@Nullable String title, @Nullable String description) {
    titleView.setText(title);
    descriptionView.setText(description);

    titleView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);
    descriptionView.setVisibility(TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);

    Views.executeOnMeasure(this, () ->
        Views.executeOnNextLayout(this, () -> {
          int extraTopPadding;

          if (isDescriptionCollapsed) {
            Layout descriptionViewLayout = descriptionView.getLayout();

            if (descriptionViewLayout == null) {  // TODO: remove true.
              int descriptionViewWidth = getWidth() - scrollableChild.getPaddingStart() - scrollableChild.getPaddingEnd()
                  - scrollView.getPaddingStart() - scrollView.getPaddingEnd();

              descriptionViewLayout = new StaticLayout(
                  descriptionView.getText().toString(),
                  descriptionView.getPaint(),
                  descriptionViewWidth,
                  Layout.Alignment.ALIGN_NORMAL,
                  descriptionView.getLineSpacingMultiplier(),
                  descriptionView.getLineSpacingExtra(),
                  true
              );
            }

            if (descriptionViewLayout.getLineCount() > MAX_LINES_IN_COLLAPSED_STATE) {
              // We want to keep the description visible till 2.5 lines. 0.5 lines extra so that the user knows the description is clipped.
              // Update: the calculation of 2.5th line's location is actually slightly incorrect. I couldn't figure out the error, but I
              // managed to get it visually working.
              int thirdLineTop = descriptionViewLayout.getLineTop(MAX_LINES_IN_COLLAPSED_STATE);
              int thirdLineAscent = descriptionViewLayout.getLineAscent(MAX_LINES_IN_COLLAPSED_STATE);
              int keepDescriptionVisibleTillDistance = (int) (thirdLineTop + (-thirdLineAscent * 0.75f));

              int totalSpaceAvailable = getHeight() - getPaddingBottom() - scrollHintView.getHeight();
              int scrollViewHeight = scrollView.getHeight();
              int descriptionTopInScrollView = Views.getTopRelativeToParent(scrollView, descriptionView);
              int distanceFromTopToScrollableChild = totalSpaceAvailable - scrollViewHeight;
              int sizeOfDescriptionToHide = scrollViewHeight - (descriptionTopInScrollView + keepDescriptionVisibleTillDistance);
              extraTopPadding = distanceFromTopToScrollableChild + sizeOfDescriptionToHide;

              //Timber.i("totalSpaceAvailable: %s", totalSpaceAvailable);
              //Timber.i("scrollViewHeight: %s", scrollViewHeight);
              //Timber.i("descriptionTopInScrollView: %s", descriptionTopInScrollView);
              //Timber.i("distanceFromTopToScrollableChild: %s", distanceFromTopToScrollableChild);
              //Timber.i("sizeOfDescriptionToHide: %s", sizeOfDescriptionToHide);

            } else {
              extraTopPadding = 0;
            }

          } else {
            extraTopPadding = 0;
          }

          Views.setPaddingTop(scrollView, extraTopPadding);
          scrollView.post(() -> scrollView.setVisibility(VISIBLE));

          boolean isDescriptionScrollable = extraTopPadding > 0;
          scrollHintView.setVisibility(isDescriptionScrollable ? VISIBLE : GONE);
          if (scrollHintView.getVisibility() == GONE) {
            // ScrollView is aligned above the scroll hint View. Move it to parent bottom if the hint is removed.
            ((RelativeLayout.LayoutParams) scrollView.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
          }
        })
    );
  }

  /**
   * Emits true when the title & description are being pulled upwards and the image should
   * be dimmed so that the text is readable.
   */
  public BehaviorRelay<Boolean> streamDimmingRequiredForTitleAndDescription() {
    return dimmingRequiredForTitleAndDescriptionStream;
  }

  public void resetScrollY() {
    scrollView.setScrollY(0);
  }

  @Override
  public void setVisibility(int visibility) {
    if (visibility == GONE) {
      throw new AssertionError("GONE not supported");
    }
    super.setVisibility(visibility);
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    if (ev.getAction() == MotionEvent.ACTION_DOWN) {
      if (getVisibility() == VISIBLE) {
        // Register touch events only if they are made on the scrollable content, because this ScrollView's height could be MATCH_PARENT.
        scrollableChild.getGlobalVisibleRect(tempRect);
        touchStartedOnScrollableChild = tempRect.contains((int) ev.getX(), (int) ev.getY());

        if (!touchStartedOnScrollableChild) {
          scrollHintView.getGlobalVisibleRect(tempRect);
          boolean touchStartedOnScrollHint = tempRect.contains((int) ev.getX(), (int) ev.getY());
          if (touchStartedOnScrollHint) {
            // ScrollView isn't going to scroll in response to this event, so might as
            // well ignore block it so that the image doesn't accidentally get flicked.
            return true;
          }
        }
      } else {
        touchStartedOnScrollableChild = false;
      }
    }

    return touchStartedOnScrollableChild && super.dispatchTouchEvent(ev);
  }
}
