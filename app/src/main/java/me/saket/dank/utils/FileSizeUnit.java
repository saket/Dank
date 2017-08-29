package me.saket.dank.utils;

import android.icu.util.TimeUnit;

/**
 * Inspired by {@link TimeUnit}.
 */
public enum FileSizeUnit {

  BYTES {
    public double toBytes(double bytes) {
      return bytes;
    }

    public double toKiloBytes(double bytes) {
      return bytes / 1024;
    }

    public double toMegaBytes(double bytes) {
      return toKiloBytes(bytes) / 1024;
    }

    public double toGigaBytes(double bytes) {
      return toMegaBytes(bytes) / 1024;
    }
  },

  KILOBYTES {
    public double toBytes(double kiloBytes) {
      return kiloBytes * 1024;
    }

    public double toKiloBytes(double kiloBytes) {
      return kiloBytes;
    }

    public double toMegaBytes(double kiloBytes) {
      return kiloBytes / 1024;
    }

    public double toGigaBytes(double kiloBytes) {
      return toMegaBytes(kiloBytes) / 1024;
    }
  },

  MEGABYTES {
    public double toBytes(double megaBytes) {
      return toKiloBytes(megaBytes) * 1024;
    }

    public double toKiloBytes(double megaBytes) {
      return megaBytes * 1024;
    }

    public double toMegaBytes(double megaBytes) {
      return megaBytes;
    }

    public double toGigaBytes(double megaBytes) {
      return megaBytes / 1024;
    }
  },

  GIGABYTES {
    public double toBytes(double gigaBytes) {
      return toKiloBytes(gigaBytes) * 1024;
    }

    public double toKiloBytes(double gigaBytes) {
      return toMegaBytes(gigaBytes) * 1024;
    }

    public double toMegaBytes(double gigaBytes) {
      return gigaBytes * 1024;
    }

    public double toGigaBytes(double gigaBytes) {
      return gigaBytes;
    }
  };

  public double toBytes(double megaBytes) {
    throw new AbstractMethodError();
  }

  public double toKiloBytes(double megaBytes) {
    throw new AbstractMethodError();
  }

  public double toMegaBytes(double megaBytes) {
    throw new AbstractMethodError();
  }

  public double toGigaBytes(double megaBytes) {
    throw new AbstractMethodError();
  }
}
