package me.saket.dank.utils;

import android.os.Build;

import javax.inject.Inject;

public class DeviceInfo {

  @Inject
  public DeviceInfo() {
  }

  public boolean isRunningOnEmulator() {
    return Build.FINGERPRINT.contains("generic");
  }

  public boolean isNougatMrOneOrAbove() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1;
  }
}
