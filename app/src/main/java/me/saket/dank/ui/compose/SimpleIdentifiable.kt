package me.saket.dank.ui.compose

import android.os.Parcelable
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import net.dean.jraw.models.Identifiable

@Parcelize
@JsonClass(generateAdapter = true)
data class SimpleIdentifiable(override val fullName: String, override val id: String) : Identifiable, Parcelable {

  override val uniqueId: String
    get() = fullName

  companion object {
    fun from(identifiable: Identifiable): SimpleIdentifiable {
      return identifiable.let { SimpleIdentifiable(fullName = it.fullName, id = it.id) }
    }

    fun from(fullName: String): SimpleIdentifiable {
      return SimpleIdentifiable(fullName = fullName, id = fullName.substring(3))
    }
  }
}
