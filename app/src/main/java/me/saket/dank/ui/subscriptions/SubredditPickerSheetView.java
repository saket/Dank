package me.saket.dank.ui.subscriptions;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.subreddit.SubredditActivity;
import me.saket.dank.ui.subreddit.Subscribeable;
import me.saket.dank.ui.user.UserSessionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.itemanimators.SlideLeftAlphaAnimator;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;
import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.doOnceAfterNext;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.RxUtils.onStartAndFirstEvent;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.setMarginBottom;
import static me.saket.dank.utils.Views.setPaddingBottom;
import static me.saket.dank.utils.Views.touchLiesOn;

/**
 * Lets the user:
 * - Pick
 * - Add new
 * - Hide
 * - Unsubscribe.
 * - Set default.
 */
public class SubredditPickerSheetView extends FrameLayout implements SubredditAdapter.OnSubredditClickListener {

  private static final int HEIGHT_CHANGE_ANIM_DURATION = 300;
  private static final int OPTIONS_VISIBILITY_ANIM_DURATION = 150;
  private static final Interpolator ANIM_INTERPOLATOR = Animations.INTERPOLATOR;

  @BindView(R.id.subredditpicker_root) ViewGroup rootViewGroup;
  @BindView(R.id.subredditpicker_search) public EditText searchView;
  @BindView(R.id.subredditpicker_subreddit_list) RecyclerView subredditList;
  @BindView(R.id.subredditpicker_load_progress) View subredditsLoadProgressView;
  @BindView(R.id.subredditpicker_refresh_progress) View subredditsRefreshProgressView;
  @BindView(R.id.subredditpicker_options_container) ViewGroup optionsContainer;
  @BindView(R.id.subredditpicker_option_manage) View editButton;
  @BindView(R.id.subredditpicker_add_and_more_options_container) ViewGroup addAndMoreOptionsContainer;
  @BindView(R.id.subredditpicker_save_fab) FloatingActionButton saveButton;

  @BindDimen(R.dimen.subreddit_picker_sheet_height) int collapsedPickerSheetHeight;
  @BindDimen(R.dimen.subreddit_picker_sheet_bottom_margin) int parentSheetBottomMarginForShadows;
  @BindColor(R.color.subredditpicker_subreddit_button_tint_selected) int focusedSubredditButtonTintColor;

  @Inject Lazy<SubscriptionRepository> subscriptionRepository;
  @Inject Lazy<Reddit> reddit;
  @Inject Lazy<UserSessionRepository> userSessionRepository;

  private ViewGroup activityRootLayout;
  private ToolbarExpandableSheet parentSheet;
  private SubredditAdapter subredditAdapter;
  private CompositeDisposable subscriptions;
  private Callbacks callbacks;
  private SheetState sheetState;
  private BehaviorRelay<Boolean> showHiddenSubredditsSubject;
  private Runnable pendingOnWindowDetachedRunnable;
  private SlideLeftAlphaAnimator itemAnimator;

  enum SheetState {
    BROWSE_SUBS,
    MANAGE_SUBS
  }

  public interface Callbacks {
    void onSelectSubreddit(String subredditName);

    void onClickAddNewSubreddit();

    /**
     * Called when the sheet is collapsing and the subscriptions were changed (added or removed).
     */
    void onSubredditsChange();
  }

  public void showIn(ToolbarExpandableSheet toolbarSheet, ViewGroup activityRootLayout) {
    setActivityRootLayout(activityRootLayout);
    setParentSheet(toolbarSheet);
    toolbarSheet.addView(this, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  public SubredditPickerSheetView(Context context) {
    super(context);
    inflate(context, R.layout.view_subreddit_picker_sheet, this);
    ButterKnife.bind(this, this);
    Dank.dependencyInjector().inject(this);

    saveButton.setVisibility(INVISIBLE);
    sheetState = SheetState.BROWSE_SUBS;
    subscriptions = new CompositeDisposable();
    showHiddenSubredditsSubject = BehaviorRelay.createDefault(false);
    itemAnimator = new SlideLeftAlphaAnimator(0);
  }

  public boolean shouldInterceptPullToCollapse(float downX, float downY) {
    // FlexboxLayoutManager does not override methods required for View#canScrollVertically.
    // So let's intercept all pull-to-collapses if the touch is made on the list.
    return touchLiesOn(subredditList, downX, downY);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    subredditAdapter = new SubredditAdapter();
    subredditAdapter.setOnSubredditClickListener(this);
    subredditList.setAdapter(subredditAdapter);
    subredditList.setLayoutManager(new SubredditFlexboxLayoutManager(getContext()));

    setupSubredditsSearch();

    // Track changes in subscriptions and send a callback if needed when this sheet collapses.
    subscriptions.add(subscriptionRepository.get()
        .getAllIncludingHidden()
        .scan((oldSubscriptions, newSubscriptions) -> {
          if (oldSubscriptions.size() != newSubscriptions.size()) {
            pendingOnWindowDetachedRunnable = () -> callbacks.onSubredditsChange();
          }
          return newSubscriptions;
        })
        .subscribe());
  }

  @Override
  protected void onDetachedFromWindow() {
    subscriptions.clear();
    if (pendingOnWindowDetachedRunnable != null) {
      pendingOnWindowDetachedRunnable.run();
    }
    super.onDetachedFromWindow();
  }

  public void setActivityRootLayout(ViewGroup activityRootLayout) {
    this.activityRootLayout = activityRootLayout;
  }

  public void setParentSheet(ToolbarExpandableSheet parentSheet) {
    this.parentSheet = parentSheet;
  }

  public void setCallbacks(Callbacks callbacks) {
    this.callbacks = callbacks;
  }

  /**
   * Called when a subreddit is clicked in the adapter. Depending upon the {@link SheetState},
   * the subreddit is either passed to {@link SubredditActivity} for switching the sub or an
   * options menu is shown if the user is managing the subs.
   */
  @Override
  public void onClickSubreddit(SubredditSubscription subscription, View subredditItemView) {
    if (sheetState == SheetState.BROWSE_SUBS) {
      callbacks.onSelectSubreddit(subscription.name());

      // Fire-n-forget.
      subscriptionRepository.get()
          .incrementVisitCount(subscription)
          .subscribe();

    } else {
      showSubredditOptionsMenu(subscription, subredditItemView);
    }
  }

// ======== SEARCH & SUBREDDIT LIST ======== //

  // TODO: 15/04/17 Handle error.
  // TODO: 15/04/17 Empty state.
  private void setupSubredditsSearch() {
    // RxTextView emits an initial event on subscribe, so the following code will result in loading all subreddits.
    subscriptions.add(RxTextView.textChanges(searchView)
        .map(searchTerm -> {
          // Since CharSequence is mutable, it's important to create a copy before doing a debounce().
          return searchTerm.toString().trim();
        })
        .doOnNext(o -> {
          // So this part is interesting. We want item change animations to work only if the user is
          // adding/removing subreddits and not while searching. Setting the animator to null here ensures
          // that the animation is disabled as soon as a text change event is received. However, enabling
          // the animation later like when a subscription is removed in showSubredditOptionsMenu() ensures
          // that the animations run when any event is emitted by downstream Rx chains.
          subredditList.setItemAnimator(null);
        })
        .flatMap(o -> showHiddenSubredditsSubject)
        .switchMap(showHidden -> subscriptionRepository.get()
            .getAll(searchView.getText().toString(), showHidden)
            .subscribeOn(io()))
        .map(filteredSubs -> {
          if (sheetState == SheetState.BROWSE_SUBS) {
            // If search is active, show user's search term in the results unless an exact match was found.
            String searchTerm = searchView.getText().toString();

            if (!searchTerm.isEmpty()) {
              boolean exactSearchFound = false;
              for (SubredditSubscription filteredSub : filteredSubs) {
                if (filteredSub.name().equalsIgnoreCase(searchTerm)) {
                  exactSearchFound = true;
                  break;
                }
              }

              if (!exactSearchFound) {
                ArrayList<SubredditSubscription> filteredSubsWithQuery = new ArrayList<>(filteredSubs.size() + 1);
                filteredSubsWithQuery.addAll(filteredSubs);
                filteredSubsWithQuery.add(SubredditSubscription.create(searchTerm, SubredditSubscription.PendingState.NONE, false));
                return Collections.unmodifiableList(filteredSubsWithQuery);
              }
            }
          }

          return filteredSubs;
        })
        .onErrorResumeNext(error -> {
          // Don't let an error terminate the stream.
          Timber.e(error, "Error in fetching subreddits");
          return Observable.just(Collections.emptyList());
        })
        .observeOn(mainThread())
        .compose(onStartAndFirstEvent(setSubredditLoadProgressVisible()))
        .compose(doOnceAfterNext(o -> listenToBackgroundRefreshes()))
        .doOnNext(o -> optionsContainer.setVisibility(VISIBLE))
        .subscribe(subreddits -> subredditAdapter.updateDataAndNotifyDatasetChanged(subreddits))
    );
  }

  private void listenToBackgroundRefreshes() {
    subscriptions.add(SubredditSubscriptionsSyncJob.progressUpdates()
        .observeOn(mainThread())
        .subscribe(setSubredditLoadProgressVisible())
    );
  }

  private Consumer<Boolean> setSubredditLoadProgressVisible() {
    return visible -> {
      if (visible) {
        if (subredditAdapter.getItemCount() == 0) {
          subredditsLoadProgressView.setVisibility(View.VISIBLE);
        } else {
          subredditsRefreshProgressView.setVisibility(View.VISIBLE);
        }

      } else {
        subredditsRefreshProgressView.setVisibility(View.GONE);
        subredditsLoadProgressView.setVisibility(View.GONE);
      }
    };
  }

  @OnEditorAction(R.id.subredditpicker_search)
  boolean onClickSearchFieldEnterKey(int actionId) {
    if (sheetState == SheetState.BROWSE_SUBS && actionId == EditorInfo.IME_ACTION_GO) {
      String searchTerm = searchView.getText().toString().trim();
      callbacks.onSelectSubreddit(searchTerm);
      return true;
    }
    return false;
  }

// ======== OPTIONS ======== //

  @OnClick(R.id.subredditpicker_option_manage)
  void onClickEditSubreddits() {
    sheetState = SheetState.MANAGE_SUBS;

    // FIXME: Refresh height with keyboard visibility changes.
    int height = activityRootLayout.getHeight() - getTop();

    animateAlpha(editButton, 0f)
        .withEndAction(() -> {
          editButton.setVisibility(GONE);
          addAndMoreOptionsContainer.setVisibility(VISIBLE);
          animateAlpha(addAndMoreOptionsContainer, 1f);
        });

    ValueAnimator heightAnimator = ObjectAnimator.ofInt(rootViewGroup.getHeight(), height);
    heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      boolean spacingsAdjusted = false;

      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        setHeight(rootViewGroup, ((int) animation.getAnimatedValue()));

        // The parent sheet keeps a bottom margin to make space for its shadows.
        // Remove it so that this sheet can extend to the window bottom. Hide it
        // midway animation so that the user does not notice.
        if (!spacingsAdjusted && animation.getAnimatedFraction() > 0.5f) {
          spacingsAdjusted = true;
          setMarginBottom(parentSheet, 0);

          // Also add bottom padding to the list to make space for the FAB. The
          // list has clipToPadding=false in XML.
          setPaddingBottom(subredditList, saveButton.getHeight());
        }
      }
    });
    heightAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        saveButton.show();
      }
    });
    heightAnimator.setInterpolator(ANIM_INTERPOLATOR);
    heightAnimator.setDuration(HEIGHT_CHANGE_ANIM_DURATION);
    heightAnimator.start();
  }

  @OnClick(R.id.subredditpicker_save_fab)
  void onClickSaveSubreddits() {
    sheetState = SheetState.BROWSE_SUBS;

    // Hide hidden subs.
    showHiddenSubredditsSubject.accept(false);

    animateAlpha(addAndMoreOptionsContainer, 0f)
        .withStartAction(() -> saveButton.hide())
        .withEndAction(() -> {
          editButton.setVisibility(VISIBLE);
          addAndMoreOptionsContainer.setVisibility(GONE);
          animateAlpha(editButton, 1f);
        });

    ValueAnimator heightAnimator = ObjectAnimator.ofInt(rootViewGroup.getHeight(), collapsedPickerSheetHeight);
    heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      boolean spacingsAdjusted = false;

      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        setHeight(rootViewGroup, ((int) animation.getAnimatedValue()));

        if (!spacingsAdjusted && animation.getAnimatedFraction() > 0.5f) {
          spacingsAdjusted = true;
          setMarginBottom(parentSheet, parentSheetBottomMarginForShadows);
          setPaddingBottom(subredditList, 0);
        }
      }
    });
    heightAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        saveButton.hide();
      }
    });
    heightAnimator.setInterpolator(ANIM_INTERPOLATOR);
    heightAnimator.setDuration(HEIGHT_CHANGE_ANIM_DURATION);
    heightAnimator.start();
  }

  @OnClick(R.id.subredditpicker_option_overflow_menu)
  void onClickOverflowMenu(View optionView) {
    PopupMenu overflowMenu = new PopupMenu(getContext(), optionView, Gravity.END);
    overflowMenu.inflate(R.menu.menu_subredditpicker_overflow_menu);

    overflowMenu.getMenu().findItem(R.id.action_show_hidden_subreddits).setVisible(!showHiddenSubredditsSubject.getValue());
    overflowMenu.getMenu().findItem(R.id.action_hide_hidden_subreddits).setVisible(showHiddenSubredditsSubject.getValue());

    // Enable item change animation in case refresh is clicked or hidden subs are toggled.
    subredditList.setItemAnimator(itemAnimator);

    overflowMenu.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.action_show_hidden_subreddits:
          showHiddenSubredditsSubject.accept(true);
          return true;

        case R.id.action_hide_hidden_subreddits:
          showHiddenSubredditsSubject.accept(false);
          return true;

        case R.id.action_refresh_subreddits:
          SubredditSubscriptionsSyncJob.syncImmediately(getContext());
          return true;

        default:
          throw new UnsupportedOperationException("Unknown option itemId");
      }
    });
    overflowMenu.show();
  }

  private ViewPropertyAnimator animateAlpha(View view, float toAlpha) {
    return view.animate().alpha(toAlpha).setDuration(OPTIONS_VISIBILITY_ANIM_DURATION).setInterpolator(ANIM_INTERPOLATOR);
  }

  private void showSubredditOptionsMenu(SubredditSubscription subscription, View subredditItemView) {
    PopupMenu popupMenu = new PopupMenu(getContext(), subredditItemView, Gravity.BOTTOM, 0, R.style.DankPopupMenu_SubredditOptions);
    popupMenu.inflate(R.menu.menu_subredditpicker_subreddit_options);

    popupMenu.getMenu().findItem(R.id.action_set_subreddit_as_default).setVisible(!subscriptionRepository.get().isDefault(subscription));
    popupMenu.getMenu().findItem(R.id.action_hide_subreddit).setVisible(!subscription.isHidden());
    popupMenu.getMenu().findItem(R.id.action_unhide_subreddit).setVisible(subscription.isHidden());

    MenuItem unsubscribeItem = popupMenu.getMenu().findItem(R.id.action_unsubscribe_subreddit);
    unsubscribeItem.setVisible(!subscriptionRepository.get().isFrontpage(subscription.name()));

    if (userSessionRepository.get().isUserLoggedIn()) {
      boolean needsRemoteSubscription = reddit.get().subscriptions().needsRemoteSubscription(subscription.name());
      unsubscribeItem.setTitle(needsRemoteSubscription ? R.string.subredditpicker_unsubscribe : R.string.subredditpicker_remove);
    } else {
      unsubscribeItem.setTitle(R.string.subredditpicker_remove);
    }

    // Enable item change animation, until the user starts searching.
    subredditList.setItemAnimator(itemAnimator);

    popupMenu.setOnMenuItemClickListener(item -> {
      switch (item.getItemId()) {
        case R.id.action_set_subreddit_as_default:
          subscriptionRepository.get().setAsDefault(subscription);
          return true;

        case R.id.action_unsubscribe_subreddit:
          if (subscriptionRepository.get().isDefault(subscription)) {
            subscriptionRepository.get().resetDefaultSubreddit();
          }

          subscriptionRepository.get()
              .unsubscribe(subscription)
              .compose(applySchedulersCompletable())
              .subscribe(doNothingCompletable(), logError("Couldn't unsubscribe: %s", subscription));
          return true;

        case R.id.action_hide_subreddit:
          if (subscriptionRepository.get().isDefault(subscription)) {
            subscriptionRepository.get().resetDefaultSubreddit();
          }

          subscriptionRepository.get()
              .setHidden(subscription, true)
              .compose(applySchedulersCompletable())
              .subscribe(doNothingCompletable(), logError("Couldn't hide: %s", subscription));
          return true;

        case R.id.action_unhide_subreddit:
          subscriptionRepository.get()
              .setHidden(subscription, false)
              .compose(applySchedulersCompletable())
              .subscribe(doNothingCompletable(), logError("Couldn't unhide: %s", subscription));
          return true;

        default:
          throw new UnsupportedOperationException();
      }
    });

    // Apply a subtle tint on the clicked subreddit button to indicate focus.
    ColorStateList originalTintList = subredditItemView.getBackgroundTintList();
    subredditItemView.setBackgroundTintList(ColorStateList.valueOf(focusedSubredditButtonTintColor));
    popupMenu.setOnDismissListener(menu -> subredditItemView.setBackgroundTintList(originalTintList));

    popupMenu.show();
  }

  @OnClick(R.id.subredditpicker_option_add_new)
  void onClickNewSubreddit() {
    callbacks.onClickAddNewSubreddit();
  }

  public void subscribeTo(Subscribeable subscribeable) {
    Action findAndHighlightSubredditAction = () -> Views.executeOnNextLayout(subredditList, () -> {
      List<SubredditSubscription> subscriptions = subredditAdapter.getData();

      //noinspection ConstantConditions
      for (int position = 0; position < subscriptions.size(); position++) {
        SubredditSubscription subscription = subscriptions.get(position);
        if (subscription.name().equalsIgnoreCase(subscribeable.displayName())) {
          subredditAdapter.temporarilyHighlight(subscription);
          subredditAdapter.notifyItemChanged(position);

          // Smooth scrolling doesn't seem to work.
          final int finalPosition = position;
          subredditList.post(() -> subredditList.scrollToPosition(finalPosition));
          break;
        }
      }
    });

    // Enable item change animation, until the user starts searching again.
    subredditList.setItemAnimator(itemAnimator);

    subscriptions.add(subscriptionRepository.get().subscribe(subscribeable)
        .andThen(Completable.fromAction(findAndHighlightSubredditAction))
        .compose(applySchedulersCompletable())
        .subscribe(doNothingCompletable())
    );
  }
}
