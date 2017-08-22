package me.saket.dank.utils;

import android.content.res.Resources;
import android.os.Environment;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

import me.saket.dank.R;

/**
 * Because {@link Files} already exists.
 */
public class Files2 {

  private Files2() {
  }

  public static File copyFileToPicturesDirectory(Resources resources, File fileToCopy, String newFileName) throws IOException {
    File picturesDirectory = new File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath(),
        resources.getString(R.string.image_download_directory_name)
    );
    File userAccessibleFile = new File(picturesDirectory, newFileName);

    //noinspection ResultOfMethodCallIgnored
    picturesDirectory.mkdirs();
    //noinspection ResultOfMethodCallIgnored
    userAccessibleFile.createNewFile();

    Files.copy(fileToCopy, userAccessibleFile);
    return userAccessibleFile;
  }
}
