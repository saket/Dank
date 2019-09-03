package me.saket.dank.ui.preferences.gestures

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import me.saket.dank.R
import javax.inject.Inject

interface GesturePreferencesSwipeActionPlaceholder {
  data class UiModel(
    val isStartAction: Boolean
  ) : GesturePreferenceUiModel {
    override fun adapterId() = hashCode().toLong()

    override fun type() = GesturePreferenceUiModel.Type.SWIPE_ACTION_PLACEHOLDER
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.item_gesturepreferences_swipeaction_add)
    lateinit var addButton: Button

    lateinit var uiModel: UiModel

    init {
      ButterKnife.bind(this, itemView)
    }
  }

  class Adapter @Inject constructor() : GesturePreferenceUiModel.ChildAdapter<UiModel, ViewHolder> {
    private val addClicks = PublishRelay.create<Pair<View, UiModel>>()

    @CheckResult
    fun streamAddClicks(): Observable<Pair<View, UiModel>> = addClicks

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
      val view = inflater.inflate(R.layout.list_item_gesture_preference_swipe_action_placeholder, parent, false)
      val holder = ViewHolder(view)
      holder.addButton.setOnClickListener {
        addClicks.accept(it to holder.uiModel)
      }
      return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel) {
      holder.uiModel = uiModel
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel, payloads: List<Any>) {
      throw UnsupportedOperationException()
    }
  }
}
