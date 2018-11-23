package com.gurpreetsk.remotelogger

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.gurpreetsk.remotelogger.internal.logError
import io.hypertrack.smart_scheduler.Job.Builder
import io.hypertrack.smart_scheduler.Job.NetworkType
import io.hypertrack.smart_scheduler.Job.Type
import io.hypertrack.smart_scheduler.SmartScheduler

private const val REMOTE_LOGGER_URL    = "REMOTE_LOGGER_URL"
private const val DEFAULT_JOB_INTERVAL = 6 * 60 * 60 * 1000L // 6 hours

const val REMOTE_LOGGER_USER_UUID = "REMOTE_LOGGER_USER_UUID"

object RemoteLogger {
  private lateinit var storageType: RemoteLogsStorage
  private const val WTF: Int = 7447

  fun initialize(
      context: Context,
      url: String,
      userUniqueIdentifier: String,
      storageType: RemoteLogsStorage = SqliteRemoteLogsStorage(context, BuildConfig.DATABASE_NAME, BuildConfig.DATABASE_VERSION),
      jobIntervalMillis: Long = DEFAULT_JOB_INTERVAL
  ) {
    storageType.setup()
        .also {
          this.storageType = storageType

          storeUuidAndRemoteUrlToPreferences(context, userUniqueIdentifier, url)
        }
        .also { schedulePeriodicJob(storageType, jobIntervalMillis, context) }
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

  private fun storeUuidAndRemoteUrlToPreferences(context: Context, userUniqueIdentifier: String, url: String) {
    PreferenceManager
        .getDefaultSharedPreferences(context)
        .edit()
        .putString(REMOTE_LOGGER_USER_UUID, userUniqueIdentifier)
        .putString(REMOTE_LOGGER_URL, url)
        .apply()
  }

  private fun schedulePeriodicJob(storageType: RemoteLogsStorage, jobIntervalMillis: Long, context: Context) {
    val periodicTaskTag = "REMOTE-LOGGER-PERIODIC-JOB"

    val callback = SmartScheduler.JobScheduledCallback { jobContext, _ ->
      RemoteJobExecutor().execute(
          PreferenceManager.getDefaultSharedPreferences(jobContext).getString(REMOTE_LOGGER_URL, "")
              ?: "",
          storageType
      )
    }

    val job = Builder(1, callback, Type.JOB_TYPE_PERIODIC_TASK, periodicTaskTag)
        .setPeriodic(jobIntervalMillis)
        .setRequiredNetworkType(NetworkType.NETWORK_TYPE_CONNECTED)
        .build()

    val jobCreatedSuccessfully = SmartScheduler.getInstance(context)
        .addJob(job)

    if (!jobCreatedSuccessfully) {
      logError("RemoteLogger", "Job creation failed, your logs will not be synced", null)
    }
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
