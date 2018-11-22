package com.gurpreetsk.remotelogger

import android.content.Context
import android.preference.PreferenceManager

val REMOTE_LOGGER_USER_UUID = "REMOTE_LOGGER_USER_UUID"
val REMOTE_LOGGER_URL = "REMOTE_LOGGER_URL"

object RemoteLogger {
  fun initialize(
      context: Context,
      url: String,
      userUniqueIdentifier: String,
      storageType: RemoteLogsStorage = SqliteRemoteLogsStorage(context, BuildConfig.DATABASE_NAME, BuildConfig.DATABASE_VERSION)
  ) {
    storageType.setup().also {
      PreferenceManager
          .getDefaultSharedPreferences(context)
          .edit()
          .putString(REMOTE_LOGGER_USER_UUID, userUniqueIdentifier)
          .putString(REMOTE_LOGGER_URL, url)
          .apply()
    }
  }
}
