package com.gurpreetsk.remotelogger

import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class SqliteRemoteLogsStorageTests {
  private val tableName  = "remote_logs"
  private val id         = "log_local_id"
  private val userUUID   = "user_identifier"
  private val timestamp  = "timestamp"
  private val osName     = "os_name"
  private val osVersion  = "os_version"
  private val appVersion = "app_version"
  private val logTag     = "log_tag"
  private val logLevel   = "log_level"
  private val message    = "message"
  private val stackTrace = "stack_trace"

  private lateinit var storage: SqliteRemoteLogsStorage
  private lateinit var database: SQLiteDatabase

  @Before fun setup() {
    val databaseName = "RemoteLoggerDb"
    val context  = InstrumentationRegistry.getContext()
    storage = SqliteRemoteLogsStorage(context, databaseName, 1)
    storage.setup()

    database = storage.writableDatabase
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS $tableName ($id INTEGER PRIMARY KEY AUTOINCREMENT, $userUUID TEXT, $timestamp INTEGER, $osName TEXT, $osVersion TEXT, $appVersion TEXT, $logTag TEXT, $logLevel TEXT, $message TEXT, $stackTrace TEXT);"
    )
  }

  @Test fun insertLogIntoTable() {
    // Act
    storage.insertLog("DEBUG", "DEBUG", "Room is so much better :/", null)

    // Assert
    assertThat(DatabaseUtils.queryNumEntries(database, tableName))
        .isEqualTo(1)
  }

  @After fun teardown() {
    database.delete(tableName, null, null)
  }
}
