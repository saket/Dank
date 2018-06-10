package me.saket.dank.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

import me.saket.dank.reply.PendingSyncReply
import me.saket.dank.ui.appshortcuts.AppShortcut
import me.saket.dank.ui.subscriptions.SubredditSubscription
import me.saket.dank.ui.user.messages.CachedMessage
import timber.log.Timber

class DankSqliteOpenHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

  override fun onCreate(db: SQLiteDatabase) {
    db.execSQL(SubredditSubscription.QUERY_CREATE_TABLE)
    db.execSQL(CachedMessage.QUERY_CREATE_TABLE)
    db.execSQL(PendingSyncReply.QUERY_CREATE_TABLE)
    db.execSQL(AppShortcut.QUERY_CREATE_TABLE)
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    Timber.d("onUpgrade() -> from %s to %s", oldVersion, newVersion)

    if (oldVersion == 1 && newVersion == 2) {
      Timber.d("Resetting cached-message rows")
      // JRAW was bumped to v1.0.
      db.execSQL("DELETE FROM ${CachedMessage.TABLE_NAME}")
    }
  }

  companion object {
    private const val DB_VERSION = 2
    private const val DB_NAME = "Dank"
  }
}
