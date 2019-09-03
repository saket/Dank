package me.saket.dank.ui.preferences

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import butterknife.ButterKnife
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.Lazy
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.io
import kotterknife.bindView
import me.saket.dank.R
import me.saket.dank.di.Dank
import me.saket.dank.ui.ScreenSavedState
import me.saket.dank.ui.UrlRouter
import me.saket.dank.ui.preferences.adapter.UserPreferencesAdapter
import me.saket.dank.ui.preferences.adapter.UserPreferencesConstructor
import me.saket.dank.ui.preferences.adapter.UserPrefsItemDiffer
import me.saket.dank.urlparser.Link
import me.saket.dank.urlparser.MediaLink
import me.saket.dank.urlparser.RedditUserLink
import me.saket.dank.utils.BackPressCallback
import me.saket.dank.utils.Optional
import me.saket.dank.utils.RxDiffUtil
import me.saket.dank.utils.Views
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator
import me.saket.dank.utils.lifecycle.LifecycleOwnerActivity
import me.saket.dank.utils.lifecycle.LifecycleOwnerViews
import me.saket.dank.utils.lifecycle.ViewLifecycleEvent
import me.saket.dank.widgets.InboxUI.ExpandablePageLayout
import me.saket.dank.widgets.InboxUI.InboxRecyclerView
import javax.inject.Inject

/**
 * Uses custom layouts for preference items because customizing them + having custom design & controls is easier.
 */
class PreferenceGroupsScreen(context: Context, attrs: AttributeSet) :
    ExpandablePageLayout(context, attrs), PreferenceButtonClickHandler {

  @Inject
  lateinit var preferencesConstructor: Lazy<UserPreferencesConstructor>

  @Inject
  lateinit var preferencesAdapter: Lazy<UserPreferencesAdapter>

  @Inject
  lateinit var urlRouter: Lazy<UrlRouter>

  @LayoutRes
  private var expandedPageLayoutRes: Int? = null

  private val toolbar by bindView<Toolbar>(R.id.toolbar)
  private val preferenceRecyclerView by bindView<InboxRecyclerView>(R.id.userpreferences_preferences_recyclerview)
  private val nestedPage by bindView<ExpandablePageLayout>(R.id.userpreferences_nested_page)

  private val groupChanges = BehaviorRelay.createDefault(Optional.empty<UserPreferenceGroup>())
  private lateinit var lifecycle: LifecycleOwnerViews.Streams

  override fun onFinishInflate() {
    super.onFinishInflate()
    ButterKnife.bind(this, this)
    Dank.dependencyInjector().inject(this)

    toolbar.setNavigationOnClickListener { (context as UserPreferencesActivity).onClickPreferencesToolbarUp() }

    setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(preferenceRecyclerView))

    lifecycle = LifecycleOwnerViews.create(this, (context as LifecycleOwnerActivity).lifecycle())

    setupPreferenceList()

    preferenceRecyclerView.setExpandablePage(nestedPage, toolbar)
    setNestedExpandablePage(nestedPage)
  }

  override fun onSaveInstanceState(): Parcelable? {
    val values = Bundle()

    groupChanges.value.ifPresent { group ->
      values.putSerializable(KEY_ACTIVE_PREFERENCE_GROUP, group)
    }

    expandedPageLayoutRes?.let {
      values.putInt(KEY_EXPANDED_PAGE_LAYOUT_RES, it)
    }
    preferenceRecyclerView.saveExpandableState(values)

    return ScreenSavedState.combine(super.onSaveInstanceState(), values)
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    val savedState = state as ScreenSavedState
    super.onRestoreInstanceState(savedState.superSavedState())

    if (savedState.values().containsKey(KEY_ACTIVE_PREFERENCE_GROUP)) {
      val retainedGroup = savedState.values().getSerializable(KEY_ACTIVE_PREFERENCE_GROUP) as UserPreferenceGroup
      populatePreferences(retainedGroup)
    }

    preferenceRecyclerView.restoreExpandableState(savedState.values())
    if (savedState.values().containsKey(KEY_EXPANDED_PAGE_LAYOUT_RES)) {
      val retainedExpandablePageRes = savedState.values().getInt(KEY_EXPANDED_PAGE_LAYOUT_RES)
      inflateNestedPageLayout(retainedExpandablePageRes)
    }
  }

  fun populatePreferences(preferenceGroup: UserPreferenceGroup) {
    toolbar.setTitle(preferenceGroup.titleRes)
    groupChanges.accept(Optional.of(preferenceGroup))
  }

  private fun setupPreferenceList() {
    val itemAnimator = SlideUpAlphaAnimator.create()
    itemAnimator.supportsChangeAnimations = false
    preferenceRecyclerView.itemAnimator = itemAnimator
    preferenceRecyclerView.layoutManager = preferenceRecyclerView.createLayoutManager()

    preferencesAdapter.get().dataChanges()
        .take(1)
        .takeUntil<ViewLifecycleEvent>(lifecycle.viewDetaches())
        .subscribe { preferenceRecyclerView.adapter = preferencesAdapter.get() }

    groupChanges
        .observeOn(io())
        .switchMap { group -> preferencesConstructor.get().stream(context, group) }
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(UserPrefsItemDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle.viewDetachesFlowable())
        .subscribe(preferencesAdapter.get())

    // Button clicks.
    preferencesAdapter.get().streamButtonClicks()
        .takeUntil<ViewLifecycleEvent>(lifecycle.viewDetaches())
        .subscribe { it.clickListener().onClick(this, it) }

    // Switch clicks.
    preferencesAdapter.get().streamSwitchToggles()
        .takeUntil<ViewLifecycleEvent>(lifecycle.viewDetaches())
        .subscribe { it.preference().set(it.isChecked) }
  }

  fun onInterceptBackPress(): BackPressCallback {
    return if (nestedPage.isExpandedOrExpanding) {
      preferenceRecyclerView.collapse()
      BackPressCallback.asIntercepted()
    } else {
      BackPressCallback.asIgnored()
    }
  }

  override fun show(popupBuilder: MultiOptionPreferencePopup.Builder<*>, viewHolder: RecyclerView.ViewHolder) {
    val showLocation = Point(0, viewHolder.itemView.top + Views.statusBarHeight(resources))

    // Align with padding.
    val padding = resources.getDimensionPixelSize(R.dimen.userprefs_item_padding_for_preference_popups)
    showLocation.offset(padding, padding)

    popupBuilder
        .build(context)
        .showAtLocation(viewHolder.itemView, Gravity.NO_GRAVITY, showLocation)
  }

  override fun openIntent(intent: Intent) {
    context.startActivity(intent)
  }

  override fun openLink(link: Link) {
    if (link is RedditUserLink || link is MediaLink) {
      throw UnsupportedOperationException("Use other variants of forLink() instead.")
    }

    urlRouter.get().forLink(link)
        .expandFromBelowToolbar()
        .open(context)
  }

  // ======== EXPANDABLE PAGE ======== //

  override fun expandNestedPage(@LayoutRes nestedLayoutRes: Int, viewHolderToExpand: RecyclerView.ViewHolder) {
    inflateNestedPageLayout(nestedLayoutRes)

    val itemViewPosition = preferenceRecyclerView.indexOfChild(viewHolderToExpand.itemView)
    val itemId = viewHolderToExpand.itemId
    nestedPage.post { preferenceRecyclerView.expandItem(itemViewPosition, itemId) }
  }

  private fun inflateNestedPageLayout(@LayoutRes nestedLayoutRes: Int) {
    expandedPageLayoutRes = nestedLayoutRes

    if (nestedPage.childCount > 0) {
      nestedPage.removeAllViews()
    }

    val nestedPageView = LayoutInflater.from(context).inflate(nestedLayoutRes, nestedPage, false)
    val nestedPageScreen = nestedPageView as UserPreferenceNestedScreen
    nestedPageScreen.setNavigationOnClickListener { preferenceRecyclerView.collapse() }
    nestedPage.setPullToCollapseIntercepter(nestedPageScreen)
    nestedPage.addView(nestedPageView)
  }

  override fun onPageAboutToExpand(expandAnimDuration: Long) {}

  override fun onPageCollapsed() {
    groupChanges.accept(Optional.empty())
    expandedPageLayoutRes = null
  }

  companion object {
    private const val KEY_ACTIVE_PREFERENCE_GROUP = "activePreferenceGroup"
    private const val KEY_EXPANDED_PAGE_LAYOUT_RES = "expandedPageLayoutRes"
  }
}
