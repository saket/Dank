package me.saket.dank.ui.submission.adapter

import android.annotation.SuppressLint
import android.text.Selection
import android.text.Spannable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.RecyclerView
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import me.saket.bettermovementmethod.BetterLinkMovementMethod
import me.saket.dank.R
import me.saket.dank.data.LocallyPostedComment
import me.saket.dank.data.SpannableWithTextEquality
import me.saket.dank.data.SwipeEvent
import me.saket.dank.reply.PendingSyncReply
import me.saket.dank.ui.UiEvent
import me.saket.dank.ui.submission.CommentSwipeActionsProvider
import me.saket.dank.ui.submission.events.CommentClicked
import me.saket.dank.ui.submission.events.ReplyRetrySendClickEvent
import me.saket.dank.utils.DankLinkMovementMethod
import me.saket.dank.widgets.IndentedLayout
import me.saket.dank.widgets.swipe.SwipeableLayout
import me.saket.dank.widgets.swipe.ViewHolderWithSwipeActions
import javax.inject.Inject

interface SubmissionLocalComment {

  enum class PartialChange {
    BYLINE
  }

  data class UiModel(
      val adapterId: Long,
      val byline: SpannableWithTextEquality,
      val body: SpannableWithTextEquality,
      @ColorInt val bylineTextColor: Int,
      @ColorInt val bodyTextColor: Int,
      val bodyMaxLines: Int,
      val indentationDepth: Int,
      val comment: LocallyPostedComment,
      @ColorRes val backgroundColorRes: Int,
      val isCollapsed: Boolean
  ) : SubmissionScreenUiModel {

    override fun adapterId(): Long {
      return adapterId
    }

    override fun type(): SubmissionCommentRowType {
      return SubmissionCommentRowType.LOCAL_USER_COMMENT
    }
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), ViewHolderWithSwipeActions {
    private val indentedLayout: IndentedLayout = itemView.findViewById(R.id.item_comment_indented_container)
    private val bylineView: TextView = itemView.findViewById(R.id.item_comment_byline)
    private val bodyView: TextView = itemView.findViewById(R.id.item_comment_body)

    lateinit var uiModel: UiModel

    private var hasSelection: Boolean = false

    init {
      bodyView.setTextIsSelectable(true)
    }

    fun setBodyLinkMovementMethod(movementMethod: DankLinkMovementMethod) {
      bodyView.movementMethod = movementMethod
    }

    fun setupGestures(commentSwipeActionsProvider: CommentSwipeActionsProvider) {
      swipeableLayout.setSwipeActionIconProvider(commentSwipeActionsProvider.iconProvider())
      swipeableLayout.setSwipeActions(commentSwipeActionsProvider.actions())
      swipeableLayout.setOnPerformSwipeActionListener { _, _ -> /* TODO.*/ }
    }

    fun setupClicks(uiEvents: PublishRelay<UiEvent>) {
      itemView.setOnClickListener {
        if (hasSelection) {
          return@setOnClickListener
        }

        when (uiModel.comment.pendingSyncReply.state()) {
          PendingSyncReply.State.FAILED -> {
            uiEvents.accept(ReplyRetrySendClickEvent.create(uiModel.comment.pendingSyncReply))
          }
          else -> {
            val willCollapse = !uiModel.isCollapsed
            uiEvents.accept(CommentClicked.create(uiModel.comment, adapterPosition, itemView, willCollapse))
          }
        }
      }

      itemView.setOnLongClickListener { !uiModel.isCollapsed }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun forwardTouchEventsToBackground(linkMovementMethod: BetterLinkMovementMethod) {
      bodyView.setOnTouchListener { _, event ->
        val action = event.action
        val bodyViewText = bodyView.text

        if (action == MotionEvent.ACTION_DOWN) {
          // Capture selection state to prevent collapsing if text is still selected
          val selectionStart = Selection.getSelectionStart(bodyViewText)
          hasSelection = selectionStart > -1 && selectionStart != Selection.getSelectionEnd(bodyViewText)
        }

        if (linkMovementMethod.onTouchEvent(bodyView, bodyViewText as Spannable, event)) {
          // handled by link movement method, stop event propagation
          return@setOnTouchListener true
        }

        if (action == MotionEvent.ACTION_DOWN) {
          // Workaround for "TextView does not support text selection"
          bodyView.isEnabled = false
          bodyView.isEnabled = true
        }

        // Dispatch touch event for both item and textView
        //  to get both touch ripple and text selection
        val handledByItem = itemView.onTouchEvent(event)
        val handledByBody = !uiModel.isCollapsed && bodyView.onTouchEvent(event) // forbid selection when collapsed

        handledByBody || handledByItem
      }
    }

    fun render() {
      itemView.setBackgroundResource(uiModel.backgroundColorRes)
      indentedLayout.setIndentationDepth(uiModel.indentationDepth)
      bylineView.text = uiModel.byline
      bylineView.setTextColor(uiModel.bylineTextColor)
      bodyView.text = uiModel.body
      bodyView.setTextColor(uiModel.bodyTextColor)
      bodyView.maxLines = uiModel.bodyMaxLines

      // TODO: Add support for locally posted replies too.
      swipeableLayout.setSwipeEnabled(false)
    }

    fun renderPartialChanges(payloads: List<Any>) {
      for (payload in payloads) {

        @Suppress("UNCHECKED_CAST")
        for (partialChange in payload as List<PartialChange>) {
          when (partialChange) {
            PartialChange.BYLINE -> bylineView.text = uiModel.byline
          }
        }
      }
    }

    override fun getSwipeableLayout(): SwipeableLayout {
      return itemView as SwipeableLayout
    }
  }

  class Adapter @Inject constructor(
    private val linkMovementMethod: DankLinkMovementMethod,
    private val swipeActionsProvider: CommentSwipeActionsProvider
  ) : SubmissionScreenUiModel.Adapter<UiModel, ViewHolder> {

    private val uiEvents = PublishRelay.create<UiEvent>()

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
      return ViewHolder(inflater.inflate(R.layout.list_item_submission_comment, parent, false)).apply {
        setBodyLinkMovementMethod(linkMovementMethod)
        setupGestures(swipeActionsProvider)
        setupClicks(uiEvents)
        forwardTouchEventsToBackground(linkMovementMethod)
      }
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel) {
      holder.uiModel = uiModel
      holder.render()
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel, payloads: List<Any>) {
      holder.uiModel = uiModel
      holder.renderPartialChanges(payloads)
    }

    fun swipeEvents(): PublishRelay<SwipeEvent> {
      return swipeActionsProvider.swipeEvents
    }

    override fun uiEvents(): Observable<out UiEvent> {
      return uiEvents
    }
  }
}
