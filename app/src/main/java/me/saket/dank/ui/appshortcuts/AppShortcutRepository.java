package me.saket.dank.ui.appshortcuts;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.CheckResult;

import com.squareup.sqlbrite2.BriteDatabase;

import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;

public class AppShortcutRepository {

  private final Lazy<BriteDatabase> database;

  @Inject
  public AppShortcutRepository(Lazy<BriteDatabase> database) {
    this.database = database;
  }

  @CheckResult
  public Observable<List<AppShortcut>> shortcuts() {
    return database.get()
        .createQuery(AppShortcut.TABLE_NAME, AppShortcut.QUERY_GET_ALL_ORDERED_BY_RANK)
        .mapToList(AppShortcut.MAPPER);
  }

  @CheckResult
  public Completable add(AppShortcut shortcut) {
    return Completable.fromAction(() -> database.get().insert(AppShortcut.TABLE_NAME, shortcut.toValues(), SQLiteDatabase.CONFLICT_REPLACE));
  }

  @CheckResult
  public Completable delete(AppShortcut shortcut) {
    return Completable.fromAction(() -> database.get().delete(AppShortcut.TABLE_NAME, AppShortcut.WHERE_LABEL, shortcut.label()));
  }
}
