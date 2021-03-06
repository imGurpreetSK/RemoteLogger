/*
 * Copyright (C) 2018 Gurpreet Singh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gurpreetsk.remotelogger.internal

import android.util.Log
import com.gurpreetsk.remotelogger.BuildConfig

fun logError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}

fun logInfo(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
        Log.i(tag, message)
    }
}
