package me.saket.dank.ui.subreddits;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import android.support.annotation.NonNull;

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
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.data.links.LinkMetadata;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.NetworkStateListener;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Submission.class, Uri.class })
public class CachePreFillerShould {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SubmissionRepository submissionRepo;
  @Mock NetworkStateListener networkStateListener;
  @Mock MediaHostRepository mediaHostRepo;
  @Mock LinkMetadataRepository linkMetadataRepo;
  @Mock UserPreferences userPreferences;

  private CachePreFiller cachePreFiller;

  @Before
  public void setUp() throws Exception {
    PowerMockito.mockStatic(Uri.class);
    cachePreFiller = new CachePreFiller(null, submissionRepo, networkStateListener, mediaHostRepo, linkMetadataRepo, userPreferences);
  }

  @Test
  public void whenPreFillingIsDisabled_shouldNotCacheAnything_shouldCompleteRxChain() {
    when(userPreferences.streamCachePreFillNetworkStrategy(any())).thenReturn(Observable.just(NetworkStrategy.NEVER));
    when(networkStateListener.streamNetworkInternetCapability(NetworkStrategy.NEVER)).thenReturn(Observable.just(false));

    cachePreFiller.preFillInParallelThreads(Collections.emptyList(), 720, 160)
        .test()
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();

    verify(mediaHostRepo, never()).resolveActualLinkIfNeeded(any());
    verify(linkMetadataRepo, never()).unfurl(any());
    verify(submissionRepo, never()).submissionWithComments(any());
  }

  @Test
  public void shouldAvoidPreFillingSameThingTwice() {
    List<Submission> submissions = new ArrayList<>();

    Submission submission = PowerMockito.mock(Submission.class);
    String url = "https://play.google.com/store/apps/details?id=com.pinpinteam.vikings";
    PowerMockito.when(submission.getUrl()).thenReturn(url);
    Uri uri = createMockUriFor(url);
    PowerMockito.when(Uri.parse(url)).thenReturn(uri);
    submissions.add(submission);
    submissions.add(submission);

    when(userPreferences.streamCachePreFillNetworkStrategy(any())).thenReturn(Observable.just(NetworkStrategy.WIFI_ONLY));
    when(networkStateListener.streamNetworkInternetCapability(NetworkStrategy.WIFI_ONLY)).thenReturn(Observable.just(true));
    when(linkMetadataRepo.unfurl(any())).thenReturn(Single.just(mock(LinkMetadata.class)));

    cachePreFiller.preFillInParallelThreads(submissions, 720, 160)
        .subscribe();

    verify(linkMetadataRepo, times(1)).unfurl(any());
  }

  // TODO: Extract this into an @Rule.
  @NonNull
  private Uri createMockUriFor(String url) {
    Uri mockUri = mock(Uri.class);

    // Really hacky way, but couldn't think of a better way.
    String domainTld;
    if (url.contains(".com/")) {
      domainTld = ".com";
    } else if (url.contains(".it/")) {
      domainTld = ".it";
    } else {
      throw new UnsupportedOperationException("Unknown TLD");
    }

    when(mockUri.getPath()).thenReturn(url.substring(url.indexOf(domainTld) + domainTld.length()));
    when(mockUri.getHost()).thenReturn(url.substring(url.indexOf("://") + "://".length(), url.indexOf(domainTld) + domainTld.length()));
    when(mockUri.getScheme()).thenReturn(url.substring(0, url.indexOf("://")));
    when(mockUri.toString()).thenReturn(url);
    return mockUri;
  }
}
