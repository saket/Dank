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

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * This class is a TTFInputStream for RandomAccessFile (used to read system fonts).
 */
public class TTFRandomAccessFile implements TTFInputStream {
    final private RandomAccessFile mFile;

    TTFRandomAccessFile(RandomAccessFile file) {
        mFile = file;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mFile.read(b);
    }

    @Override
    public int read() throws IOException {
        return mFile.read();
    }

    @Override
    public void seek(long pos) throws IOException {
        mFile.seek(pos);
    }

    @Override
    public void close() throws IOException {
        mFile.close();
    }
}
