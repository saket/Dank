package me.saket.dank.ui.subreddits;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.dean.jraw.models.Submission;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import io.reactivex.Observable;
import me.saket.dank.data.LinkMetadataRepository;
import me.saket.dank.data.NetworkStrategy;
import me.saket.dank.data.UserPreferences;
import me.saket.dank.ui.media.MediaHostRepository;
import me.saket.dank.ui.submission.SubmissionRepository;
import me.saket.dank.utils.NetworkStateListener;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Submission.class })
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
}
