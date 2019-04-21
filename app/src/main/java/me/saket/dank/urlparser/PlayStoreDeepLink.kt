package me.saket.dank.urlparser

import android.content.Context
import android.content.Intent
import me.saket.dank.ui.UrlRouter
import me.saket.dank.utils.Intents

data class PlayStoreDeepLink(val url: String): Deeplink {

  private val packageName = "com.android.vending"

  override fun intent(context: Context): Intent {
    return if (UrlRouter.isPackageNameInstalledAndEnabled(context, packageName)) {
      Intents.createForOpeningUrl(url).setPackage(packageName)
    } else {
      Intents.createForPlayStoreListing(context, url)
    }
  }
}