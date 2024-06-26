/*
 * Copyright 2017 Keval Patel.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.mendhak.gpslogger.androidhiddencamera.config;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Keval on 12-Nov-16.
 * Supported image format lists.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

@SuppressWarnings("WeakerAccess")
public final class CameraImageFormat {

    /**
     * Image format for .jpg/.jpeg.
     */
    public static final int FORMAT_JPEG = 849;
    /**
     * Image format for .png.
     */
    public static final int FORMAT_PNG = 545;
    /**
     * Image format for .png.
     */
    public static final int FORMAT_WEBP = 563;

    private CameraImageFormat() {
        throw new RuntimeException("Cannot initialize CameraImageFormat.");
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FORMAT_JPEG, FORMAT_PNG, FORMAT_WEBP})
    public @interface SupportedImageFormat {
    }
}
