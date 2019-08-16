package me.saket.dank.ui.subreddit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.util.Size;

import com.f2prateek.rx.preferences2.Preference;

import io.reactivex.observers.TestObserver;
import net.dean.jraw.models.Submission;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import me.saket.dank.cache.CachePreFiller;
import me.saket.dank.data.CachePreFillThing;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.ui.preferences.NetworkStrategy;
import me.saket.dank.ui.submission.SubmissionImageLoader;
import me.saket.dank.urlparser.ExternalLink;
import me.saket.dank.urlparser.LinkMetadata;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.urlparser.UrlParser;
import me.saket.dank.utils.NetworkStateListener;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.RxUtils;
import me.saket.dank.utils.UrlParserTest;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Submission.class, Uri.class, RxUtils.class })
public class CachePreFillerShould {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SubmissionRepository submissionRepo;
  @Mock NetworkStateListener networkStateListener;
  @Mock MediaHostRepository mediaHostRepo;
  @Mock LinkMetadataRepository linkMetadataRepo;
  @Mock UrlParser urlParser;
  @Mock HashMap<CachePreFillThing, Preference<NetworkStrategy>> networkStrategies;
  @Mock SubmissionImageLoader imageLoader;

  private CachePreFiller cachePreFiller;
  private static final Size DISPLAY_SIZE = new Size(1280, 1920);

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(Uri.class);
    //noinspection ConstantConditions
    cachePreFiller = new CachePreFiller(
        null,
        submissionRepo,
        networkStateListener,
        mediaHostRepo,
        linkMetadataRepo,
        () -> urlParser,
        () -> imageLoader,
        () -> Schedulers.computation(),
        () -> networkStrategies);

    PowerMockito.mockStatic(RxUtils.class);
    PowerMockito.when(RxUtils.errorIfMainThread()).thenReturn(o -> {
      // Suppress internal accesses of Looper.
    });
  }

  @Test
  @SuppressWarnings("unchecked")
  public void whenPreFillingIsDisabled_shouldNotCacheAnything_shouldCompleteRxChain() {
    Preference mockPref = mock(Preference.class);
    when(mockPref.asObservable()).thenReturn(Observable.just(NetworkStrategy.NEVER));
    when(networkStrategies.get(any(CachePreFillThing.class))).thenReturn(mockPref);

    when(networkStateListener.streamNetworkInternetCapability(NetworkStrategy.NEVER, Optional.empty())).thenReturn(Observable.just(false));

    List<Submission> submissions = new ArrayList<>();
    submissions.add(PowerMockito.mock(Submission.class));
    submissions.add(PowerMockito.mock(Submission.class));

    //noinspection ConstantConditions
    cachePreFiller.preFillInParallelThreads(submissions, 160)
        .test()
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

    verify(mediaHostRepo, never()).resolveActualLinkIfNeeded(any());
    verify(linkMetadataRepo, never()).unfurl(any());
    verify(submissionRepo, never()).submissionWithComments(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void shouldAvoidPreFillingSameThingTwice() {
    List<Submission> submissions = new ArrayList<>();

    Submission submission = PowerMockito.mock(Submission.class);
    String url = "https://play.google.com/store/apps/details?id=com.pinpinteam.vikings";
    PowerMockito.when(submission.getUrl()).thenReturn(url);
    Uri uri = UrlParserTest.createMockUriFor(url);
    PowerMockito.when(Uri.parse(url)).thenReturn(uri);
    submissions.add(submission);
    submissions.add(submission);

    Preference mockPref = mock(Preference.class);
    when(mockPref.asObservable()).thenReturn(Observable.just(NetworkStrategy.WIFI_ONLY));
    when(networkStrategies.get(any(CachePreFillThing.class))).thenReturn(mockPref);

    when(networkStateListener.streamNetworkInternetCapability(NetworkStrategy.WIFI_ONLY, Optional.empty())).thenReturn(Observable.just(true));

    when(linkMetadataRepo.unfurl(any())).thenReturn(Single.just(mock(LinkMetadata.class)));

    when(urlParser.parse(any(), any())).thenReturn(ExternalLink.create(url));

    TestObserver<Void> preFillObserver = cachePreFiller.preFillInParallelThreads(submissions, 160).test();
    preFillObserver.awaitTerminalEvent();
    preFillObserver
        .assertNoErrors()
        .assertComplete();

    verify(linkMetadataRepo, times(1)).unfurl(any());
  }
}
