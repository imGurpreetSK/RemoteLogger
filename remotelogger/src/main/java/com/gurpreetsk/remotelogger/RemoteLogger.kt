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
import android.util.Log
import com.gurpreetsk.remotelogger.executor.RemoteJobExecutor
import com.gurpreetsk.remotelogger.internal.SettingsStore
import com.gurpreetsk.remotelogger.internal.SettingsStoreImpl
import com.gurpreetsk.remotelogger.storage.RemoteLogsStorage
import com.gurpreetsk.remotelogger.storage.SqliteRemoteLogsStorage
import io.hypertrack.smart_scheduler.Job.*
import io.hypertrack.smart_scheduler.SmartScheduler
import java.util.concurrent.TimeUnit

private const val REMOTE_LOGGER_URL = "REMOTE_LOGGER_URL"
private val DEFAULT_JOB_INTERVAL = Duration(12, TimeUnit.HOURS)
private const val WTF: Int = 7447

const val REMOTE_LOGGER_USER_UUID = "REMOTE_LOGGER_USER_UUID"

object RemoteLogger {

    @Volatile
    private lateinit var storageType: RemoteLogsStorage
    @Volatile
    private lateinit var settingsStore: SettingsStore

    /**
     * Method to initialise the remote logging library.
     * This method should be called before calling any other methods.
     *
     * @param url The remote url to send the logs to.
     * API 28 & above require `https://` scheme. See https://developer.android.com/training/articles/security-config#CleartextTrafficPermitted for more info.
     * @param logsStore The storage type where logs are stored.
     * @param jobInterval The time period after which log upload job should be re-run.
     */
    fun initialize(
            context: Context,
            url: String,
            userUniqueIdentifier: String,
            logsStore: RemoteLogsStorage = SqliteRemoteLogsStorage(context, BuildConfig.DATABASE_NAME, BuildConfig.DATABASE_VERSION),
            jobInterval: Duration = DEFAULT_JOB_INTERVAL
    ) {
        initializeSettingsStore(context, userUniqueIdentifier, url)
        initializeLogsStore(logsStore, jobInterval, context)
    }

    /**
     * Method to free up any resources used by the library.
     */
    fun teardown() {
        if (::storageType.isInitialized) {
            storageType.teardown()
        } else {
            reportUninitializedPropertyAccessError()
        }

        if (::settingsStore.isInitialized) {
            settingsStore.clear()
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * Verbose remote log.
     */
    fun v(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.VERBOSE), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * Debug remote log.
     */
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.DEBUG), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * Info remote log.
     */
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.INFO), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * Warning remote log.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.WARN), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * Error remote log.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.ERROR), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }


    /**
     * Assert remote log.
     */
    fun a(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(Log.ASSERT), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    /**
     * What a Terrible Failure ¯\_(ツ)_/¯
     */
    fun wtf(tag: String, message: String, throwable: Throwable? = null) {
        if (::storageType.isInitialized) {
            storageType.insertLog(getLogPriorityLevel(WTF), tag, message, throwable)
        } else {
            reportUninitializedPropertyAccessError()
        }
    }

    fun setUserIdentifier(userIdentifier: String) {
        settingsStore.putString(REMOTE_LOGGER_USER_UUID, userIdentifier)
    }

    private fun initializeSettingsStore(context: Context, userUniqueIdentifier: String, url: String) {
        if (!::settingsStore.isInitialized) {
            synchronized(SettingsStore::class) {
                this.settingsStore = SettingsStoreImpl(context)
                storeUuidAndRemoteUrl(userUniqueIdentifier, url)
            }
        }
    }

    private fun initializeLogsStore(logsStore: RemoteLogsStorage, jobInterval: Duration, context: Context) {
        if (!::storageType.isInitialized) {
            synchronized(RemoteLogsStorage::class) {
                logsStore.setup().also {
                    this.storageType = logsStore
                    schedulePeriodicJob(
                            logsStore,
                            jobInterval.unit.toMillis(jobInterval.time),
                            context
                    )
                }
            }
        }
    }

    private fun reportUninitializedPropertyAccessError() {
        Log.e(
                "Remote Logger",
                "Log Store not Initialized",
                Exception("Remote Logger: Log Store not Initialized")
        )
    }

    private fun storeUuidAndRemoteUrl(userUniqueIdentifier: String, url: String) {
        settingsStore.putString(REMOTE_LOGGER_USER_UUID, userUniqueIdentifier)
        settingsStore.putString(REMOTE_LOGGER_URL, url)
    }

    private fun schedulePeriodicJob(storageType: RemoteLogsStorage, jobInterval: Long, context: Context) {
        val periodicTaskTag = "REMOTE-LOGGER-PERIODIC-JOB"

        val callback = SmartScheduler.JobScheduledCallback { _, _ ->
            RemoteJobExecutor.execute(
                    settingsStore.getString(REMOTE_LOGGER_URL, ""),
                    storageType
            )
        }

        val job = Builder(1, callback, Type.JOB_TYPE_PERIODIC_TASK, periodicTaskTag)
                .setPeriodic(jobInterval)
                .setRequiredNetworkType(NetworkType.NETWORK_TYPE_CONNECTED)
                .build()

        val jobCreatedSuccessfully = SmartScheduler
                .getInstance(context)
                .addJob(job)

        if (!jobCreatedSuccessfully) {
            Log.e("RemoteLogger", "Job creation failed, your logs will not be synced.")
        }
    }

    private fun getLogPriorityLevel(messageLogLevel: Int): String =
            when (messageLogLevel) {
                Log.VERBOSE -> "VERBOSE"
                Log.DEBUG -> "DEBUG"
                Log.INFO -> "INFO"
                Log.WARN -> "WARN"
                Log.ERROR -> "ERROR"
                Log.ASSERT -> "ASSERT"
                WTF -> "WTF"

                else -> "UNKNOWN"
            }
}

data class Duration(
        val time: Long,
        val unit: TimeUnit
)
