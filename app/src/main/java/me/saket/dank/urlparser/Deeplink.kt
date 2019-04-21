package me.saket.dank.urlparser

import android.content.Context
import android.content.Intent

interface Deeplink {

  fun intent(context: Context): Intent
}