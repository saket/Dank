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

package com.onegravity.rteditor.utils;

public abstract class Constants {

    /*
     * Request codes for startActivityForResult calls.
     * Change to codes if they overlap with the ones already in use by the app.
     */
    public enum MediaAction {
        PICK_PICTURE(101),
        PICK_VIDEO(102),
        PICK_AUDIO(103),
        CAPTURE_PICTURE(104),
        CAPTURE_VIDEO(105),
        CAPTURE_AUDIO(106);

        private int mRequestCode;

        private MediaAction(int requestCode) {
            mRequestCode = requestCode;
        }

        public int requestCode() {
            return mRequestCode;
        }
    }

    public final static int CROP_IMAGE = 107;

}
