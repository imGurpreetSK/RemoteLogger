package com.gurpreetsk.remotelogger.internal

import android.util.Log
import com.gurpreetsk.remotelogger.BuildConfig

fun logError(tag: String, message: String, throwable: Throwable?) {
  if (BuildConfig.DEBUG) {
    Log.e(tag, message, throwable)
  }
}

fun logInfo(tag: String, message: String) {
  if (BuildConfig.DEBUG) {
    Log.i(tag, message)
  }
}
