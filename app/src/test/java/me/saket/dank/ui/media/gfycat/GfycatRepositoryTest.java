package me.saket.dank.ui.media.gfycat;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Single;
import me.saket.dank.di.DankApi;
import me.saket.dank.urlparser.GfycatLink;
import me.saket.dank.urlparser.UrlParserConfig;
import retrofit2.HttpException;

public class GfycatRepositoryTest {

  private static final String UNRESOLVED_ID = "threewordid";
  private static final String RESOLVED_ID = "ThreeWordId";
  private static final String AUTH_TOKEN = "authToken";

  private static final GfycatResponse.Data GFYCAT_RESPONSE = GfycatResponse.Data.create(RESOLVED_ID, "high", "low");
  private static final GfycatLink GFYCAT_RESOLVED_LINK = GfycatLink.create(
      "https://gfycat.com/" + RESOLVED_ID,
      RESOLVED_ID,
      "high",
      "low");

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock GfycatRepositoryData data;
  @Mock DankApi dankApi;

  private GfycatRepository gfycatRepository;
  private Boolean tokenRequired;

  @Before
  public void setUp() throws Exception {
    UrlParserConfig urlParserConfig = new UrlParserConfig();
    gfycatRepository = new GfycatRepository(() -> dankApi, () -> urlParserConfig, () -> data);
  }

  @Test
  public void when_auth_token_is_not_required() throws Exception {
    when(data.isAccessTokenRequired()).thenReturn(false);

    GfycatResponse response = GfycatResponse.create(GFYCAT_RESPONSE);
    when(dankApi.gfycat(UNRESOLVED_ID)).thenReturn(Single.just(response));

    gfycatRepository.gif(UNRESOLVED_ID)
        .test()
        .assertNoErrors()
        .assertComplete()
        .assertValue(GFYCAT_RESOLVED_LINK);
  }

  @Test
  public void when_auth_token_is_required_and_token_is_present() throws Exception {
    when(data.isAccessTokenRequired()).thenReturn(true);
    long thirtyMinutesFromNow = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30);
    when(data.tokenExpiryTimeMillis()).thenReturn(Single.just(thirtyMinutesFromNow));
    when(data.accessToken()).thenReturn(Single.just(AUTH_TOKEN));

    GfycatResponse response = GfycatResponse.create(GFYCAT_RESPONSE);
    when(dankApi.gfycat(AUTH_TOKEN, UNRESOLVED_ID)).thenReturn(Single.just(response));

    gfycatRepository.gif(UNRESOLVED_ID)
        .test()
        .assertNoErrors()
        .assertComplete()
        .assertValue(GFYCAT_RESOLVED_LINK);
  }

  @Test
  public void when_auth_token_is_required_and_token_is_expired() throws Exception {
    when(data.isAccessTokenRequired()).thenReturn(true);
    long thirtyMinutesBeforeNow = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30);
    when(data.tokenExpiryTimeMillis()).thenReturn(Single.just(thirtyMinutesBeforeNow));
    when(data.accessToken()).thenReturn(Single.just(AUTH_TOKEN));

    GfycatResponse response = GfycatResponse.create(GFYCAT_RESPONSE);
    when(dankApi.gfycat(AUTH_TOKEN, UNRESOLVED_ID)).thenReturn(Single.just(response));

    GfycatOauthResponse oAuthResponse = GfycatOauthResponse.create(3600, AUTH_TOKEN);
    when(dankApi.gfycatOAuth(anyString(), anyString())).thenReturn(Single.just(oAuthResponse));
    when(data.saveOAuthResponse(oAuthResponse)).thenReturn(Completable.complete());

    gfycatRepository.gif(UNRESOLVED_ID)
        .test()
        .assertNoErrors()
        .assertComplete()
        .assertValue(GFYCAT_RESOLVED_LINK);

    verify(dankApi).gfycatOAuth(anyString(), anyString());
    verify(data).saveOAuthResponse(oAuthResponse);
  }

  @Test
  public void when_access_token_is_not_used_and_gfycat_returns_with_403_then_get_token_and_retry() throws Exception {
    when(data.isAccessTokenRequired())
        .thenAnswer(o -> tokenRequired);
    doAnswer(invocation -> {
      tokenRequired = invocation.getArgument(0);
      //noinspection ReturnOfNull
      return null;
    }).when(data).setAccessTokenRequired(anyBoolean());

    HttpException forbiddenError = mock(HttpException.class);
    when(forbiddenError.code()).thenReturn(403);
    when(dankApi.gfycat(UNRESOLVED_ID)).thenReturn(Single.error(forbiddenError));

    when(data.tokenExpiryTimeMillis()).thenReturn(Single.just(0L));

    GfycatOauthResponse oAuthResponse = GfycatOauthResponse.create(3600, AUTH_TOKEN);
    when(dankApi.gfycatOAuth(anyString(), anyString())).thenReturn(Single.just(oAuthResponse));
    when(data.saveOAuthResponse(oAuthResponse)).thenReturn(Completable.complete());

    GfycatResponse response = GfycatResponse.create(GFYCAT_RESPONSE);
    when(dankApi.gfycat(AUTH_TOKEN, UNRESOLVED_ID)).thenReturn(Single.just(response));
    when(data.accessToken()).thenReturn(Single.just(AUTH_TOKEN));

    gfycatRepository.gif(UNRESOLVED_ID)
        .test()
        .assertNoErrors()
        .assertComplete()
        .assertValue(GFYCAT_RESOLVED_LINK);

    verify(data).setAccessTokenRequired(true);
    verify(dankApi).gfycatOAuth(anyString(), anyString());
    verify(data).saveOAuthResponse(oAuthResponse);
  }
}
