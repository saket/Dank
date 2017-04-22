package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulersCompletable;
import static me.saket.dank.utils.RxUtils.doNothing;
import static me.saket.dank.utils.RxUtils.doNothingCompletable;
import static me.saket.dank.utils.RxUtils.doOnStartAndNext;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.setMarginBottom;
import static me.saket.dank.utils.Views.setPaddingBottom;
import static me.saket.dank.utils.Views.touchLiesOn;
import static rx.Observable.just;
import static rx.android.schedulers.AndroidSchedulers.mainThread;
import static rx.schedulers.Schedulers.io;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.Interpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxrelay.BehaviorRelay;

import net.dean.jraw.models.Subreddit;

import java.util.Collections;

import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import me.saket.dank.R;
import me.saket.dank.data.SubredditSubscription;
import me.saket.dank.di.Dank;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Completable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

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
    private static final Interpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();

    @BindView(R.id.subredditpicker_root) ViewGroup rootViewGroup;
    @BindView(R.id.subredditpicker_search) EditText searchView;
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

    private ViewGroup activityRootLayout;
    private ToolbarExpandableSheet parentSheet;
    private SubredditAdapter subredditAdapter;
    private CompositeSubscription subscriptions;
    private Callbacks callbacks;
    private SheetState sheetState;
    private BehaviorRelay<Boolean> showHiddenSubredditsSubject;
    private Runnable pendingOnWindowDetachedRunnable;

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
        void onSubredditsChanged();
    }

    public static SubredditPickerSheetView showIn(ToolbarExpandableSheet toolbarSheet, ViewGroup activityRootLayout) {
        SubredditPickerSheetView subredditPickerView = new SubredditPickerSheetView(toolbarSheet.getContext());
        subredditPickerView.setActivityRootLayout(activityRootLayout);
        subredditPickerView.setParentSheet(toolbarSheet);
        toolbarSheet.addView(subredditPickerView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return subredditPickerView;
    }

    public SubredditPickerSheetView(Context context) {
        super(context);
        inflate(context, R.layout.view_subreddit_picker_sheet, this);
        ButterKnife.bind(this, this);

        saveButton.setVisibility(INVISIBLE);
        sheetState = SheetState.BROWSE_SUBS;
        subscriptions = new CompositeSubscription();
        showHiddenSubredditsSubject = BehaviorRelay.create(false /* defaultValue */);
    }

    public boolean shouldInterceptPullToCollapse(float downX, float downY) {
        // FlexboxLayoutManager does not override methods required for View#canScrollVertically.
        // So let's intercept all pull-to-collapses if the touch is made on the list.
        return touchLiesOn(subredditList, downX, downY);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        FlexboxLayoutManager flexboxLayoutManager = new FlexboxLayoutManager();
        flexboxLayoutManager.setFlexDirection(FlexDirection.ROW);
        flexboxLayoutManager.setFlexWrap(FlexWrap.WRAP);
        flexboxLayoutManager.setAlignItems(AlignItems.STRETCH);
        subredditList.setLayoutManager(flexboxLayoutManager);

        subredditAdapter = new SubredditAdapter();
        subredditAdapter.setOnSubredditClickListener(this);
        subredditList.setAdapter(subredditAdapter);
        subredditList.setItemAnimator(null);

        setupSubredditsSearch();

        subscriptions.add(SubredditSubscriptionsSyncJob
                .progressUpdates()
                .observeOn(mainThread())
                .subscribe(setSubredditLoadProgressVisible())
        );

        // Track changes in subscriptions and send a callback if needed when this sheet collapses.
        subscriptions.add(Dank.subscriptionManager()
                .getAllIncludingHidden()
                .scan((oldSubscriptions, newSubscriptions) -> {
                    if (oldSubscriptions.size() != newSubscriptions.size()) {
                        pendingOnWindowDetachedRunnable = () -> callbacks.onSubredditsChanged();
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
                .observeOn(io())
                .switchMap(showHidden -> Dank.subscriptionManager().getAll(searchView.getText().toString(), showHidden))
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
                                return ImmutableList.<SubredditSubscription>builder()
                                        .addAll(filteredSubs)
                                        .add(SubredditSubscription.create(searchTerm, SubredditSubscription.PendingState.NONE, false))
                                        .build();
                            }
                        }
                    }
                    return filteredSubs;
                })
                .onErrorResumeNext(error -> {
                    // Don't let an error terminate the stream.
                    Timber.e(error, "Error in fetching subreddits");
                    return just(Collections.emptyList());
                })
                .observeOn(mainThread())
                .compose(doOnStartAndNext(setSubredditLoadProgressVisible()))
                .doOnNext(o -> optionsContainer.setVisibility(VISIBLE))
                .subscribe(subredditAdapter)
        );
    }

    private Action1<Boolean> setSubredditLoadProgressVisible() {
        return visible -> {
            if (visible) {
                if (subredditAdapter.getItemCount() == 0) {
                    subredditsLoadProgressView.setVisibility(View.VISIBLE);
                } else {
                    subredditsRefreshProgressView.setVisibility(View.VISIBLE);
                }

            } else {
                subredditsLoadProgressView.setVisibility(View.GONE);
                subredditsRefreshProgressView.setVisibility(View.GONE);
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
        showHiddenSubredditsSubject.call(false);

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
        PopupMenu overflowMenu = new PopupMenu(getContext(), optionView, Gravity.NO_GRAVITY, 0, R.style.DankOverflowMenu);
        overflowMenu.inflate(R.menu.menu_subredditpicker_overflow_menu);

        overflowMenu.getMenu().findItem(R.id.action_show_hidden_subreddits).setVisible(!showHiddenSubredditsSubject.getValue());
        overflowMenu.getMenu().findItem(R.id.action_hide_hidden_subreddits).setVisible(showHiddenSubredditsSubject.getValue());

        // Enable item change animation, until the user starts searching.
        subredditList.setItemAnimator(new DefaultItemAnimator());

        overflowMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_show_hidden_subreddits:
                    showHiddenSubredditsSubject.call(true);
                    return true;

                case R.id.action_hide_hidden_subreddits:
                    showHiddenSubredditsSubject.call(false);
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
        PopupMenu popupMenu = new PopupMenu(getContext(), subredditItemView);
        popupMenu.inflate(R.menu.menu_subredditpicker_subreddit_options);

        popupMenu.getMenu().findItem(R.id.action_set_subreddit_as_default).setVisible(!Dank.subscriptionManager().isDefault(subscription));
        popupMenu.getMenu().findItem(R.id.action_unsubscribe_subreddit).setVisible(!Dank.subscriptionManager().isFrontpage(subscription.name()));
        popupMenu.getMenu().findItem(R.id.action_hide_subreddit).setVisible(!subscription.isHidden());
        popupMenu.getMenu().findItem(R.id.action_unhide_subreddit).setVisible(subscription.isHidden());

        // Enable item change animation, until the user starts searching.
        subredditList.setItemAnimator(new DefaultItemAnimator());

        Action1<SubredditSubscription> resetDefaultIfDefaultWasRemoved = removedSub -> {
            if (Dank.subscriptionManager().isDefault(removedSub)) {
                Dank.subscriptionManager().resetDefaultSubreddit();
            }
        };

        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_set_subreddit_as_default:
                    Dank.subscriptionManager().setAsDefault(subscription);
                    return true;

                case R.id.action_unsubscribe_subreddit:
                    resetDefaultIfDefaultWasRemoved.call(subscription);

                    Dank.subscriptionManager()
                            .unsubscribe(subscription)
                            .compose(applySchedulersCompletable())
                            .subscribe(doNothingCompletable(), logError("Couldn't unsubscribe: %s", subscription));
                    return true;

                case R.id.action_hide_subreddit:
                    resetDefaultIfDefaultWasRemoved.call(subscription);

                    Dank.subscriptionManager()
                            .setHidden(subscription, true)
                            .compose(applySchedulersCompletable())
                            .subscribe(doNothingCompletable(), logError("Couldn't hide: %s", subscription));
                    return true;

                case R.id.action_unhide_subreddit:
                    Dank.subscriptionManager()
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

    /**
     * Finds the View for <var>newSubreddit</var> in the list and highlights it temporarily.
     */
    public void subscribeTo(Subreddit newSubreddit) {
        Action0 findAndHighlightSubredditAction = () -> Views.executeOnNextLayout(subredditList, () -> {
            for (int position = 0; position < subredditAdapter.getItemCount(); position++) {
                SubredditSubscription subscription = subredditAdapter.getItem(position);
                if (subscription.name().equalsIgnoreCase(newSubreddit.getDisplayName())) {
                    subredditAdapter.temporarilyHighlight(subscription);
                    subredditAdapter.notifyItemChanged(position);

                    // FlexboxLayoutManager doesn't support smooth scrolling :(
                    final int finalPosition = position;
                    subredditList.post(() -> subredditList.scrollToPosition(finalPosition));
                    break;
                }
            }
        });

        // Enable item change animation, until the user starts searching.
        subredditList.setItemAnimator(new DefaultItemAnimator());

        subscriptions.add(Dank.subscriptionManager()
                .subscribe(newSubreddit)
                // Add some delay to ensure the list's dataset has been updated with this new subreddit.
                .andThen(Completable.fromAction(findAndHighlightSubredditAction))
                .compose(applySchedulersCompletable())
                .subscribe(doNothingCompletable(), doNothing())
        );
    }

}
