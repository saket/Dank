package me.saket.dank.data;

import javax.inject.Inject;

import me.saket.dank.BuildConfig;

public class AppInfo {

  @Inject
  public AppInfo() {
  }

  public String appVersionName() {
    return BuildConfig.VERSION_NAME;
  }

  public int appVersionCode() {
    return BuildConfig.VERSION_CODE;
  }
}
