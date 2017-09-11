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

package com.onegravity.rteditor.media.choose.processor;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.onegravity.rteditor.api.RTApi;
import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;

import com.onegravity.rteditor.utils.io.FilenameUtils;
import com.onegravity.rteditor.utils.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class MediaProcessor implements Runnable {

    public interface MediaProcessorListener {
        public void onError(String reason);
    }

    final private MediaProcessorListener mListener;

    final private String mOriginalFile;

    protected final RTMediaFactory<RTImage, RTAudio, RTVideo> mMediaFactory;

    public MediaProcessor(String originalFile, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory, MediaProcessorListener listener) {
        mOriginalFile = originalFile;
        mMediaFactory = mediaFactory;
        mListener = listener;
    }

    @Override
    final public void run() {
        try {
            processMedia();
        } catch (Exception e) {
            if (mListener != null) {
                mListener.onError(e.getMessage());
            }
        }
    }

    protected String getOriginalFile() {
        return mOriginalFile;
    }

    protected abstract void processMedia() throws IOException, Exception;

    protected InputStream getInputStream() throws IOException, Exception {
        InputStream in = null;
        if (mOriginalFile.startsWith("http")) {
            // http download
            in = downloadFile(mOriginalFile);
        } else if (mOriginalFile.startsWith("content://")) {
            // ContentProvider file
            in = processContentProviderMedia(mOriginalFile);
        } else {
            // file system
            in = copyFileToDir(mOriginalFile);
        }

        return in;
    }

    protected String getMimeType() throws IOException, Exception {
        if (mOriginalFile.startsWith("content://")) {
            // ContentProvider file
            ContentResolver resolver = RTApi.getApplicationContext().getContentResolver();
            Uri uri = Uri.parse(mOriginalFile);
            return resolver.getType(uri);
        }

        String extension = FilenameUtils.getExtension(mOriginalFile);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    private InputStream copyFileToDir(String sourceFile) {
        InputStream in = null;
        try {
            File fileFrom = new File(Uri.parse(sourceFile).getPath());
            in = new FileInputStream(fileFrom);
        } catch (IOException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }

        return in;
    }

    private InputStream downloadFile(String sourceFile) {
        InputStream in = null;
        try {
            URL url = new URL(sourceFile);
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(urlConnection.getInputStream()) {
                public void close() throws IOException {
                    super.close();
                    urlConnection.disconnect();
                }
            };
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
            IOUtils.closeQuietly(in);
        }

        return in;
    }

    private InputStream processContentProviderMedia(String sourceFile) {
        ContentResolver resolver = RTApi.getApplicationContext().getContentResolver();
        Uri uri = Uri.parse(sourceFile);

        InputStream in = null;
        try {
            in = resolver.openInputStream(uri);
        } catch (IOException ioe) {
            Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
        }

        return in;
    }

}