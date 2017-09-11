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

import java.io.Closeable;
import java.io.IOException;

/**
 * We use this interface as an abstraction layer for the different ttf files:
 * AssetInputStream to read fonts from the assets folder and RandomAccessFile to read system fonts.
 */
public interface TTFInputStream extends Closeable {
    public int read (byte[] b) throws IOException;
    public int read () throws IOException;
    public void seek(long pos) throws IOException;
    public void close() throws IOException;
}
