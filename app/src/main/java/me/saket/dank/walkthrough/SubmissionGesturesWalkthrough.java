package me.saket.dank.walkthrough;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.f2prateek.rx.preferences2.Preference;
import com.google.auto.value.AutoValue;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import net.dean.jraw.models.Submission;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.ui.preferences.TypefaceInflationInterceptor;
import me.saket.dank.ui.preferences.gestures.submissions.SubmissionSwipeActionsRepository;
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider;
import me.saket.dank.ui.subreddit.SubredditSubmissionsAdapter;
import me.saket.dank.ui.subreddit.uimodels.SubredditScreenUiModel;
import me.saket.dank.utils.Optional;
import me.saket.dank.widgets.swipe.SwipeAction;
import me.saket.dank.widgets.swipe.SwipeActionIconView;
import me.saket.dank.widgets.swipe.SwipeActions;
import me.saket.dank.widgets.swipe.SwipeDirection;
import me.saket.dank.widgets.swipe.SwipeTriggerRippleDrawable.RippleType;
import me.saket.dank.widgets.swipe.SwipeableLayout;
import me.saket.dank.widgets.swipe.SwipeableLayout.SwipeActionIconProvider;
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions;

public class SubmissionGesturesWalkthrough {

  private final Preference<Boolean> hasUserLearnedPref;
  private final Preference<Boolean> showSubmissionThumbnailsOnLeftPref;

  @Inject
  public SubmissionGesturesWalkthrough(
      @Named("user_learned_submission_gestures") Preference<Boolean> hasUserLearnedPref,
      @Named("show_submission_thumbnails_on_left") Preference<Boolean> showSubmissionThumbnailsOnLeftPref
  ) {
    this.hasUserLearnedPref = hasUserLearnedPref;
    this.showSubmissionThumbnailsOnLeftPref = showSubmissionThumbnailsOnLeftPref;
  }

  public Observable<Optional<UiModel>> walkthroughRows() {
    return Observable.combineLatest(
        hasUserLearnedPref.asObservable(),
        showSubmissionThumbnailsOnLeftPref.asObservable(),
        (hasLearned, showIconOnLeft) -> hasLearned
            ? Optional.empty()
            : Optional.of(UiModel.create(showSubmissionThumbnailsOnLeftPref.get()))
    );
  }

  @AutoValue
  public abstract static class UiModel implements SubredditScreenUiModel.SubmissionRowUiModel {

    @Override
    public Type type() {
      return Type.GESTURES_WALKTHROUGH;
    }

    @Override
    public long adapterId() {
      return SubredditSubmissionsAdapter.ADAPTER_ID_GESTURES_WALKTHROUGH;
    }

    public abstract boolean displayIconOnLeftSide();

    public static UiModel create(boolean displayIconOnLeftSide) {
      return new AutoValue_SubmissionGesturesWalkthrough_UiModel(displayIconOnLeftSide);
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder implements ViewHolderWithSwipeActions {
    private final TextSwitcher titleSwitcherView;
    private final TextSwitcher messageSwitcherView;
    private final ConstraintLayout constraintLayout;
    private final ConstraintSet originalConstraintSet = new ConstraintSet();
    private final ConstraintSet constraintSetWithIconOnLeft = new ConstraintSet();

    protected ViewHolder(View itemView, TypefaceInflationInterceptor typefaceInflationInterceptor) {
      super(itemView);
      titleSwitcherView = itemView.findViewById(R.id.submissiongestureswalkthrough_item_title_switcher);
      messageSwitcherView = itemView.findViewById(R.id.submissiongestureswalkthrough_item_message_switcher);
      constraintLayout = itemView.findViewById(R.id.submissiongestureswalkthrough_item_content_container);

      Context context = itemView.getContext();
      Resources res = itemView.getResources();

      titleSwitcherView.setFactory(() -> {
        TextView titleView = new TextView(context);
        titleView.setLineSpacing(res.getDimensionPixelSize(R.dimen.spacing2), 1f);
        titleView.setTextColor(ContextCompat.getColor(context, R.color.amber_200));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        typefaceInflationInterceptor.applyTypefaceChanges(titleView);
        return titleView;
      });

      messageSwitcherView.setFactory(() -> {
        TextView messageView = new TextView(context);
        messageView.setEllipsize(TextUtils.TruncateAt.END);
        messageView.setLineSpacing(res.getDimensionPixelSize(R.dimen.spacing2), 1f);
        messageView.setTextColor(ContextCompat.getColor(context, R.color.gray_500));
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        typefaceInflationInterceptor.applyTypefaceChanges(messageView);
        return messageView;
      });

      Animation inAnimation = AnimationUtils.loadAnimation(context, R.anim.gesture_walkthroughs_slide_and_fade_in_from_bottom);
      Animation outAnimation = AnimationUtils.loadAnimation(context, R.anim.gesture_walkthroughs_slide_and_fade_out_to_top);

      for (TextSwitcher switcher : new TextSwitcher[] { titleSwitcherView, messageSwitcherView }) {
        switcher.setInAnimation(inAnimation);
        switcher.setOutAnimation(outAnimation);
        switcher.setAnimateFirstView(false);
      }

      titleSwitcherView.setText(res.getString(R.string.subreddit_gestureswalkthrough_title));
      messageSwitcherView.setText(res.getString(R.string.subreddit_gestureswalkthrough_message));

      originalConstraintSet.clone(constraintLayout);
      constraintSetWithIconOnLeft.clone(context, R.layout.list_item_submission_gestures_walkthrough_content_left);
    }

    public void render(UiModel uiModel) {
      if (uiModel.displayIconOnLeftSide()) {
        constraintSetWithIconOnLeft.applyTo(constraintLayout);
      } else {
        originalConstraintSet.applyTo(constraintLayout);
      }
    }

    @Override
    public SwipeableLayout getSwipeableLayout() {
      return ((SwipeableLayout) itemView);
    }
  }

  public static class Adapter implements SubredditScreenUiModel.SubmissionRowUiChildAdapter<UiModel, ViewHolder> {
    private final WalkthroughSwipeActionsProvider swipeActionsProvider;
    private final Relay<SubmissionGestureWalkthroughProceedEvent> clickStream = PublishRelay.create();
    private final Lazy<TypefaceInflationInterceptor> typefaceInflationInterceptor;

    @Inject
    public Adapter(WalkthroughSwipeActionsProvider swipeActionsProvider, Lazy<TypefaceInflationInterceptor> typefaceInflationInterceptor) {
      this.swipeActionsProvider = swipeActionsProvider;
      this.typefaceInflationInterceptor = typefaceInflationInterceptor;
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
      View itemView = inflater.inflate(R.layout.list_item_submission_gestures_walkthrough, parent, false);
      ViewHolder holder = new ViewHolder(itemView, typefaceInflationInterceptor.get());
      SwipeableLayout swipeableLayout = holder.getSwipeableLayout();
      swipeableLayout.setSwipeActions(swipeActionsProvider.actions());
      swipeableLayout.setSwipeActionIconProvider(swipeActionsProvider);
      swipeableLayout.setOnPerformSwipeActionListener((action, swipeDirection) ->
          swipeActionsProvider.perform(action, holder, swipeableLayout, clickStream, swipeDirection)
      );
      return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel) {
      holder.render(uiModel);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, UiModel uiModel, List<Object> payloads) {
      onBindViewHolder(holder, uiModel);
    }

    public Observable<SubmissionGestureWalkthroughProceedEvent> proceedClicks() {
      return clickStream;
    }
  }

  public static class WalkthroughSwipeActionsProvider implements SwipeActionIconProvider {
    private final SubmissionSwipeActionsProvider submissionSwipeActionsProvider;
    private SubmissionSwipeActionsRepository submissionSwipeActionsRepository;
    private int discoveryCount = 0;
    private SwipeAction lastPerformedAction;
    private Submission syntheticSubmission = new SyntheticSubmission(3, "Here's a heart-warming photo to start your journey");

    @Inject
    public WalkthroughSwipeActionsProvider(
        SubmissionSwipeActionsProvider submissionSwipeActionsProvider,
        SubmissionSwipeActionsRepository submissionSwipeActionsRepository
    ) {
      this.submissionSwipeActionsProvider = submissionSwipeActionsProvider;
      this.submissionSwipeActionsRepository = submissionSwipeActionsRepository;
    }

    public SwipeActions actions() {
      return submissionSwipeActionsRepository.getDefaultSwipeActions();
    }

    @Override
    public void showSwipeActionIcon(SwipeActionIconView imageView, @Nullable SwipeAction oldAction, SwipeAction newAction) {
      submissionSwipeActionsProvider.showSwipeActionIcon(imageView, oldAction, newAction, syntheticSubmission);
    }

    public void perform(SwipeAction action, ViewHolder holder, SwipeableLayout layout, Relay<SubmissionGestureWalkthroughProceedEvent> clickStream, SwipeDirection swipeDirection) {
      Context context = layout.getContext();
      holder.titleSwitcherView.setText(String.format("'%s'", context.getString(action.labelRes())));
      TextView titleView = (TextView) holder.titleSwitcherView.getChildAt(holder.titleSwitcherView.getDisplayedChild());
      titleView.setTextColor(ContextCompat.getColor(context, action.backgroundColorRes()));

      ++discoveryCount;

      boolean isUndoAction = action.equals(lastPerformedAction);
      if (isUndoAction) {
        // Setting it to null so that this swipe action
        // can be registered when it's performed again.
        lastPerformedAction = null;
      } else {
        lastPerformedAction = action;
      }

      layout.playRippleAnimation(action, isUndoAction ? RippleType.UNDO : RippleType.REGISTER, swipeDirection);

      switch (discoveryCount) {
        case 1:
          holder.messageSwitcherView.setText(context.getString(R.string.subreddit_gestureswalkthrough_message_after_first_swipe_action));
          break;

        default:
        case 2:
          holder.messageSwitcherView.setText(context.getString(R.string.subreddit_gestureswalkthrough_message_after_second_swipe_action));
          holder.itemView.setOnClickListener(o ->
              clickStream.accept(SubmissionGestureWalkthroughProceedEvent.create(holder.itemView, holder.getItemId()))
          );
          break;
      }
    }
  }
}
