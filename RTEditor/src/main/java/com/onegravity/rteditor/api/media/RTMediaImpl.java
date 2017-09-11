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

package com.onegravity.rteditor.api.media;

import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;

import com.onegravity.rteditor.api.format.RTFormat;

import com.onegravity.rteditor.utils.io.FilenameUtils;

import java.io.File;

/**
 * This is a basic implementation of the RTMedia interface.
 */
public abstract class RTMediaImpl implements RTMedia {
    private static final long serialVersionUID = 5098840799124458004L;

    private String mFilePath;

    public RTMediaImpl(String filePath) {
        mFilePath = filePath;
    }

    @Override
    public String getFilePath(RTFormat textFormat) {
        return mFilePath;
    }

    @Override
    public String getFileExtension() {
        return FilenameUtils.getExtension(mFilePath);
    }

    @Override
    public String getFileName() {
        return FilenameUtils.getName(mFilePath);
    }

    @Override
    public boolean exists() {
        return mFilePath != null && new File(mFilePath).exists();
    }

    @Override
    public void remove() {
        removeFile(mFilePath);
    }

    @Override
    public long getSize() {
        if (mFilePath != null) {
            File file = new File(mFilePath);
            return file.length();
        }
        return 0;
    }

    @Override
    public int getWidth() {
        return getWidth(getFilePath(RTFormat.SPANNED));
    }

    @Override
    public int getHeight() {
        return getHeight(getFilePath(RTFormat.SPANNED));
    }

    /**
     * This returns the file path passed into the constructor without
     * any modifications (unlike getFilePath(RTFormat) that might alter it).
     */
    protected String getFilePath() {
        return mFilePath;
    }

    /**
     * We need this since sub classes might want to use a custom
     * Serialization implementation.
     */
    protected void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    protected void removeFile(String path) {
        if (path != null) {
            File file = new File(path);
            file.delete();
        }
    }

    protected int getWidth(String filePath) {
        int width = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            String w = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            width = w.equals("0") ? getDimension(filePath, true) : Integer.parseInt(w);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
        return width;
    }

    protected int getHeight(String filePath) {
        int height = 0;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            String h = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            height = h.equals("0") ? getDimension(filePath, false) : Integer.parseInt(h);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
        return height;
    }

    private int getDimension(String path, boolean width) {
        // options.inJustDecodeBounds = true to get the out parameters without allocating the memory for the Bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.outWidth = 0;
        options.outHeight = 0;
        options.inSampleSize = 1;
        BitmapFactory.decodeFile(path, options);
        return width ? options.outWidth : options.outHeight;
    }

}