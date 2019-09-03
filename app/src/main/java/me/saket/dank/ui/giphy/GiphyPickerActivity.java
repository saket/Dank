package me.saket.dank.ui.giphy;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.data.ErrorResolver;
import me.saket.dank.data.ResolvedError;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.utils.Keyboards;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatedProgressBar;
import me.saket.dank.widgets.EmptyStateView;
import me.saket.dank.widgets.ErrorStateView;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * Search GIFs using GIPHY.com.
 */
public class GiphyPickerActivity extends DankPullCollapsibleActivity {

  private static final String KEY_PICKED_GIPHY = "pickedGiphy";
  private static final String KEY_EXTRA_PAYLOAD = "extraPayload";

  @BindView(R.id.giphypicker_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.giphypicker_recyclerview) RecyclerView gifRecyclerView;
  @BindView(R.id.giphypicker_search) EditText searchField;
  @BindView(R.id.giphypicker_search_progress) AnimatedProgressBar searchProgressBarView;
  @BindView(R.id.giphypicker_empty_state) EmptyStateView emptyStateView;
  @BindView(R.id.giphypicker_error_state) ErrorStateView errorStateView;

  @Inject MediaHostRepository mediaHostRepository;
  @Inject ErrorResolver errorResolver;

  public static Intent intent(Context context) {
    return new Intent(context, GiphyPickerActivity.class);
  }

  /**
   * @param extraPayload Returned in {@link #extractExtraPayload(Intent)}.
   */
  public static Intent intentWithPayload(Context context, Parcelable extraPayload) {
    Intent intent = new Intent(context, GiphyPickerActivity.class);
    intent.putExtra(KEY_EXTRA_PAYLOAD, extraPayload);
    return intent;
  }

  public static GiphyGif extractPickedGif(Intent data) {
    return data.getParcelableExtra(KEY_PICKED_GIPHY);
  }

  public static <T> T extractExtraPayload(Intent data) {
    //noinspection unchecked
    return data.getParcelableExtra(KEY_EXTRA_PAYLOAD);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    Dank.dependencyInjector().inject(this);
    setPullToCollapseEnabled(true);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_giphy_picker);
    ButterKnife.bind(this);
    findAndSetupToolbar();
    setTitle(null);

    setupContentExpandablePage(activityContentPage);
    expandFromBelowToolbar();

    activityContentPage.setPullToCollapseIntercepter(Views.verticalScrollPullToCollapseIntercepter(gifRecyclerView));
  }

  @OnClick(R.id.giphypicker_giphy_attribution)
  void onClickResetGifs() {
    if (BuildConfig.DEBUG) {
      mediaHostRepository.clearCachedGifs();
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    boolean isInPortraitOrientation = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    int columnCount = isInPortraitOrientation ? 2 : 3;

    GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columnCount);
    GiphyAdapter giphyAdapter = new GiphyAdapter(getResources().getIntArray(R.array.giphy_placeholder_colors));
    gifRecyclerView.setLayoutManager(gridLayoutManager);
    gifRecyclerView.setAdapter(giphyAdapter);

    Observable<Object> retries = errorStateView.retryClicks().share();

    RxTextView.textChanges(searchField)
        .map(sequence -> sequence.toString().toLowerCase(Locale.ENGLISH))
        .debounce(200, TimeUnit.MILLISECONDS, mainThread())
        .flatMap(searchQuery -> retries.map(o -> searchQuery).startWith(searchQuery))
        .switchMapSingle(searchQuery -> mediaHostRepository.searchGifs(searchQuery)
            .retry(3)
            .subscribeOn(Schedulers.io())
            .observeOn(mainThread())
            .doOnSubscribe(o -> {
              searchProgressBarView.show();
              errorStateView.setVisibility(View.GONE);
            })
            .doFinally(() -> searchProgressBarView.hide())
            .onErrorResumeNext(error -> {
              gifRecyclerView.setVisibility(View.INVISIBLE);
              errorStateView.setVisibility(View.VISIBLE);

              ResolvedError resolvedError = errorResolver.resolve(error);
              if (resolvedError.isUnknown()) {
                Timber.e(error, "Error while searching GIFs");
              }
              errorStateView.applyFrom(resolvedError);
              return Single.never();
            })
        )
        .takeUntil(lifecycle().onDestroy())
        .doOnNext(o -> gifRecyclerView.scrollToPosition(0))
        .doOnNext(gifs -> emptyStateView.setVisibility(gifs.isEmpty() ? View.VISIBLE : View.GONE))
        .doOnNext(o -> gifRecyclerView.setVisibility(View.VISIBLE))
        .subscribe(giphyAdapter);

    giphyAdapter.streamClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(clickedGiphyGif -> {
          Intent resultData = new Intent();
          resultData.putExtra(KEY_PICKED_GIPHY, clickedGiphyGif);
          resultData.putExtra(KEY_EXTRA_PAYLOAD, (Parcelable) getIntent().getParcelableExtra(KEY_EXTRA_PAYLOAD));
          setResult(RESULT_OK, resultData);
          finish();
        });

    // Hide keyboard on scroll.
    RxRecyclerView.scrollEvents(gifRecyclerView)
        .filter(scrollEvent -> Math.abs(scrollEvent.dy()) > 0)
        .takeUntil(lifecycle().onDestroy())
        .subscribe(scrollEvent -> Keyboards.hide(this, searchField));
  }
}
