package me.saket.dank.ui.preferences.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.f2prateek.rx.preferences2.Preference
import com.jakewharton.rxrelay2.PublishRelay
import me.saket.dank.R
import me.saket.dank.ui.preferences.events.UserPreferenceSwitchToggleEvent
import javax.inject.Inject

interface UserPreferenceSwitch {

  data class UiModel @JvmOverloads constructor(
    val title: String,
    val summary: String? = null,
    val isChecked: Boolean,
    val preference: Preference<Boolean>,
    val isEnabled: Boolean = true
  ) : UserPreferencesScreenUiModel {
    override fun adapterId() = preference.key().hashCode().toLong()

    override fun type() = UserPreferencesScreenUiModel.Type.SWITCH
  }

  class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    @BindView(R.id.item_preferences_switch_title) lateinit var titleSwitchView: Switch
    @BindView(R.id.item_preferences_switch_summary) lateinit var summaryView: TextView

    lateinit var uiModel: UiModel

    init {
      ButterKnife.bind(this, itemView)

      itemView.setOnClickListener { titleSwitchView.performClick() }
    }

    fun render() {
      titleSwitchView.text = uiModel.title
      titleSwitchView.isChecked = uiModel.isChecked
      titleSwitchView.isEnabled = uiModel.isEnabled
      summaryView.text = uiModel.summary
      summaryView.isEnabled = uiModel.isEnabled
      itemView.isEnabled = uiModel.isEnabled

      titleSwitchView.gravity = if (uiModel.summary != null) Gravity.TOP else Gravity.CENTER_VERTICAL
      summaryView.visibility = if (uiModel.summary != null) View.VISIBLE else View.GONE
    }
  }

  class Adapter @Inject constructor() : UserPreferencesScreenUiModel.ChildAdapter<UiModel, ViewHolder> {
    val itemClicks: PublishRelay<UserPreferenceSwitchToggleEvent> = PublishRelay.create()

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
      val holder = ViewHolder(inflater.inflate(R.layout.list_item_preference_switch, parent, false))

      holder.titleSwitchView.setOnCheckedChangeListener { _, isChecked ->
        itemClicks.accept(UserPreferenceSwitchToggleEvent.create(holder.uiModel.preference, isChecked))
      }
      return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, uiModel: UiModel) {
      holder.uiModel = uiModel
      holder.render()
    }
  }
}
