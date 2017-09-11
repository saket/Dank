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
import java.io.InputStream;

/**
 * This class is a TTFInputStream for AssetInputStream (used to read fonts from the assets folder).
 */
public class TTFAssetInputStream implements TTFInputStream {
    final private InputStream mIn;

    TTFAssetInputStream(InputStream in) {
        mIn = in;
        if (mIn.markSupported()) {
            mIn.mark(-1);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mIn.read(b);
    }

    @Override
    public int read() throws IOException {
        return mIn.read();
    }

    @Override
    public void seek(long pos) throws IOException {
        if (mIn.markSupported()) {
            mIn.reset();
            mIn.skip(pos);
        }
    }

    @Override
    public void close() throws IOException {
        mIn.close();
    }
}
