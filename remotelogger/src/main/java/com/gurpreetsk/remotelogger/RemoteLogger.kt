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
package com.gurpreetsk.remotelogger

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import io.hypertrack.smart_scheduler.Job.Builder
import io.hypertrack.smart_scheduler.Job.NetworkType
import io.hypertrack.smart_scheduler.Job.Type
import io.hypertrack.smart_scheduler.SmartScheduler

private const val REMOTE_LOGGER_URL    = "REMOTE_LOGGER_URL"
private const val DEFAULT_JOB_INTERVAL = 12 * 60 * 60 * 1000L // 12 hours

const val REMOTE_LOGGER_USER_UUID = "REMOTE_LOGGER_USER_UUID"

object RemoteLogger {
  private lateinit var storageType: RemoteLogsStorage
  private const val WTF: Int = 7447

  /**
   * Method to initialise the remote logging library.
   * This method should be called before calling any other methods.
   *
   * @param url The remote url to send the logs to.
   * API 28 & above require `https://` scheme. See https://developer.android.com/training/articles/security-config#CleartextTrafficPermitted for more info.
   * @param storageType The storage type where logs are stored.
   * @param jobIntervalMillis The time period after which log upload job should be re-run.
   */
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
   * Method to free up any resources used by the library.
   */
  fun teardown() {
    if (::storageType.isInitialized) {
      storageType.teardown()
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
      RemoteJobExecutor.execute(
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
      Log.e("RemoteLogger", "Job creation failed, your logs will not be synced.", null)
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
