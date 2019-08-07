package me.saket.dank.ui.appshortcuts;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static io.reactivex.schedulers.Schedulers.io;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.AnimRes;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.jakewharton.rxrelay2.BehaviorRelay;
import com.jakewharton.rxrelay2.PublishRelay;
import com.jakewharton.rxrelay2.Relay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import butterknife.BindInt;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dagger.Lazy;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Observable;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankActivity;
import me.saket.dank.ui.appshortcuts.AppShortcutsAdapter.AppShortcutViewHolder;
import me.saket.dank.ui.subscriptions.SubredditAdapter;
import me.saket.dank.ui.subscriptions.SubredditFlexboxLayoutManager;
import me.saket.dank.ui.subscriptions.SubredditSubscription;
import me.saket.dank.ui.subscriptions.SubscriptionRepository;
import me.saket.dank.utils.Animations;
import me.saket.dank.utils.ItemTouchHelperDragAndDropCallback;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Pair;
import me.saket.dank.utils.RxDiffUtil;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.itemanimators.SlideUpAlphaAnimator;
import me.saket.dank.widgets.swipe.RecyclerSwipeListener;
import timber.log.Timber;

@DeepLink(ConfigureAppShortcutsActivity.DEEP_LINK)
@RequiresApi(Build.VERSION_CODES.N_MR1)
public class ConfigureAppShortcutsActivity extends DankActivity {

  private static final int MAX_SHORTCUT_COUNT = AppShortcutRepository.MAX_SHORTCUT_COUNT;
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
  @Inject Lazy<ErrorResolver> errorResolver;

  @BindInt(R.integer.submissionoptions_animation_duration) int pageChangeAnimDuration;

  private final BehaviorRelay<Screen> screenChanges = BehaviorRelay.create();
  private final Relay<SubredditSubscription> subredditSelections = PublishRelay.create();

  private enum Screen {
    SHORTCUTS(R.id.appshortcuts_flipper_shortcuts_screen),
    ADD_NEW_SUBREDDIT(R.id.appshortcuts_flipper_add_new_screen);

    private final int viewId;

    Screen(@IdRes int viewId) {
      this.viewId = viewId;
    }
  }

  public static Intent intent(Context context) {
    return new Intent(context, ConfigureAppShortcutsActivity.class);
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

    screenChanges.accept(Optional.ofNullable(savedState)
        .map(state -> (Screen) state.getSerializable(KEY_VISIBLE_SCREEN))
        .orElse(Screen.SHORTCUTS));

    setupScreenChanges();
    setupShortcutList();
    setupSearchScreen();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(KEY_VISIBLE_SCREEN, screenChanges.getValue());
  }

  @Override
  @OnClick(R.id.appshortcuts_shortcuts_done)
  public void finish() {
    super.finish();
  }

  private void setupScreenChanges() {
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
        // Delay because showing of keyboard interferes with page change animation.
        .delay(needsKeyboard -> Observable.timer(needsKeyboard ? pageChangeAnimDuration / 2 : 0, TimeUnit.MILLISECONDS, mainThread()))
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
    SlideUpAlphaAnimator animator = SlideUpAlphaAnimator.create();
    animator.setSupportsChangeAnimations(false);
    shortcutsRecyclerView.setItemAnimator(animator);
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
              .add(AppShortcut.create(shortcuts.size(), subreddit.name()))
              .subscribeOn(io())
              .observeOn(mainThread())
              .subscribe(
                  RxUtils.doNothingCompletable(),
                  error -> Toast.makeText(this, "That failed (╯°□°）╯︵ ┻━┻", Toast.LENGTH_SHORT).show());
        });

    // Drags.
    ItemTouchHelper dragHelper = new ItemTouchHelper(createDragAndDropCallbacks());
    dragHelper.attachToRecyclerView(shortcutsRecyclerView);
    shortcutsAdapter.get().streamDragStarts()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(viewHolder -> dragHelper.startDrag(viewHolder));

    // Deletes.
    // WARNING: THIS TOUCH LISTENER FOR SWIPE SHOULD BE REGISTERED AFTER DRAG-DROP LISTENER.
    // Drag-n-drop's long-press listener does not get canceled if a row is being swiped.
    shortcutsRecyclerView.addOnItemTouchListener(new RecyclerSwipeListener(shortcutsRecyclerView));
    shortcutsAdapter.get().streamDeleteClicks()
        .observeOn(io())
        .flatMapCompletable(shortcutToDelete -> shortcutsRepository.get().delete(shortcutToDelete))
        .ambWith(lifecycle().onDestroyCompletable())
        .subscribe();

    // Dismiss on outside click.
    rootViewGroup.setOnClickListener(o -> finish());
  }

  private ItemTouchHelperDragAndDropCallback createDragAndDropCallbacks() {
    return new ItemTouchHelperDragAndDropCallback() {
      @Override
      protected boolean onItemMove(ViewHolder source, ViewHolder target) {
        AppShortcutViewHolder sourceViewHolder = (AppShortcutViewHolder) source;
        AppShortcutViewHolder targetViewHolder = (AppShortcutViewHolder) target;

        int fromPosition = sourceViewHolder.getAdapterPosition();
        int toPosition = targetViewHolder.getAdapterPosition();

        //noinspection ConstantConditions
        List<AppShortcut> appShortcuts = Observable.fromIterable(shortcutsAdapter.get().getData())
            .ofType(AppShortcut.class)
            .toList()
            .blockingGet();

        if (fromPosition < toPosition) {
          for (int i = fromPosition; i < toPosition; i++) {
            Collections.swap(appShortcuts, i, i + 1);
          }
        } else {
          for (int i = fromPosition; i > toPosition; i--) {
            Collections.swap(appShortcuts, i, i - 1);
          }
        }

        for (int i = 0; i < appShortcuts.size(); i++) {
          AppShortcut shortcut = appShortcuts.get(i);
          shortcutsRepository.get().add(shortcut.withRank(i))
              .subscribeOn(io())
              .subscribe();
        }
        return true;
      }
    };
  }

  private void setupSearchScreen() {
    addNewSubredditUpButton.setOnClickListener(o -> screenChanges.accept(Screen.SHORTCUTS));

    subredditRecyclerView.setAdapter(subredditAdapter.get());
    subredditRecyclerView.setLayoutManager(new SubredditFlexboxLayoutManager(this));
    subredditRecyclerView.setItemAnimator(null);

    // Hide keyboard on scroll.
    RxRecyclerView.scrollEvents(subredditRecyclerView)
        .filter(scrollEvent -> Math.abs(scrollEvent.dy()) > 0)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(scrollEvent -> Keyboards.hide(searchEditText));

    // Subreddit clicks.
    subredditAdapter.get().setOnSubredditClickListener((subscription, subredditItemView) -> {
      subredditSelections.accept(subscription);

      // Reset the search field. Wait till the screen has changed. The subreddit
      // list getting updated while it's going away doesn't look nice.
      Observable.timer(300, TimeUnit.MILLISECONDS, mainThread())
          .takeUntil(lifecycle().onDestroy())
          .subscribe(o -> searchEditText.setText(null));
    });

    // Search.
    RxTextView.textChanges(searchEditText)
        // CharSequence is mutable.
        .map(searchTerm -> searchTerm.toString().trim())
        .switchMap(searchTerm -> subscriptionRepository.get().getAll(searchTerm, true)
            .doOnError(error -> {
              ResolvedError resolvedError = errorResolver.get().resolve(error);
              resolvedError.ifUnknown(() -> Timber.e(error, "Error in fetching subreddits"));
            })
            .onErrorReturnItem(Collections.emptyList())
            .subscribeOn(io())
            .observeOn(mainThread())
            .doOnNext(o -> subredditsLoadProgressView.setVisibility(View.GONE))
            .map(filteredSubs -> Pair.create(filteredSubs, searchTerm)))
        .startWith(Pair.create(Collections.emptyList(), ""))
        .map(pair -> addSearchTermIfMatchNotFound(pair))
        .observeOn(mainThread())
        .takeUntil(lifecycle().onDestroy())
        .subscribe(subreddits -> subredditAdapter.get().updateDataAndNotifyDatasetChanged(subreddits));

    subredditsLoadProgressView.setVisibility(View.VISIBLE);
  }

  /**
   * Show user's search term in the results unless an exact match was found.
   */
  private List<SubredditSubscription> addSearchTermIfMatchNotFound(Pair<List<SubredditSubscription>, String> pair) {
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
        ArrayList<SubredditSubscription> filteredSubsWithQuery = new ArrayList<>(filteredSubs.size() + 1);
        filteredSubsWithQuery.addAll(filteredSubs);
        filteredSubsWithQuery.add(SubredditSubscription.create(searchTerm, SubredditSubscription.PendingState.NONE, false));
        return Collections.unmodifiableList(filteredSubsWithQuery);
      }
    }
    return filteredSubs;
  }

  private Animation animationWithInterpolator(@AnimRes int animRes) {
    Animation animation = AnimationUtils.loadAnimation(this, animRes);
    animation.setInterpolator(Animations.INTERPOLATOR);
    return animation;
  }
}
