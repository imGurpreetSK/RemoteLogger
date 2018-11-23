package com.gurpreetsk.remotelogger

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log

val REMOTE_LOGGER_USER_UUID = "REMOTE_LOGGER_USER_UUID"
val REMOTE_LOGGER_URL = "REMOTE_LOGGER_URL"

object RemoteLogger {
  private lateinit var storageType: RemoteLogsStorage
  private const val WTF: Int = 7447

  fun initialize(
      context: Context,
      url: String,
      userUniqueIdentifier: String,
      storageType: RemoteLogsStorage = SqliteRemoteLogsStorage(context, BuildConfig.DATABASE_NAME, BuildConfig.DATABASE_VERSION)
  ) {
    storageType.setup().also {
      this.storageType = storageType

      PreferenceManager
          .getDefaultSharedPreferences(context)
          .edit()
          .putString(REMOTE_LOGGER_USER_UUID, userUniqueIdentifier)
          .putString(REMOTE_LOGGER_URL, url)
          .apply()
    }
  }

  /**
   * Verbose remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun v(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.VERBOSE), tag, message, throwable)
  }

  /**
   * Debug remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun d(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.DEBUG), tag, message, throwable)
  }

  /**
   * Info remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun i(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.INFO), tag, message, throwable)
  }

  /**
   * Warning remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun w(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.WARN), tag, message, throwable)
  }

  /**
   * Error remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun e(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.ERROR), tag, message, throwable)
  }


  /**
   * Assert remote log.
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun a(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(Log.ASSERT), tag, message, throwable)
  }

  /**
   * What a Terrible Failure ¯\_(ツ)_/¯
   *
   * @throws IllegalStateException if [storageType] is uninitialized.
   */
  fun wtf(tag: String, message: String, throwable: Throwable? = null) {
    check(::storageType.isInitialized)
    storageType.insertLog(getLogPriorityLevel(WTF), tag, message, throwable)
  }

  private fun getLogPriorityLevel(messageLogLevel: Int): String =
      when (messageLogLevel) {
        Log.VERBOSE -> "VERBOSE"
        Log.DEBUG   -> "DEBUG"
        Log.INFO    -> "INFO"
        Log.WARN    -> "WARN"
        Log.ERROR   -> "ERROR"
        Log.ASSERT  -> "ASSERT"
        WTF         -> "WTF"

        else -> "UNKNOWN"
      }
}
