package me.saket.dank.urlparser

import android.content.Context
import android.content.Intent
import me.saket.dank.ui.UrlRouter
import me.saket.dank.utils.Intents

data class YouTubeDeepLink(val url: String): Deeplink {

  private val packageName = "com.google.android.youtube"

  override fun intent(context: Context): Intent {
    return if (UrlRouter.isPackageNameInstalledAndEnabled(context, packageName)) {
      Intents.createForOpeningUrl(url).setPackage(packageName)
    } else {
      Intents.createForYouTube(url)
    }
  }
}