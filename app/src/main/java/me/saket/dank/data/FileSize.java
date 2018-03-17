package me.saket.dank.data;

import com.google.auto.value.AutoValue;

import me.saket.dank.utils.FileSizeUnit;

@AutoValue
public abstract class FileSize {

  public abstract int size();

  public abstract FileSizeUnit unit();

  public static FileSize create(int size, FileSizeUnit unit) {
    return new AutoValue_FileSize(size, unit);
  }

  public double bytes() {
    return unit().toBytes(size());
  }
}
