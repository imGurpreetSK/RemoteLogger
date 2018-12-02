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

import com.gurpreetsk.remotelogger.internal.RemoteLog

interface RemoteLogsStorage {
  /**
   * Setup the storage before performing any operations
   */
  fun setup()

  /**
   * Insert a single log into the storage
   */
  fun insertLog(priorityLevel: String, tag: String, log: String, throwable: Throwable?)

  /**
   * Get all the logs present in the storage.
   */
  suspend fun getLogs(): List<RemoteLog>

  /**
   * Delete a single log with the given [id] from the storage
   */
  fun deleteLog(id: Long)

  /**
   * Delete all available logs from storage
   */
  fun purge(): Int

  /**
   * Get count of total logs present
   *
   * @default -1
   * @return total count of present un-synced logs
   */
  fun getCount(): Long {
    return -1
  }

  /**
   * Free up any used resources.
   */
  fun teardown()
}
