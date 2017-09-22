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
import io.reactivex.ObservableSource;
import me.saket.dank.data.NetworkStrategy;

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
  public ObservableSource<Boolean> streamNetworkCapability(NetworkStrategy strategy) {
    return streamNetworkStateChanges()
        .distinctUntilChanged()
        .map(networkState -> satisfiesNetworkRequirement(strategy, networkState));
  }

  @CheckResult
  private Observable<NetworkState> streamNetworkStateChanges() {
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

  private boolean satisfiesNetworkRequirement(NetworkStrategy networkStrategy, NetworkState networkState) {
    if (networkStrategy == NetworkStrategy.NEVER || !networkState.isConnectedOrConnectingToInternet()) {
      return false;
    }

    //Timber.d("--------------------------------");
    boolean isConnectedToWifi = networkState.networkType() == ConnectivityManager.TYPE_WIFI;
    boolean isConnectedToMobileData = networkState.networkType() == ConnectivityManager.TYPE_MOBILE;
    //Timber.i("preference: %s", preference);
    //Timber.i("isConnectedToWifi: %s", isConnectedToWifi);
    //Timber.i("isConnectedToMobileData: %s", isConnectedToMobileData);

    if (networkStrategy == NetworkStrategy.WIFI_ONLY) {
      return isConnectedToWifi;
    }

    if (networkStrategy == NetworkStrategy.WIFI_OR_MOBILE_DATA) {
      return isConnectedToMobileData || isConnectedToWifi;
    }

    throw new AssertionError("Unknown network type. PreFillPreference: " + networkStrategy + ", network state: " + networkState);
  }
}
