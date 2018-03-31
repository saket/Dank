package me.saket.dank.ui.appshortcuts;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ViewFlipper;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.jakewharton.rxrelay2.BehaviorRelay;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;

@DeepLink(ConfigureAppShortcutsActivity.DEEP_LINK)
@TargetApi(Build.VERSION_CODES.N_MR1)
public class ConfigureAppShortcutsActivity extends DankActivity {

  private static final int MAX_SHORTCUT_COUNT = 4;
  public static final String DEEP_LINK = "dank://configureAppShortcuts";

  @BindView(R.id.appshortcuts_root) ViewGroup rootViewGroup;
  @BindView(R.id.appshortcuts_content_flipper) ViewFlipper contentViewFlipper;

  @BindView(R.id.appshortcuts_shortcuts_recyclerview) RecyclerView shortcutsRecyclerView;

  @BindView(R.id.appshrotcuts_addnew_up) ImageButton addNewSubredditUpButton;
  @BindView(R.id.appshortcuts_addnew_search_field) EditText searchEditText;

  @Inject AppShortcutRepository shortcutsRepository;
  @Inject AppShortcutsAdapter shortcutsAdapter;

  private BehaviorRelay<Screen> screenChanges = BehaviorRelay.createDefault(Screen.SHORTCUTS);

  private enum Screen {
    SHORTCUTS(R.id.appshortcuts_flipper_shortcuts_screen),
    ADD_NEW_SUBREDDIT(R.id.appshortcuts_flipper_add_new_screen);

    private final int viewId;

    Screen(@IdRes int viewId) {
      this.viewId = viewId;
    }
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_configure_app_shortcuts);
    ButterKnife.bind(this);

    contentViewFlipper.setClipToOutline(true);
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    setupShortcutList();
    setupSearchScreen();

    // Screen changes.
    //noinspection RedundantTypeArguments
    screenChanges
        .map(screen -> contentViewFlipper.<View>findViewById(screen.viewId))
        .map(screenView -> contentViewFlipper.indexOfChild(screenView))
        .takeUntil(lifecycle().onDestroy())
        .subscribe(screenIndex -> contentViewFlipper.setDisplayedChild(screenIndex));

    // Screen change animation.
    screenChanges
        .takeUntil(lifecycle().onDestroy())
        .subscribe(screen -> {
          if (screen == Screen.SHORTCUTS) {
            contentViewFlipper.setInAnimation(animationWithInterpolator(R.anim.submission_options_viewflipper_submenu_enter));
            contentViewFlipper.setOutAnimation(animationWithInterpolator(R.anim.submission_options_viewflipper_mainmenu_exit));
          } else {
            contentViewFlipper.setInAnimation(animationWithInterpolator(R.anim.submission_options_viewflipper_mainmenu_enter));
            contentViewFlipper.setOutAnimation(animationWithInterpolator(R.anim.submission_options_viewflipper_submenu_exit));
          }
        });

    // Keyboard.
    screenChanges
        .map(screen -> screen == Screen.ADD_NEW_SUBREDDIT)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(needsKeyboard -> {
          if (needsKeyboard) {
            Keyboards.show(searchEditText);
          } else {
            Keyboards.hide(searchEditText);
          }
        });
  }

  private void setupShortcutList() {
    shortcutsRecyclerView.setItemAnimator(SlideUpAlphaAnimator.create());
    shortcutsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    shortcutsRecyclerView.setAdapter(shortcutsAdapter);

    Observable<List<AppShortcut>> replayedShortcuts = shortcutsRepository.shortcuts()
        .subscribeOn(io())
        .replay()
        .refCount();

    // Adapter data-set.
    replayedShortcuts
        .toFlowable(BackpressureStrategy.LATEST)
        .map(shortcuts -> {
          List<AppShortcutScreenUiModel> uiModels = new ArrayList<>(MAX_SHORTCUT_COUNT);
          uiModels.addAll(shortcuts);

          for (int i = uiModels.size(); i < MAX_SHORTCUT_COUNT; i++) {
            uiModels.add(AppShortcutPlaceholderUiModel.create());
          }
          return uiModels;
        })
        .compose(RxDiffUtil.calculateDiff(AppShortcutsUiModelDiffer::create))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroyFlowable())
        .subscribe(shortcutsAdapter);

    // Add new.
    shortcutsAdapter.streamAddClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> screenChanges.accept(Screen.ADD_NEW_SUBREDDIT));

    // Delete.
    shortcutsAdapter.streamDeleteClicks()
        .observeOn(io())
        .flatMapCompletable(shortcutToDelete -> shortcutsRepository.delete(shortcutToDelete))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    // Dismiss on outside click.
    rootViewGroup.setOnClickListener(o -> finish());
  }

  private void setupSearchScreen() {
    addNewSubredditUpButton.setOnClickListener(o -> screenChanges.accept(Screen.SHORTCUTS));
  }

  private Animation animationWithInterpolator(@AnimRes int animRes) {
    Animation animation = AnimationUtils.loadAnimation(this, animRes);
    animation.setInterpolator(Animations.INTERPOLATOR);
    return animation;
  }
}
