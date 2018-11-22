package com.gurpreetsk.remotelogger

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
}
