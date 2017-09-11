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

package com.onegravity.rteditor.media.choose;

import android.content.Intent;
import android.os.Bundle;

import com.onegravity.rteditor.api.RTMediaFactory;
import com.onegravity.rteditor.api.media.RTAudio;
import com.onegravity.rteditor.api.media.RTImage;
import com.onegravity.rteditor.api.media.RTVideo;
import com.onegravity.rteditor.media.MonitoredActivity;
import com.onegravity.rteditor.media.choose.processor.AudioProcessor.AudioProcessorListener;
import com.onegravity.rteditor.utils.Constants.MediaAction;

class AudioChooserManager extends MediaChooserManager implements AudioProcessorListener {

    public interface AudioChooserListener extends MediaChooserListener {
        /**
         * Callback method to inform the caller that an audio file has been processed
         */
        public void onAudioChosen(RTAudio audio);
    }

    AudioChooserManager(MonitoredActivity activity,
                        MediaAction mediaAction, RTMediaFactory<RTImage, RTAudio, RTVideo> mediaFactory,
                        AudioChooserListener listener, Bundle savedInstanceState) {
        super(activity, mediaAction, mediaFactory, listener, savedInstanceState);
    }

    @Override
    boolean chooseMedia() throws IllegalArgumentException {
        return false;
    }

    @Override
    void processMedia(MediaAction mediaAction, Intent data) {
    }

    @Override
    /* AudioChooserListener */
    public void onAudioProcessed(RTAudio Audio) {
    }

}