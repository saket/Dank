package me.saket.dank.utils;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import me.saket.dank.ui.preferences.NetworkStrategy;

public class NetworkStateListenerShould {

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ConnectivityManager connectivityManager;

  private NetworkStateListener networkStateListener;

  @Before
  public void setUp() {
    networkStateListener = Mockito.spy(new NetworkStateListener(connectivityManager));
  }

  @Test
  public void whenPhoneIsConnectedToMobileData_shouldReturnFalseForInternetCapabilityWithWifi() {
    NetworkInfo networkInfo = mock(NetworkInfo.class);
    when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
    when(networkInfo.getType()).thenReturn(ConnectivityManager.TYPE_MOBILE);
    when(connectivityManager.getActiveNetworkInfo()).thenReturn(networkInfo);

    doAnswer(invocation -> {
      ConnectivityManager.NetworkCallback callbacks = invocation.getArgument(1);
      callbacks.onAvailable(mock(Network.class));
      //noinspection ReturnOfNull
      return null;
    }).when(connectivityManager).registerNetworkCallback(any(NetworkRequest.class), any(ConnectivityManager.NetworkCallback.class));
    doReturn(mock(NetworkRequest.class)).when(networkStateListener).createInternetCapableNetworkRequest();

    networkStateListener.streamNetworkInternetCapability(NetworkStrategy.WIFI_ONLY, Optional.empty())
        .take(1)
        .test()
        .assertValue(false);
  }
}
