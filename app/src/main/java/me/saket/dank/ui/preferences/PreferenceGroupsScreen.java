package me.saket.dank.ui.preferences;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;

import com.jakewharton.rxrelay2.BehaviorRelay;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.ScreenSavedState;
import me.saket.dank.ui.preferences.adapter.UserPreferencesAdapter;
import me.saket.dank.ui.preferences.adapter.UserPreferencesConstructor;
import me.saket.dank.ui.preferences.adapter.UserPrefsItemDiffer;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.Views;
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity;
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews;
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout;
import me.saket.dank.widgets.InboxUI.InboxRecyclerView;

/**
 * Uses custom layouts for preference items because customizing them + having custom design & controls is easier.
 */
public class PreferenceGroupsScreen extends ExpandablePageLayout {

  private static final String KEY_ACTIVE_PREFERENCE_GROUP = "activePreferenceGroup";

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.userpreferences_preferences_recyclerview) InboxRecyclerView preferenceRecyclerView;
  @BindView(R.id.userpreferences_nested_page) ExpandablePageLayout nestedPage;

  @Inject Lazy<UserPreferencesConstructor> preferencesConstructor;
  @Inject Lazy<UserPreferencesAdapter> preferencesAdapter;

  private BehaviorRelay<Optional<UserPreferenceGroup>> groupChanges = BehaviorRelay.createDefault(Optional.empty());
  private LifecycleOwnerViews.Streams lifecycle;

  public PreferenceGroupsScreen(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this, this);
    Dank.dependencyInjector().inject(this);

    //noinspection ConstantConditions
    toolbar.setNavigationOnClickListener(v -> ((UserPreferencesActivity) getContext()).onClickPreferencesToolbarUp());
    setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return Views.touchLiesOn(preferenceRecyclerView, downX, downY) && preferenceRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1);
    });

    lifecycle = LifecycleOwnerViews.create(this, ((LifecycleOwnerActivity) getContext()).lifecycle());

    setupPreferenceList();

    // TODO: background items aren't expanding properly.
    // TODO: close nested page on back press and toolbar up press.
    preferenceRecyclerView.setExpandablePage(nestedPage, toolbar);
    setNestedExpandablePage(nestedPage);
  }

  @Nullable
  @Override
  protected Parcelable onSaveInstanceState() {
    Bundle values = new Bundle();
    Optional<UserPreferenceGroup> optionalGroup = groupChanges.getValue();
    optionalGroup.ifPresent(group -> values.putSerializable(KEY_ACTIVE_PREFERENCE_GROUP, group));
    return ScreenSavedState.combine(super.onSaveInstanceState(), values);
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    ScreenSavedState savedState = (ScreenSavedState) state;
    super.onRestoreInstanceState(savedState.superSavedState());

    if (savedState.values().containsKey(KEY_ACTIVE_PREFERENCE_GROUP)) {
      //noinspection ConstantConditions
      populatePreferences((UserPreferenceGroup) savedState.values().getSerializable(KEY_ACTIVE_PREFERENCE_GROUP));
    }
  }

  public void populatePreferences(UserPreferenceGroup preferenceGroup) {
    toolbar.setTitle(preferenceGroup.titleRes);
    groupChanges.accept(Optional.of(preferenceGroup));
  }

  private void setupPreferenceList() {
    preferenceRecyclerView.setLayoutManager(preferenceRecyclerView.createLayoutManager());
    preferenceRecyclerView.setItemAnimator(null);

    preferencesAdapter.get().dataChanges()
        .take(1)
        .takeUntil(lifecycle.viewDetaches())
        .subscribe(o -> preferenceRecyclerView.setAdapter(preferencesAdapter.get()));

    groupChanges
        .observeOn(io())
        .switchMap(group -> preferencesConstructor.get().stream(getContext(), group))
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(UserPrefsItemDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle.viewDetachesFlowable())
        .subscribe(preferencesAdapter.get());

    preferencesAdapter.get().streamButtonPreferenceClicks()
        .takeUntil(lifecycle.viewDetaches())
        .subscribe(event -> preferenceRecyclerView.expandItem(event.itemPosition(), event.itemId()));
  }

// ======== EXPANDABLE PAGE ======== //

  @Override
  protected void onPageAboutToExpand(long expandAnimDuration) {

  }

  @Override
  protected void onPageCollapsed() {
    groupChanges.accept(Optional.empty());
  }
}
