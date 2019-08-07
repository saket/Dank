package me.saket.dank.ui.preferences.gestures.submissions

import android.content.Context
import me.saket.dank.di.Dank
import me.saket.dank.ui.subreddit.SubmissionSwipeActions
import me.saket.dank.utils.NestedOptionsPopupMenu
import me.saket.dank.utils.Optional
import java.util.*
import javax.inject.Inject

class SubmissionSwipeActionPreferenceChoicePopup(
  context: Context,
  private val forStartAction: Boolean
) : NestedOptionsPopupMenu(context) {

  @Inject
  lateinit var swipeActionsRepository: SubmissionSwipeActionsRepository

  init {
    Dank.dependencyInjector().inject(this)
    createMenuLayout(context, menuStructure(context))
  }

  private fun menuStructure(context: Context): NestedOptionsPopupMenu.MenuStructure {
    val menuItems = ArrayList<MenuStructure.SingleLineItem>()
    val swipeActions = swipeActionsRepository.unusedSwipeActions(forStartAction).blockingFirst()
    for (swipeAction in swipeActions) {
      menuItems.add(
        NestedOptionsPopupMenu.MenuStructure.SingleLineItem.create(
          swipeAction.displayNameRes,
          context.getString(swipeAction.displayNameRes),
          SubmissionSwipeActions.getSwipeActionIconRes(swipeAction)
        )
      )
    }
    return NestedOptionsPopupMenu.MenuStructure.create(Optional.empty(), menuItems)
  }

  override fun handleAction(c: Context, actionId: Int) {
    val swipeAction = SubmissionSwipeActions.getActionByLabel(actionId)
    swipeActionsRepository.addSwipeAction(swipeAction, forStartAction)
    dismiss()
  }
}
