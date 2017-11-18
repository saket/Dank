package me.saket.dank.utils;

import android.content.res.Resources;
import android.icu.util.TimeUnit;

import me.saket.dank.R;

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

  KB {
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

  MB {
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

  GB {
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

  public static String formatForDisplay(Resources resources, double size, FileSizeUnit sizeUnit) {
    int stringTemplateInSensibleUnitRes = R.string.filesize_gigabytes;
    double sizeInSensibleUnit = sizeUnit.toGigaBytes(size);

    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_megabytes;
      sizeInSensibleUnit = sizeUnit.toMegaBytes(size);
    }
    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_kilobytes;
      sizeInSensibleUnit = sizeUnit.toKiloBytes(size);
    }
    if (sizeInSensibleUnit < 1) {
      stringTemplateInSensibleUnitRes = R.string.filesize_bytes;
      sizeInSensibleUnit = sizeUnit.toBytes(size);
    }

    return resources.getString(stringTemplateInSensibleUnitRes, (int) sizeInSensibleUnit);
  }
}
