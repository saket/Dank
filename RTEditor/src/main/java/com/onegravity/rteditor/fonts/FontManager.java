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
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Typeface;

import com.onegravity.rteditor.utils.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

/**
 * This class retrieves fonts from the assets and the Android system folders.
 */
public class FontManager {

    private static final String[] FONT_DIRS = {"/system/fonts", "/system/font", "/data/fonts"};

    private static final Map<String, String> ASSET_FONTS_BY_NAME = new TreeMap<String, String>();
    private static final Map<String, String> SYSTEM_FONTS_BY_PATH = new TreeMap<String, String>();
    private static final Map<String, String> SYSTEM_FONTS_BY_NAME = new TreeMap<String, String>();

    /*
     * Don't load the same font more than once -> cache them here.
     */
    private static final RTTypefaceSet ALL_FONTS = new RTTypefaceSet() {
        /**
         * @return The RTTypeface with the specified name or false if no such RTTypeface exists.
         */
        RTTypeface get(String fontName) {
            for (RTTypeface typeface : this) {
                if (typeface.getName().equals(fontName)) {
                    return typeface;
                }
            }
            return null;
        }

        /**
         * @return True if the collections contains an RTTypeface with the specified name, false otherwise.
         */
        boolean contains(String fontName) {
            return get(fontName) != null;
        }
    };

    /**
     * Use this method to preload fonts asynchronously e.g. when the app starts up.
     */
    public static void preLoadFonts(final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (ASSET_FONTS_BY_NAME) {
                    getAssetFonts(context);
                }
                synchronized (SYSTEM_FONTS_BY_NAME) {
                    getSystemFonts();
                }
            }
        }).start();
    }

    /**
     * Retrieve the fonts from the asset and the system folder.
     *
     * @return A Map mapping the name of the font to the Typeface.
     * If the name can't be retrieved the file name will be used (e.g. arial.ttf).
     */
    public static SortedSet<RTTypeface> getFonts(Context context) {
        /*
         * Fonts from the assets folder
         */
        Map<String, String> assetFonts = getAssetFonts(context);
        AssetManager assets = context.getResources().getAssets();
        for (String fontName : assetFonts.keySet()) {
            String filePath = assetFonts.get(fontName);
            if (!ALL_FONTS.contains(fontName)) {
                try {
                    Typeface typeface = Typeface.createFromAsset(assets, filePath);
                    ALL_FONTS.add(new RTTypeface(fontName, typeface));
                }
                catch (Exception e) {
                    // this can happen if we don't have access to the font or it's not a font or...
                }
            }
        }

        /*
         * Fonts from the system
         */
        Map<String, String> systemFonts = getSystemFonts();
        for (String fontName : systemFonts.keySet()) {
            String filePath = systemFonts.get(fontName);
            if (!ALL_FONTS.contains(fontName)) {
                try {
                    Typeface typeface = Typeface.createFromFile(filePath);
                    ALL_FONTS.add(new RTTypeface(fontName, typeface));
                }
                catch (Exception e) {
                    // this can happen if we don't have access to the font or it's not a font or...
                }
            }
        }

        return ALL_FONTS;
    }

    /**
     * Returns the RTTypeface for a specific font identified by name.
     * This assumes that the fonts have already been loaded.
     *
     * @return The RTTypeface for a specific font name or Null of no such font exists.
     */
    public static RTTypeface getTypeface(String fontName) {
        return ALL_FONTS.get(fontName);
    }

    /**
     * Retrieve the fonts from the asset folder.
     *
     * @return A Map mapping name of the font to the file path.
     * If the name can't be retrieved the file name will be used (e.g. arial.ttf).
     */
    private static Map<String, String> getAssetFonts(Context context) {
        synchronized (ASSET_FONTS_BY_NAME) {
            /*
             * Let's do this only once because it's expensive and the result won't change in any case.
             */
            if (ASSET_FONTS_BY_NAME.isEmpty()) {
                AssetManager assets = context.getResources().getAssets();

                Collection<String> fontFiles = AssetIndex.getAssetIndex(context);
                if (fontFiles == null || fontFiles.isEmpty()) {
                    fontFiles = listFontFiles(context.getResources());
                }

                for (String filePath : fontFiles) {
                    if (filePath.toLowerCase(Locale.getDefault()).endsWith("ttf")) {
                        String fontName = TTFAnalyzer.getFontName(assets, filePath);
                        if (fontName == null) {
                            fontName = getFileName(filePath);
                        }
                        ASSET_FONTS_BY_NAME.put(fontName, filePath);
                    }
                }
            }

            return ASSET_FONTS_BY_NAME;
        }
    }

    private static Collection<String> listFontFiles(Resources res) {
        Collection<String> fonts = new ArrayList<String>();
        listFontFiles(res.getAssets(), fonts, "");
        return fonts;
    }

    private static void listFontFiles(AssetManager assets, Collection<String> fonts, String path) {
        try {
            String[] list = assets.list(path);
            if (list != null && list.length > 0) {
                // it's a folder
                for (String file : list) {
                    String prefix = "".equals(path) ? "" : path + File.separator;
                    listFontFiles(assets, fonts, prefix + file);
                }
            } else if (path.endsWith("ttf")) {
                // it's a font file
                fonts.add(path);
            }
        } catch (IOException ignore) {
        }
    }

    /**
     * Retrieve the fonts from the system folders.
     *
     * @return A Map mapping name of the font to the file path.
     * If the name can't be retrieved the file name will be used (e.g. arial.ttf).
     */
    private static Map<String, String> getSystemFonts() {
        synchronized (SYSTEM_FONTS_BY_NAME) {
            for (String fontDir : FONT_DIRS) {
                File dir = new File(fontDir);

                if (!dir.exists()) continue;

                File[] files = dir.listFiles();

                if (files == null) continue;

                for (File file : files) {
                    String filePath = file.getAbsolutePath();
                    if (!SYSTEM_FONTS_BY_PATH.containsKey(filePath)) {
                        String fontName = TTFAnalyzer.getFontName(file.getAbsolutePath());
                        if (fontName == null) {
                            fontName = getFileName(filePath);
                        }
                        SYSTEM_FONTS_BY_PATH.put(filePath, fontName);
                        SYSTEM_FONTS_BY_NAME.put(fontName, filePath);
                    }
                }
            }

            return SYSTEM_FONTS_BY_NAME;
        }
    }

    private static String getFileName(String path) {
        return FilenameUtils.getBaseName(path).replace(File.pathSeparator, "");
    }

}
