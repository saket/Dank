package me.saket.dank.ui.submission

import com.squareup.moshi.Json
import net.dean.jraw.databind.Enveloped
import net.dean.jraw.databind.RedditModel
import net.dean.jraw.models.DelegatedList
import java.io.Serializable

@RedditModel
class SubmissionListing<T>(
    /** Gets the fullname of the model at the top of the next page, if it exists  */
    @Json(name = "after")
    val nextName: String?,

    // We have to write this in Java instead of Kotlin since Kotlin apparently doesn't attach the @Enveloped annotations
    // to the backing field it creates. Attaching it to a Java field or method (like we do here) DOES work.

    /** Gets the objects contained on this page  */
    @get:Enveloped
    val children: List<T>
) : DelegatedList<T>(), Serializable {

  override fun getDelegatedList(): List<T> {
    return children
  }
}
