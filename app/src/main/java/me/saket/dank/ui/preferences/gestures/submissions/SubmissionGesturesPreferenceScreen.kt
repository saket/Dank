package me.saket.dank.ui.preferences.gestures.submissions

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.RelativeLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.BackpressureStrategy
import me.saket.dank.R
import me.saket.dank.di.Dank
import me.saket.dank.ui.preferences.UserPreferenceNestedScreen
import me.saket.dank.ui.preferences.gestures.GesturePreferenceItemDiffer
import me.saket.dank.ui.preferences.gestures.GesturePreferencesAdapter
import me.saket.dank.ui.preferences.gestures.GesturePreferencesSwipeAction
import me.saket.dank.ui.preferences.gestures.GesturePreferencesUiConstructor
import me.saket.dank.ui.subreddit.SubmissionSwipeActionsProvider
import me.saket.dank.utils.*
import me.saket.dank.utils.itemanimators.SlideLeftAlphaAnimator
import me.saket.dank.widgets.swipe.RecyclerSwipeListener
import javax.inject.Inject

class SubmissionGesturesPreferenceScreen(context: Context, attrs: AttributeSet?) :
  RelativeLayout(context, attrs),
  UserPreferenceNestedScreen {

  @BindView(R.id.userpreferences_gestures_recyclerview)
  lateinit var gesturesRecyclerView: RecyclerView

  @Inject
  lateinit var swipeActionsAdapter: GesturePreferencesAdapter

  @Inject
  lateinit var swipeActionsProvider: SubmissionSwipeActionsProvider

  @Inject
  lateinit var uiConstructor: GesturePreferencesUiConstructor

  @Inject
  lateinit var swipeActionsRepository: SubmissionSwipeActionsRepository

  private lateinit var toolbar: Toolbar

  override fun onFinishInflate() {
    super.onFinishInflate()

    Dank.dependencyInjector().inject(this)
    ButterKnife.bind(this)

    toolbar = findViewById(R.id.userpreferences_gestures_toolbar)
    toolbar.setTitle(R.string.userprefs_customize_submission_gestures)

    setupGestureList()
  }

  override fun setNavigationOnClickListener(listener: OnClickListener) {
    toolbar.setNavigationOnClickListener(listener)
  }

  override fun onInterceptPullToCollapseGesture(
    event: MotionEvent,
    downX: Float,
    downY: Float,
    upwardPagePull: Boolean
  ): Boolean {
    return Views.touchLiesOn(gesturesRecyclerView, downX, downY)
        && gesturesRecyclerView.canScrollVertically(if (upwardPagePull) 1 else -1)
  }

  private fun setupGestureList() {
    val animator = SlideLeftAlphaAnimator.create()
    animator.supportsChangeAnimations = false
    gesturesRecyclerView.itemAnimator = animator
    gesturesRecyclerView.layoutManager = LinearLayoutManager(context)
    gesturesRecyclerView.adapter = swipeActionsAdapter

    // Drags.
    val dragHelper = ItemTouchHelper(createDragAndDropCallbacks())
    dragHelper.attachToRecyclerView(gesturesRecyclerView)
    swipeActionsAdapter.streamDragStarts()
      .takeUntil(RxView.detaches(this))
      .subscribe(dragHelper::startDrag)

    // Swipes.
    // WARNING: THIS TOUCH LISTENER FOR SWIPE SHOULD BE REGISTERED AFTER DRAG-DROP LISTENER.
    // Drag-n-drop's long-press listener does not get canceled if a row is being swiped.
    gesturesRecyclerView.addOnItemTouchListener(RecyclerSwipeListener(gesturesRecyclerView))
    swipeActionsAdapter.streamDeletes()
      .takeUntil(RxView.detaches(this))
      .subscribe { actionWithDirection ->
        swipeActionsRepository.removeSwipeAction(actionWithDirection.swipeAction, actionWithDirection.isStartAction)
      }

    // Adds.
    swipeActionsAdapter.streamAddClicks()
      .takeUntil(RxView.detaches(this))
      .subscribe { (view, uiModel) ->
        val popup =
          SubmissionSwipeActionPreferenceChoicePopup(
            context,
            uiModel.isStartAction
          )
        popup.showAtLocation(view, Gravity.TOP or Gravity.START, Views.locationOnScreen(view))
      }

    uiConstructor.stream(context)
      .map { it.rowUiModels }
      .toFlowable(BackpressureStrategy.LATEST)
      .compose(RxDiffUtil.calculate(GesturePreferenceItemDiffer))
      .toObservable()
      .distinctUntilChanged { pair1, pair2 -> pair1.first() == pair2.first() }
      .compose(RxUtils.applySchedulers())
      .takeUntil(RxView.detaches(this))
      .subscribe(swipeActionsAdapter)
  }

  private fun createDragAndDropCallbacks(): ItemTouchHelperDragAndDropCallback {
    return object : ItemTouchHelperDragAndDropCallback() {
      override fun onItemMove(source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        val sourceViewHolder = source as GesturePreferencesSwipeAction.ViewHolder
        val targetViewHolder = target as GesturePreferencesSwipeAction.ViewHolder

        val isStartAction = sourceViewHolder.uiModel.isStartAction

        val fromPosition = sourceViewHolder.adapterPosition
        val toPosition = targetViewHolder.adapterPosition

        val uiModels = swipeActionsAdapter.data.toMutableList()
        val originalSwipeActions = uiModels
          .filterIsInstance<GesturePreferencesSwipeAction.UiModel>()
          .filter { it.isStartAction == isStartAction }
          .map { it.swipeAction }

        val reorderedSwipeActions = uiModels
          .apply { move(fromPosition, toPosition) }
          .filterIsInstance<GesturePreferencesSwipeAction.UiModel>()
          .filter { it.isStartAction == isStartAction }
          .map { it.swipeAction }

        if (originalSwipeActions != reorderedSwipeActions) {
          swipeActionsRepository.setSwipeActions(reorderedSwipeActions, isStartAction)
          return true
        }

        return false
      }
    }
  }
}
