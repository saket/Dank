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

import android.content.res.AssetManager;

import com.onegravity.rteditor.utils.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * This class offers methods to analyze ttf files, namely to retrieve the font name.
 */
abstract class TTFAnalyzer {

    /**
     * Retrieve the file name for a system font.
     *
     * @param filePath the full path for the font to retrieve.
     *
     * @return The file name or null of none could be retrieved.
     */
    static String getFontName(String filePath) {
        TTFRandomAccessFile in = null;
        try {
            RandomAccessFile file = new RandomAccessFile(filePath, "r");
            in = new TTFRandomAccessFile(file);
            return getTTFFontName(in, filePath);
        } catch (IOException e) {
            return null;    // Missing permissions or corrupted font file?
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Retrieve the file name for a font in the asset folder.
     *
     * @param filePath the full path for the font in the asset folder to retrieve.
     *
     * @return The file name or null of none could be retrieved.
     */
    static String getFontName(AssetManager assets, String filePath) {
        TTFAssetInputStream in = null;
        try {
            InputStream file = assets.open(filePath, AssetManager.ACCESS_RANDOM);
            in = new TTFAssetInputStream(file);
            return getTTFFontName(in, filePath);
        } catch (FileNotFoundException e) {
            return null;    // Missing permissions?
        } catch (IOException e) {
            return null;    // Corrupted font file?
        }
        finally {
            IOUtils.closeQuietly(in);
        }
    }

    private static String getTTFFontName(TTFInputStream in, String fontFilename) {
        try {
            // Read the version first
            int version = readDword(in);

            // The version must be either 'true' (0x74727565) or 0x00010000 or 'OTTO' (0x4f54544f) for CFF style fonts.
            if (version != 0x74727565 && version != 0x00010000 && version != 0x4f54544f) {
                return null;
            }

            // The TTF file consist of several sections called "tables", and
            // we need to know how many of them are there.
            int numTables = readWord(in);

            // Skip the rest in the header
            readWord(in); // skip searchRange
            readWord(in); // skip entrySelector
            readWord(in); // skip rangeShift

            // Now we can read the tables
            for (int i = 0; i < numTables; i++) {
                // Read the table entry
                int tag = readDword(in);
                readDword(in); // skip checksum
                int offset = readDword(in);
                int length = readDword(in);

                // Now here' the trick. 'name' field actually contains the
                // textual string name.
                // So the 'name' string in characters equals to 0x6E616D65
                if (tag == 0x6E616D65) {
                    // Here's the name section. Read it completely into the
                    // allocated buffer
                    byte[] table = new byte[length];

                    in.seek(offset);
                    read(in, table);

                    // This is also a table. See
                    // http://developer.apple.com/fonts/ttrefman/rm06/Chap6name.html
                    // According to Table 36, the total number of table
                    // records is stored in the second word, at the offset
                    // 2.
                    // Getting the count and string offset - remembering
                    // it's big endian.
                    int count = getWord(table, 2);
                    int string_offset = getWord(table, 4);

                    // Record starts from offset 6
                    for (int record = 0; record < count; record++) {
                        // Table 37 tells us that each record is 6 words ->
                        // 12 bytes, and that the nameID is 4th word so its
                        // offset is 6.
                        // We also need to account for the first 6 bytes of
                        // the header above (Table 36), so...
                        int nameid_offset = record * 12 + 6;
                        int platformID = getWord(table, nameid_offset);
                        int nameid_value = getWord(table, nameid_offset + 6);

                        // Table 42 lists the valid name Identifiers. We're
                        // interested in 4 but not in Unicode encoding (for
                        // simplicity).
                        // The encoding is stored as PlatformID and we're
                        // interested in Mac encoding
                        if (nameid_value == 4 && platformID == 1) {
                            // We need the string offset and length, which
                            // are the word 6 and 5 respectively
                            int name_length = getWord(table,
                                    nameid_offset + 8);
                            int name_offset = getWord(table,
                                    nameid_offset + 10);

                            // The real name string offset is calculated by
                            // adding the string_offset
                            name_offset = name_offset + string_offset;

                            // Make sure it is inside the array
                            if (name_offset >= 0 && name_offset + name_length < table.length) {
                                return new String(table, name_offset, name_length);
                            }
                        }
                    }
                }
            }

            return null;
        } catch (FileNotFoundException e) {
            // Permissions?
            return null;
        } catch (IOException e) {
            // Most likely a corrupted font file
            return null;
        }
    }

    private static int readByte(TTFInputStream in) throws IOException {
        return in.read() & 0xFF;
    }

    private static int readWord(TTFInputStream in) throws IOException {
        int b1 = readByte(in);
        int b2 = readByte(in);

        return b1 << 8 | b2;
    }

    private static int readDword(TTFInputStream in) throws IOException {
        int b1 = readByte(in);
        int b2 = readByte(in);
        int b3 = readByte(in);
        int b4 = readByte(in);

        return b1 << 24 | b2 << 16 | b3 << 8 | b4;
    }

    private static void read(TTFInputStream in, byte[] array) throws IOException {
        if (in.read(array) != array.length) throw new IOException();
    }

    private static int getWord(byte[] array, int offset) {
        int b1 = array[offset] & 0xFF;
        int b2 = array[offset + 1] & 0xFF;

        return b1 << 8 | b2;
    }

}