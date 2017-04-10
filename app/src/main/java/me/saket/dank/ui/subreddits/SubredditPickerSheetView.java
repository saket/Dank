package me.saket.dank.ui.subreddits;

import static me.saket.dank.utils.RxUtils.applySchedulers;
import static me.saket.dank.utils.RxUtils.doOnStartAndFinish;
import static me.saket.dank.utils.RxUtils.logError;
import static me.saket.dank.utils.Views.setHeight;
import static me.saket.dank.utils.Views.setMarginBottom;
import static me.saket.dank.utils.Views.setPaddingBottom;
import static me.saket.dank.utils.Views.touchLiesOn;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
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
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindDimen;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import me.saket.dank.R;
import me.saket.dank.data.DankSubreddit;
import me.saket.dank.di.Dank;
import me.saket.dank.widgets.ToolbarExpandableSheet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Lets the user:
 * - Pick
 * - Reorder
 * - Sort
 * - Add new
 * - Set default.
 */
public class SubredditPickerSheetView extends FrameLayout {

    // TODO: 28/02/17 Cache this somewhere else.
    private static final List<DankSubreddit> USER_SUBREDDITS = new ArrayList<>();
    private static final int ANIM_DURATION = 300;
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

    @BindString(R.string.frontpage_subreddit_name) String frontpageSubredditName;
    @BindDimen(R.dimen.subreddit_picker_sheet_height) int collapsedPickerSheetHeight;
    @BindDimen(R.dimen.subreddit_picker_sheet_bottom_margin) int parentSheetBottomMarginForShadows;

    private ViewGroup activityRootLayout;
    private ToolbarExpandableSheet parentSheet;
    private SubredditAdapter subredditAdapter;
    private CompositeSubscription subscriptions = new CompositeSubscription();

    public static SubredditPickerSheetView show(ToolbarExpandableSheet toolbarSheet, ViewGroup activityRootLayout) {
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
        subredditList.setAdapter(subredditAdapter);
        subredditList.setItemAnimator(null);

        if (!USER_SUBREDDITS.isEmpty()) {
            setSubredditLoadProgressVisible().call(false);
            subredditAdapter.updateData(USER_SUBREDDITS);
            optionsContainer.setVisibility(VISIBLE);

        } else {
            Subscription apiSubscription = (Dank.reddit().isUserLoggedIn() ? loggedInSubreddits() : loggedOutSubreddits())
                    .compose(doOnStartAndFinish(setSubredditLoadProgressVisible()))
                    .map(subreddits -> {
                        subreddits.add(0, DankSubreddit.createFrontpage(frontpageSubredditName));
                        return subreddits;
                    })
                    .doOnNext(subreddits -> USER_SUBREDDITS.addAll(subreddits))
                    .doOnNext(__ -> optionsContainer.setVisibility(VISIBLE))
                    .subscribe(subredditAdapter, logError("Failed to get subreddits"));
            subscriptions.add(apiSubscription);
        }

        setupSearch();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (subscriptions != null) {
            subscriptions.clear();
        }
        super.onDetachedFromWindow();
    }

    public void setOnSubredditClickListener(OnSubredditClickListener listener) {
        subredditAdapter.setOnSubredditClickListener(listener);
    }

// ======== END PUBLIC APIs ======== //

    public void setActivityRootLayout(ViewGroup activityRootLayout) {
        this.activityRootLayout = activityRootLayout;
    }

    public void setParentSheet(ToolbarExpandableSheet parentSheet) {
        this.parentSheet = parentSheet;
    }

    private Observable<List<DankSubreddit>> loggedInSubreddits() {
        return Dank.reddit()
                .withAuth(Dank.reddit().userSubreddits())
                .compose(applySchedulers());
    }

    private Observable<List<DankSubreddit>> loggedOutSubreddits() {
        List<String> defaultSubreddits = Arrays.asList(getResources().getStringArray(R.array.default_subreddits));
        ArrayList<DankSubreddit> dankSubreddits = new ArrayList<>();
        for (String subredditName : defaultSubreddits) {
            dankSubreddits.add(DankSubreddit.create(subredditName));
        }
        return Observable.just(dankSubreddits);
    }

    @OnEditorAction(R.id.subredditpicker_search)
    boolean onClickSearchFieldEnterKey(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            String searchTerm = searchView.getText().toString().trim();
            subredditAdapter.getOnSubredditClickListener().onClickSubreddit(DankSubreddit.create(searchTerm));
            return true;
        }
        return false;
    }

    private void setupSearch() {
        Subscription subscription = RxTextView.textChanges(searchView)
                .skip(1)
                .debounce(100, TimeUnit.MILLISECONDS)
                .map(searchTerm -> {
                    // Since CharSequence is mutable, it's important
                    // to create a copy before doing a debounce().
                    return searchTerm.toString().trim();
                })
                .map(searchTerm -> {
                    if (searchTerm.isEmpty()) {
                        return USER_SUBREDDITS;
                    }

                    boolean unknownSubreddit = false;

                    List<DankSubreddit> filteredSubreddits = new ArrayList<>(USER_SUBREDDITS.size());
                    for (DankSubreddit userSubreddit : USER_SUBREDDITS) {
                        if (userSubreddit.displayName().toLowerCase(Locale.ENGLISH).contains(searchTerm.toLowerCase())) {
                            filteredSubreddits.add(userSubreddit);

                            if (userSubreddit.displayName().equalsIgnoreCase(searchTerm)) {
                                unknownSubreddit = true;
                            }
                        }
                    }

                    if (!unknownSubreddit) {
                        filteredSubreddits.add(DankSubreddit.create(searchTerm));
                    }

                    return filteredSubreddits;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(subredditAdapter, logError("Search error"));
        subscriptions.add(subscription);
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

    @OnClick(R.id.subredditpicker_option_manage)
    void onClickEditSubreddits() {
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
        heightAnimator.setDuration(ANIM_DURATION);
        heightAnimator.start();
    }

    @OnClick(R.id.subredditpicker_save_fab)
    void onClickSaveSubreddits() {
        // TODO: Save order.

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
        heightAnimator.setDuration(ANIM_DURATION);
        heightAnimator.start();
    }

    public boolean shouldInterceptPullToCollapse(float downX, float downY) {
        // FlexboxLayoutManager does not override methods required for View#canScrollVertically.
        // So let's intercept all pull-to-collapses if the touch is made on the list.
        return touchLiesOn(subredditList, downX, downY);
    }

    private ViewPropertyAnimator animateAlpha(View view, float toAlpha) {
        return view.animate().alpha(toAlpha).setDuration(ANIM_DURATION).setInterpolator(ANIM_INTERPOLATOR);
    }

}
