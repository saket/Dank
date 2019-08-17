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
import androidx.annotation.CheckResult;

import com.squareup.sqlbrite2.BriteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.deeplinks.DeepLinkHandlingActivity;
import timber.log.Timber;

@TargetApi(Build.VERSION_CODES.N_MR1)
public class AppShortcutRepository {

  public static final int MAX_SHORTCUT_COUNT = 4;

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
        .firstOrError()
        .map(shortcuts -> {
          List<ShortcutInfo> shortcutInfos;

          if (shortcuts.isEmpty()) {
            ShortcutInfo configureShortcut = new ShortcutInfo.Builder(appContext, "add_shortcuts")
                .setShortLabel(appContext.getString(R.string.add_launcher_app_shortcuts_label))
                .setIcon(Icon.createWithResource(appContext, R.drawable.ic_configure_app_shortcuts))
                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(ConfigureAppShortcutsActivity.DEEP_LINK)))
                .build();
            shortcutInfos = Collections.singletonList(configureShortcut);

          } else {
            shortcutInfos = new ArrayList<>(MAX_SHORTCUT_COUNT);

            for (int i = 0; i < shortcuts.size(); i++) {
              // Android displays shortcuts in descending rank, but our UI for configuring
              // them uses ascending. So I'm manually reversing it again here.
              int androidRank = shortcuts.size() - i;
              AppShortcut shortcut = shortcuts.get(i);

              shortcutInfos.add(new ShortcutInfo.Builder(appContext, shortcut.id())
                  .setShortLabel(appContext.getString(R.string.subreddit_name_r_prefix, shortcut.label()))    // Used by pinned shortcuts.
                  .setLongLabel(shortcut.label())           // Shown in shortcuts popup.
                  .setRank(androidRank)
                  .setIcon(Icon.createWithResource(appContext, R.drawable.ic_shortcut_subreddit))
                  .setIntent(DeepLinkHandlingActivity.appShortcutIntent(appContext, shortcut))
                  .build());
            }
          }
          return shortcutInfos;
        })
        .flatMapCompletable(shortcutInfos -> Completable.fromAction(() ->
            shortcutManager.get().setDynamicShortcuts(shortcutInfos))
        );
  }
}
