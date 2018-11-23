package com.gurpreetsk.remotelogger

import android.app.Application
import com.facebook.stetho.Stetho

class RemoteLoggingApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    Stetho.initializeWithDefaults(this)

    setupRemoteLogging()
  }

  private fun setupRemoteLogging() {
    RemoteLogger.initialize(this, "some-url", "Gurpreet")
  }
}
