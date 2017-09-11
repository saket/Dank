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

package com.onegravity.rteditor.api;

import android.content.Context;
import android.util.Log;

import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTAudioImpl;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTImageImpl;
import com.onegravity.rteditor.api.media.RTMediaSource;
import com.onegravity.rteditor.api.media.RTMediaType;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.api.media.RTVideoImpl;
import com.onegravity.rteditor.media.MediaUtils;
import com.onegravity.rteditor.utils.Helper;

import com.onegravity.rteditor.utils.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is a basic implementation of the RTMediaFactory using either the
 * internal (as in Context.context.getFilesDir() or the primary external
 * file system (as in Context.getExternalFilesDir(String).
 */
public class RTMediaFactoryImpl implements RTMediaFactory<RTImage, RTAudio, RTVideo> {
    private static final long serialVersionUID = 6970361368051595063L;

    private File mStoragePath;

    public RTMediaFactoryImpl(Context context) {
        this(context, true);    // use external storage as default
    }

    public RTMediaFactoryImpl(Context context, boolean externalStorage) {
        mStoragePath = externalStorage ?
                context.getExternalFilesDir(null) :
                context.getFilesDir();
    }

    /**
     * Returns the absolute file path for a certain RTMediaType.
     * <p>
     * The media type specific path as provided by RTMediaType is appended to
     * the storage path (e.g. <storage area>/images for image files).
     */
    protected String getAbsolutePath(RTMediaType mediaType) {
        File mediaPath = new File(mStoragePath.getAbsolutePath(), mediaType.mediaPath());
        if (!mediaPath.exists()) {
            mediaPath.mkdirs();
        }
        return mediaPath.getAbsolutePath();
    }

    /*
     * Use case 1: Inserting media objects into the rich text editor.
     *
     * This default implementation copies all files into the dedicated media
     * storage area.
     */

    @Override
    /* @inheritDoc */
    public RTImage createImage(RTMediaSource mediaSource) {
        File targetFile = loadMedia(mediaSource);
        return targetFile == null ? null :
                new RTImageImpl(targetFile.getAbsolutePath());
    }

    @Override
    /* @inheritDoc */
    public RTAudio createAudio(RTMediaSource mediaSource) {
        File targetFile = loadMedia(mediaSource);
        return targetFile == null ? null :
                new RTAudioImpl(targetFile.getAbsolutePath());
    }

    @Override
    /* @inheritDoc */
    public RTVideo createVideo(RTMediaSource mediaSource) {
        File targetFile = loadMedia(mediaSource);
        return targetFile == null ? null :
                new RTVideoImpl(targetFile.getAbsolutePath());
    }

    private File loadMedia(RTMediaSource mediaSource) {
        File targetPath = new File(getAbsolutePath(mediaSource.getMediaType()));
        File targetFile = MediaUtils.createUniqueFile(targetPath,
                mediaSource.getName(),
                mediaSource.getMimeType(),
                false);

        copyFile(mediaSource.getInputStream(), targetFile);

        return targetFile;
    }

    private void copyFile(InputStream in, File targetFile) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(targetFile);
            IOUtils.copy(in, out);
        } catch (IOException ioe) {
            Log.e(getClass().getSimpleName(), ioe.getMessage(), ioe);
        } finally {
            Helper.closeQuietly(out);
            Helper.closeQuietly(in);
        }
    }

    /*
     * Use case 2: Load a rich text with referenced media objects into the rich
     * text editor.
     *
     * This default implementation doesn't apply any transformations to the path
     * because the files are stored in the file system where they can be
     * accessed directly by the rich text editor (via ImageSpan).
     */

    @Override
    /* @inheritDoc */
    public RTImage createImage(String path) {
        return new RTImageImpl(path);
    }

    @Override
    /* @inheritDoc */
    public RTAudio createAudio(String path) {
        return new RTAudioImpl(path);
    }

    @Override
    /* @inheritDoc */
    public RTVideo createVideo(String path) {
        return new RTVideoImpl(path);
    }

}