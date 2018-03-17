package me.saket.dank.data;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;

import javax.inject.Inject;

import me.saket.dank.utils.FileSizeUnit;

public class DeviceInfo {

  private final Application appContext;

  @Inject
  public DeviceInfo(Application appContext) {
    this.appContext = appContext;
  }

  public FileSize memoryClass() {
    ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
    //noinspection ConstantConditions
    int memoryClass = activityManager.getMemoryClass();
    return FileSize.create(memoryClass, FileSizeUnit.MB);
  }
}
