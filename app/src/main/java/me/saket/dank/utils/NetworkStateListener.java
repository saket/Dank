package me.saket.dank.utils;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.support.annotation.CheckResult;

import com.google.auto.value.AutoValue;

import javax.inject.Inject;

import io.reactivex.Observable;
import timber.log.Timber;

public class NetworkStateListener {

  private ConnectivityManager connectivityManager;

  @AutoValue
  public abstract static class NetworkState {
    public abstract boolean isConnectedOrConnectingToInternet();

    public abstract int networkType();

    public static NetworkState createActive(boolean isConnectedOrConnectingToInternet, int networkType) {
      return new AutoValue_NetworkStateListener_NetworkState(isConnectedOrConnectingToInternet, networkType);
    }

    public static NetworkState createInactive() {
      return new AutoValue_NetworkStateListener_NetworkState(false, -1);
    }
  }

  @Inject
  public NetworkStateListener(ConnectivityManager connectivityManager) {
    this.connectivityManager = connectivityManager;
  }

  @CheckResult
  public Observable<NetworkState> streamNetworkStateChanges() {
    return Observable.create(emitter -> {
      ConnectivityManager.NetworkCallback networkCallbacks = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
          //Timber.d("Network changed");
          NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
          emitter.onNext(activeNetworkInfo == null
              ? NetworkState.createInactive()
              : NetworkState.createActive(activeNetworkInfo.isConnectedOrConnecting(), activeNetworkInfo.getType())
          );
        }

        @Override
        public void onLost(Network network) {
          emitter.onNext(NetworkState.createInactive());
        }
      };

      NetworkRequest networkRequest = new NetworkRequest.Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
          .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
          .build();

      // Note: ConnectivityManager gives a callback with the default value right away.
      connectivityManager.registerNetworkCallback(networkRequest, networkCallbacks);
      emitter.setCancellable(() -> connectivityManager.unregisterNetworkCallback(networkCallbacks));
    });
  }
}
