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
import com.google.common.collect.ImmutableList;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.subscriptions.SubredditAdapter;
import me.saket.dank.ui.subscriptions.SubredditFlexboxLayoutManager;
import me.saket.dank.ui.subscriptions.SubredditSubscription;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;
import timber.log.Timber;

@DeepLink(ConfigureAppShortcutsActivity.DEEP_LINK)
@TargetApi(Build.VERSION_CODES.N_MR1)
public class ConfigureAppShortcutsActivity extends DankActivity {

  private static final int MAX_SHORTCUT_COUNT = 4;
  private static final String KEY_VISIBLE_SCREEN = "visibleScreen";
  public static final String DEEP_LINK = "dank://configureAppShortcuts";

  @BindView(R.id.appshortcuts_root) ViewGroup rootViewGroup;
  @BindView(R.id.appshortcuts_content_flipper) ViewFlipper contentViewFlipper;
  @BindView(R.id.appshortcuts_shortcuts_recyclerview) RecyclerView shortcutsRecyclerView;
  @BindView(R.id.appshrotcuts_addnew_up) ImageButton addNewSubredditUpButton;
  @BindView(R.id.appshortcuts_addnew_search_field) EditText searchEditText;
  @BindView(R.id.appshortcuts_subreddits_recyclerview) RecyclerView subredditRecyclerView;
  @BindView(R.id.appshortcuts_subreddits_load_progress) View subredditsLoadProgressView;

  @Inject Lazy<SubscriptionRepository> subscriptionRepository;
  @Inject Lazy<AppShortcutRepository> shortcutsRepository;
  @Inject Lazy<AppShortcutsAdapter> shortcutsAdapter;
  @Inject Lazy<SubredditAdapter> subredditAdapter;

  private final BehaviorRelay<Screen> screenChanges = BehaviorRelay.createDefault(Screen.SHORTCUTS);
  private final Relay<SubredditSubscription> subredditSelections = PublishRelay.create();

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
  protected void onPostCreate(@Nullable Bundle savedState) {
    super.onPostCreate(savedState);

    setupShortcutList();
    setupSearchScreen();

    if (savedState != null) {
      //noinspection ConstantConditions
      screenChanges.accept(((Screen) savedState.getSerializable(KEY_VISIBLE_SCREEN)));
    }

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

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_VISIBLE_SCREEN, screenChanges.getValue());
  }

  private void setupShortcutList() {
    shortcutsRecyclerView.setItemAnimator(SlideUpAlphaAnimator.create());
    shortcutsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    shortcutsRecyclerView.setAdapter(shortcutsAdapter.get());

    Observable<List<AppShortcut>> replayedShortcuts = shortcutsRepository.get().shortcuts()
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
        .subscribe(shortcutsAdapter.get());

    // Add new.
    shortcutsAdapter.get().streamAddClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(o -> screenChanges.accept(Screen.ADD_NEW_SUBREDDIT));

    // New subreddit selections.
    subredditSelections
        .doOnNext(o -> screenChanges.accept(Screen.SHORTCUTS))
        .withLatestFrom(replayedShortcuts, Pair::create)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(pair -> {
          SubredditSubscription subreddit = pair.first();
          List<AppShortcut> shortcuts = pair.second();

          shortcutsRepository.get()
              .add(AppShortcut.create(shortcuts.size(), getString(R.string.subreddit_name_r_prefix, subreddit.name())))
              .subscribeOn(io())
              .subscribe();
        });

    // Delete.
    shortcutsAdapter.get().streamDeleteClicks()
        .observeOn(io())
        .flatMapCompletable(shortcutToDelete -> shortcutsRepository.get().delete(shortcutToDelete))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    // Dismiss on outside click.
    rootViewGroup.setOnClickListener(o -> finish());
  }

  private void setupSearchScreen() {
    addNewSubredditUpButton.setOnClickListener(o -> screenChanges.accept(Screen.SHORTCUTS));

    subredditRecyclerView.setAdapter(subredditAdapter.get());
    subredditRecyclerView.setLayoutManager(new SubredditFlexboxLayoutManager(this));
    subredditRecyclerView.setItemAnimator(null);

    // Subreddit clicks.
    subredditAdapter.get().setOnSubredditClickListener((subscription, subredditItemView) -> {
      subredditSelections.accept(subscription);
    });

    // TODO: reset search field on subreddit selection.
    // TODO: separate subreddit stream and custom subreddit streams.

    // Search.
    RxTextView.textChanges(searchEditText)
        // CharSequence is mutable.
        .map(searchTerm -> searchTerm.toString().trim())
        .switchMap(searchTerm -> subscriptionRepository.get().getAll(searchTerm, true)
            .subscribeOn(io())
            .map(filteredSubs -> Pair.create(filteredSubs, searchTerm)))
        .map(pair -> {
          // Show user's search term in the results unless an exact match was found.
          List<SubredditSubscription> filteredSubs = pair.first();
          String searchTerm = pair.second();

          if (!searchTerm.isEmpty()) {
            boolean exactSearchFound = false;
            for (SubredditSubscription filteredSub : filteredSubs) {
              if (filteredSub.name().equalsIgnoreCase(searchTerm)) {
                exactSearchFound = true;
                break;
              }
            }

            if (!exactSearchFound) {
              return ImmutableList.<SubredditSubscription>builder()
                  .addAll(filteredSubs)
                  .add(SubredditSubscription.create(searchTerm, SubredditSubscription.PendingState.NONE, false))
                  .build();
            }
          }
          return filteredSubs;
        })
        .onErrorResumeNext(error -> {
          // Don't let an error terminate the stream.
          Timber.e(error, "Error in fetching subreddits");
          return Observable.just(Collections.emptyList());
        })
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(subreddits -> subredditAdapter.get().updateDataAndNotifyDatasetChanged(subreddits));

    subredditAdapter.get().dataChanges()
        .take(1)
        .map(o -> View.GONE)
        .startWith(View.VISIBLE)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(progressVisibility -> subredditsLoadProgressView.setVisibility(progressVisibility));
  }

  private Animation animationWithInterpolator(@AnimRes int animRes) {
    Animation animation = AnimationUtils.loadAnimation(this, animRes);
    animation.setInterpolator(Animations.INTERPOLATOR);
    return animation;
  }
}
