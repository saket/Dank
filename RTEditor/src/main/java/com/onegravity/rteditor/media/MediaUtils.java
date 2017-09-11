/*
 * Copyright (C) 2015-2017 Emanuel Moecklin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegravity.rteditor.media;

import java.io.File;
import java.util.Calendar;

import com.onegravity.rteditor.utils.io.FilenameUtils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore.MediaColumns;
import android.webkit.MimeTypeMap;

import com.onegravity.rteditor.utils.Helper;

/**
 * Collection of utility functions used in the media package
 */
public class MediaUtils {

    /**
     * Creates a file with a non-conflicting file name in a specified folder based on an existing file name.
     *
     * @param targetFolder The target folder (e.g. /sdcard/Android/data)
     * @param originalFile The source file including the path (e.g. /sdcard/image.jpg)
     * @param mimeType     If the originalFile has no extension (e.g. for files provided by picasa) we use the mime type
     *                     to determine the file extension
     * @return The non-conflicting file name in targetFolder (e.g. /sdcard/Android/data/158867324_201308071234568.jpg)
     */
    public static File createUniqueFile(File targetFolder, String originalFile, boolean keepOriginal) {
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(originalFile);
        return createUniqueFile(targetFolder, originalFile, mimeType, keepOriginal);
    }

    public static File createUniqueFile(File targetFolder, String originalFile, String mimeType, boolean keepOriginal) {
        /*
         * We try to get the extension from the file name first.
         * If that fails (e.g. for images provided by the picasa content provider)
         * we use the mime type to determine the extension.
         * The extension is important to be able to determine the correct content type
         * once we create a MIME message.
         */
        String extension = FilenameUtils.getExtension(originalFile);
        if (isNullOrEmpty(extension)) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        String random = Long.toString(Math.round(Math.random() * Integer.MAX_VALUE));        // random part
        long timestamp = Calendar.getInstance().getTimeInMillis();                            // time stamp

        if (keepOriginal) {
            String baseName = FilenameUtils.getBaseName(originalFile);
            return new File(targetFolder + File.separator + baseName + "_" + random + "_" + timestamp + "." + extension);
        } else {
            return new File(targetFolder + File.separator + random + "_" + timestamp + "." + extension);
        }
    }

    /**
     * Creates a file Uri for a file defined by its absolute path.
     * The method can handle the case of an absolute path (e.g. /data/data....)
     * and a Uri path containing the file:// scheme (e.g. file:///data/data...)
     */
    public static Uri createFileUri(String path) {
        if (path.startsWith("file://")) {
            return Uri.parse(path);
        }
        return Uri.fromFile(new File(path));
    }

    /**
     * Retrieve local file path for an arbitrary Uri
     *
     * @throws IllegalArgumentException If the uri is null or we can't resolve the uri to an absolute file path
     *                                  (meaning the uri isn't valid)
     */
    public static String determineOriginalFile(Context context, Uri uri) throws IllegalArgumentException {
        String originalFile = null;

        if (uri != null) {
            // Picasa on Android >= 3.0 or other files using content providers
            if (uri.getScheme().startsWith("content")) {
                originalFile = getPathFromUri(context, uri);
            }

            // Picasa on Android < 3.0
            if (uri.toString().matches("https?://\\w+\\.googleusercontent\\.com/.+")) {
                originalFile = uri.toString();
            }

            // local storage
            if (uri.getScheme().startsWith("file")) {
                originalFile = uri.toString().substring(7);
            }

            if (isNullOrEmpty(originalFile)) {
                throw new IllegalArgumentException("File path was null");
            }
        } else {
            throw new IllegalArgumentException("Image Uri was null!");
        }

        return originalFile;
    }

    private static String getPathFromUri(Context context, Uri imageUri) {
        String filePath = "";

        if (imageUri.toString().startsWith("content://com.android.gallery3d.provider")) {
            imageUri = Uri.parse(imageUri.toString().replace("com.android.gallery3d", "com.google.android.gallery3d"));
        }

        Cursor cursor = null;
        try {
            String column = MediaColumns.DATA;
            String[] proj = {MediaColumns.DATA};

            cursor = context.getContentResolver().query(imageUri, proj, null, null, null);
            cursor.moveToFirst();

            if (imageUri.toString().startsWith("content://com.google.android.gallery3d")) {
                filePath = imageUri.toString();
            } else {
                filePath = cursor.getString(cursor.getColumnIndexOrThrow(column));
            }
        } catch (Exception ignore) {
            // Google Drive content provider throws an exception that we ignore
            // content://com.google.android.apps.docs.storage
        } finally {
            Helper.closeQuietly(cursor);
        }

        if (isNullOrEmpty(filePath) || !new File(filePath).exists() ||
                imageUri.toString().startsWith("content://com.google.android.gallery3d")) {
            filePath = imageUri.toString();
        }

        return filePath;
    }

    private static boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }
}