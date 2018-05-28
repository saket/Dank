package me.saket.dank.utils

import android.content.res.Resources
import android.os.Environment
import me.saket.dank.R
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 * Because [Files] already exists.
 */
object Files2 {

  /**
   * Copy to sdcard/Pictures/.
   */
  @Throws(IOException::class)
  fun copyFileToPicturesDirectory(resources: Resources, fileToCopy: File, newFileName: String): File {
    val picturesDirectory = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
        resources.getString(R.string.image_download_directory_name)
    )
    val userAccessibleFile = File(picturesDirectory, newFileName)

    picturesDirectory.mkdirs()
    userAccessibleFile.createNewFile()

    fileToCopy.copyTo(userAccessibleFile, overwrite = true)
    return userAccessibleFile
  }

  fun copy(from: File, to: File) {
    from.copyTo(to)
  }
}
