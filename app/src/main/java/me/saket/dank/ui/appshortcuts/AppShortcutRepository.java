package me.saket.dank.ui.appshortcuts;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.CheckResult;

import com.squareup.sqlbrite2.BriteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.R;
import timber.log.Timber;

@TargetApi(Build.VERSION_CODES.N_MR1)
public class AppShortcutRepository {

  private final Application appContext;
  private final Lazy<BriteDatabase> database;
  private final Lazy<ShortcutManager> shortcutManager;

  @Inject
  public AppShortcutRepository(Application appContext, Lazy<BriteDatabase> database, Lazy<ShortcutManager> shortcutManager) {
    this.appContext = appContext;
    this.database = database;
    this.shortcutManager = shortcutManager;
  }

  @CheckResult
  public Observable<List<AppShortcut>> shortcuts() {
    return database.get()
        .createQuery(AppShortcut.TABLE_NAME, AppShortcut.QUERY_GET_ALL_ORDERED_BY_RANK)
        .mapToList(AppShortcut.MAPPER);
  }

  @CheckResult
  public Completable add(AppShortcut shortcut) {
    return Completable
        .fromAction(() -> database.get().insert(AppShortcut.TABLE_NAME, shortcut.toValues(), SQLiteDatabase.CONFLICT_REPLACE))
        .andThen(updateInstalledShortcuts()
            .doOnError(error -> Timber.e(error, "Couldn't update app shortcuts"))
            .onErrorResumeNext(error -> delete(shortcut)
                .andThen(Completable.error(error))));
  }

  @CheckResult
  public Completable delete(AppShortcut shortcut) {
    return Completable
        .fromAction(() -> database.get().delete(AppShortcut.TABLE_NAME, AppShortcut.WHERE_LABEL, shortcut.label()))
        .andThen(updateInstalledShortcuts());
  }

  @CheckResult
  public Completable updateInstalledShortcuts() {
    return shortcuts()
        .take(1)
        .flatMapCompletable(shortcuts -> Completable.fromAction(() -> {
          List<ShortcutInfo> shortcutInfos;

          if (shortcuts.isEmpty()) {
            ShortcutInfo configureShortcut = new ShortcutInfo.Builder(appContext, "add_shortcuts")
                .setShortLabel(appContext.getString(R.string.add_launcher_app_shortcuts_label))
                .setIcon(Icon.createWithResource(appContext, R.drawable.ic_configure_app_shortcuts))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigureAppShortcutsActivity.DEEP_LINK)))
                .build();
            shortcutInfos = Collections.singletonList(configureShortcut);

          } else {
            // TODO: Open SubredditActivity.
            shortcutInfos = shortcuts
                .stream()
                .map(shortcut -> new ShortcutInfo.Builder(appContext, shortcut.label())
                    .setShortLabel(shortcut.label())
                    .setRank(shortcut.rank())
                    .setIcon(Icon.createWithResource(appContext, R.drawable.ic_app_shortcut_default))
                    .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigureAppShortcutsActivity.DEEP_LINK)))
                    .build())
                .collect(Collectors.toList());
          }

          shortcutManager.get().setDynamicShortcuts(shortcutInfos);
        }));
  }
}
