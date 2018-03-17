package me.saket.dank.utils;

import android.content.res.Resources;
import android.icu.util.TimeUnit;

import me.saket.dank.R;

/**
 * Inspired by {@link TimeUnit}.
 */
public enum FileSizeUnit {

  BYTES {
    public double toBytes(double size) {
      return size;
    }

    public double toKiloBytes(double size) {
      return size / 1024;
    }

    public double toMegaBytes(double size) {
      return toKiloBytes(size) / 1024;
    }

    public double toGigaBytes(double size) {
      return toMegaBytes(size) / 1024;
    }
  },

  KB {
    public double toBytes(double size) {
      return size * 1024;
    }

    public double toKiloBytes(double size) {
      return size;
    }

    public double toMegaBytes(double size) {
      return size / 1024;
    }

    public double toGigaBytes(double size) {
      return toMegaBytes(size) / 1024;
    }
  },

  MB {
    public double toBytes(double size) {
      return toKiloBytes(size) * 1024;
    }

    public double toKiloBytes(double size) {
      return size * 1024;
    }

    public double toMegaBytes(double size) {
      return size;
    }

    public double toGigaBytes(double size) {
      return size / 1024;
    }
  },

  GB {
    public double toBytes(double size) {
      return toKiloBytes(size) * 1024;
    }

    public double toKiloBytes(double size) {
      return toMegaBytes(size) * 1024;
    }

    public double toMegaBytes(double size) {
      return size * 1024;
    }

    public double toGigaBytes(double size) {
      return size;
    }
  };

  public double toBytes(double size) {
    throw new AbstractMethodError();
  }

  public double toKiloBytes(double size) {
    throw new AbstractMethodError();
  }

  public double toMegaBytes(double size) {
    throw new AbstractMethodError();
  }

  public double toGigaBytes(double size) {
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
