package com.gurpreetsk.remotelogger

import android.content.ContentValues
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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

    database = storage.writableDatabase
    database.execSQL(
        "CREATE TABLE IF NOT EXISTS $tableName ($id INTEGER PRIMARY KEY AUTOINCREMENT, $userUUID TEXT, $timestamp INTEGER, $osName TEXT, $osVersion TEXT, $appVersion TEXT, $logTag TEXT, $logLevel TEXT, $message TEXT, $stackTrace TEXT);"
    )
  }

  @Test fun insertLogIntoTable() {
    // Act
    storage.setup()
    storage.insertLog("DEBUG", "DEBUG", "Room is so much better :/", null)

    // Assert
    assertThat(DatabaseUtils.queryNumEntries(database, tableName))
        .isEqualTo(1)
  }

  @Test fun neverInsertLogIntoTableIfDatabaseIsNotInitialised() {
    // Act
    storage.insertLog("DEBUG", "DEBUG", "Room is so much better :/", null)

    // Assert
    assertThat(DatabaseUtils.queryNumEntries(database, tableName))
        .isEqualTo(0)
  }

  @Test fun getLogsFromDatabase() {
    // Setup
    val values: ContentValues = getContentValues()

    // Act
    storage.setup()
    database.insert(tableName, null, values)

    val logs = runBlocking {
      storage.getLogs()
    }

    // Assert
    val expectedLogs = listOf(
        RemoteLog(
            1,
            "Gurpreet",
            123456789,
            "ANDROID",
            "28",
            "1",
            "DEBUG",
            "DEBUG",
            "Room is so much better :/",
            ""
        )
    )
    assertThat(logs)
        .isEqualTo(expectedLogs)
  }

  @Test fun getEmptyListFromDatabaseIfNoLogsArePresent() {
    // Act
    storage.setup()

    val logs = runBlocking {
      storage.getLogs()
    }

    // Assert
    assertThat(logs)
        .isEqualTo(emptyList<RemoteLog>())
  }

  @Test fun deleteLogWithGivenId() {
    // Setup
    storage.setup()
    val values: ContentValues = getContentValues()
    database.insert(tableName, null, values)

    // Act
    storage.deleteLog(1)

    // Assert
    val logs = runBlocking { storage.getLogs() }
    assertThat(logs)
        .isEqualTo(emptyList<RemoteLog>())
  }

  @Test fun deleteAllDatabaseContents() {
    // Setup
    storage.setup()
    val values: ContentValues = getContentValues()
    database.insert(tableName, null, values)

    // Act
    storage.purge()

    // Assert
    val logs = runBlocking { storage.getLogs() }
    assertThat(logs)
        .isEqualTo(emptyList<RemoteLog>())
  }

  @Test fun getTotalLogsPresentInDatabase() {
    // Setup
    storage.setup()
    val values: ContentValues = getContentValues()
    database.insert(tableName, null, values)

    // Act
    val count = storage.getCount()

    // Assert
    assertThat(count).isEqualTo(1)
  }

  private fun getContentValues(): ContentValues = ContentValues()
      .also {
        with(it) {
          put(id, 1)
          put(userUUID, "Gurpreet")
          put(timestamp, "123456789")
          put(osName, "ANDROID")
          put(osVersion, 28)
          put(appVersion, 1)
          put(logTag, "DEBUG")
          put(logLevel, "DEBUG")
          put(message, "Room is so much better :/")
          put(stackTrace, "")
        }
      }

  @After fun teardown() {
    database.delete(tableName, null, null)
  }
}
