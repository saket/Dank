package me.saket.dank.ui.giphy;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static me.saket.dank.utils.RxUtils.logError;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.EditText;

import com.jakewharton.rxbinding2.widget.RxTextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.BuildConfig;
import me.saket.dank.R;
import me.saket.dank.di.Dank;
import me.saket.dank.ui.DankPullCollapsibleActivity;
import me.saket.dank.utils.Views;
import me.saket.dank.widgets.AnimatedProgressBar;
import me.saket.dank.widgets.InboxUI.IndependentExpandablePageLayout;
import timber.log.Timber;

/**
 *
 */
public class GiphyPickerActivity extends DankPullCollapsibleActivity {

  private static final String KEY_PICKED_GIPHY = "pickedGiphy";

  @BindView(R.id.giphypicker_root) IndependentExpandablePageLayout activityContentPage;
  @BindView(R.id.giphypicker_recyclerview) RecyclerView gifRecyclerView;
  @BindView(R.id.giphypicker_search) EditText searchField;
  @BindView(R.id.giphypicker_search_progress) AnimatedProgressBar searchProgressBarView;

  @Inject GiphyRepository giphyRepository;

  public static Intent intent(Context context) {
    return new Intent(context, GiphyPickerActivity.class);
  }

  public static GiphyGif handleActivityResult(Intent data) {
    return data.getParcelableExtra(KEY_PICKED_GIPHY);
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

    activityContentPage.setPullToCollapseIntercepter((event, downX, downY, upwardPagePull) -> {
      //noinspection CodeBlock2Expr
      return Views.touchLiesOn(gifRecyclerView, downX, downY) && gifRecyclerView.canScrollVertically(upwardPagePull ? 1 : -1);
    });
  }

  @OnClick(R.id.giphypicker_giphy_attribution)
  void onClickResetGifs() {
    if (BuildConfig.DEBUG) {
      Timber.i("Clearing all gifs");
      giphyRepository.clear();
    }
  }

  @Override
  protected void onPostCreate(@Nullable Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);

    GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
    gifRecyclerView.setLayoutManager(gridLayoutManager);

    GiphyAdapter giphyAdapter = new GiphyAdapter(getResources().getIntArray(R.array.giphy_placeholder_colors));
    gifRecyclerView.setAdapter(giphyAdapter);

    RxTextView.textChanges(searchField)
        .map(sequence -> sequence.toString().toLowerCase(Locale.ENGLISH))
        .debounce(200, TimeUnit.MILLISECONDS, mainThread())
        .switchMapSingle(searchQuery -> giphyRepository.search(searchQuery)
            .retry(3)
            .subscribeOn(Schedulers.io())
            .observeOn(mainThread())
            .doOnSubscribe(o -> searchProgressBarView.show())
            .doFinally(() -> searchProgressBarView.hide())
        )
        .takeUntil(lifecycle().onDestroy())
        .doOnNext(o -> gifRecyclerView.scrollToPosition(0))
        .subscribe(giphyAdapter, logError("Failed to search more gifs"));

    giphyAdapter.streamClicks()
        .takeUntil(lifecycle().onDestroy())
        .subscribe(clickedGiphyGif -> {
          Intent resultData = new Intent();
          resultData.putExtra(KEY_PICKED_GIPHY, clickedGiphyGif);
          setResult(RESULT_OK, resultData);
          finish();
        });
  }
}
