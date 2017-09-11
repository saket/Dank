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

package com.onegravity.rteditor.fonts;

import android.content.Context;
import android.util.Log;

import com.onegravity.rteditor.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsing all files in the asset folder is very slow.
 * Therefore we use a Gradle script to build an index over the asset folder during build time.
 * This class gives us access to that index file.
 * See also: http://stackoverflow.com/a/12639530/534471
 */
abstract class AssetIndex {

    private static final List<String> mAssetIndex = new ArrayList<String>();

    static List<String> getAssetIndex(Context context) {
        if (mAssetIndex.isEmpty()) {
            InputStream in = null;
            BufferedReader reader  = null;
            try {
                in = context.getAssets().open("assets.index");
                reader = new BufferedReader(new InputStreamReader(in));
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    mAssetIndex.add(line);
                }
            } catch (final IOException e) {
                Log.w(AssetIndex.class.getSimpleName(), "The assets.index file could not be read. If you want to use your own fonts, please copy the fonts to the assets folder and the build code to generate the assets.index file into your build.gradle (for more details consult the readme, chapter fonts)", e);
            } finally {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(reader);
            }
        }

        return mAssetIndex;
    }

}