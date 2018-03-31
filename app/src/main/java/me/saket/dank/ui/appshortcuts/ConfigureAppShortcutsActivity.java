package me.saket.dank.ui.appshortcuts;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.Button;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;

@DeepLink(ConfigureAppShortcutsActivity.DEEP_LINK)
@TargetApi(Build.VERSION_CODES.N_MR1)
public class ConfigureAppShortcutsActivity extends DankActivity {

  public static final String DEEP_LINK = "dank://configureAppShortcuts";

  @BindView(R.id.appshortcuts_root) ViewGroup rootViewGroup;
  @BindView(R.id.appshortcuts_content_container) ViewGroup contentViewGroup;
  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.appshortcuts_recyclerview) RecyclerView recyclerView;
  @BindView(R.id.appshortcuts_add) Button addButton;

  @Inject AppShortcutRepository repository;
  @Inject AppShortcutsAdapter adapter;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configure_app_shortcuts);
    ButterKnife.bind(this);
    findAndSetupToolbar();

    contentViewGroup.setClipToOutline(true);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    recyclerView.setItemAnimator(SlideUpAlphaAnimator.create());
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    Observable<List<AppShortcut>> replayedShortcuts = repository.shortcuts()
        .subscribeOn(io())
        .replay()
        .refCount();

    // Adapter data-set.
    replayedShortcuts
        .toFlowable(BackpressureStrategy.LATEST)
        .compose(RxDiffUtil.calculateDiff(AppShortcutsItemDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyFlowable())
        .subscribe(adapter);

    // Keep add button enabled till 4 shortcuts.
    replayedShortcuts
        .map(shortcuts -> shortcuts.size())
        .map(shortcutCount -> shortcutCount < 4)
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(RxView.enabled(addButton));

    // Add new.
    RxView.clicks(addButton)
        .withLatestFrom(replayedShortcuts, (o, shortcuts) -> shortcuts)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(shortcuts -> {
          repository.add(AppShortcut.create(shortcuts.size(), "r/Android + " + Math.random()))
              .subscribeOn(io())
              .subscribe();
        });

    // Delete.
    adapter.streamDeleteClicks()
        .observeOn(io())
        .flatMapCompletable(shortcutToDelete -> repository.delete(shortcutToDelete))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    // Dismiss on outside click.
    rootViewGroup.setOnClickListener(o -> finish());
  }
}
