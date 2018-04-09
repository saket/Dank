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
}
