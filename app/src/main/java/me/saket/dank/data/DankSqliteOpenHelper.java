package me.saket.dank.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import me.saket.dank.ui.submission.CachedSubmission;
import me.saket.dank.ui.submission.PendingSyncReply;
import me.saket.dank.ui.user.messages.CachedMessage;

public class DankSqliteOpenHelper extends SQLiteOpenHelper {

  private static final int DB_VERSION = 1;
  private static final String DB_NAME = "Dank";

  public DankSqliteOpenHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(SubredditSubscription.QUERY_CREATE_TABLE);
    db.execSQL(CachedMessage.QUERY_CREATE_TABLE);
    db.execSQL(CachedSubmission.QUERY_CREATE_TABLE);
    db.execSQL(PendingSyncReply.QUERY_CREATE_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
