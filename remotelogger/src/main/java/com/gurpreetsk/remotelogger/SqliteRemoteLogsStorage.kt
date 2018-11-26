package com.gurpreetsk.remotelogger

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.gurpreetsk.remotelogger.internal.logError
import com.gurpreetsk.remotelogger.internal.logInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SqliteRemoteLogsStorage(
    private val context: Context,
    databaseName: String,
    databaseVersion: Int
) : SQLiteOpenHelper(context, databaseName, null, databaseVersion), RemoteLogsStorage {
  private lateinit var database: SQLiteDatabase

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

  override fun setup() {
    CoroutineScope(Dispatchers.IO).launch {
      database = writableDatabase
    }
  }

  override fun onCreate(db: SQLiteDatabase) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $tableName ($id INTEGER PRIMARY KEY AUTOINCREMENT, $userUUID TEXT, $timestamp INTEGER, $osName TEXT, $osVersion TEXT, $appVersion TEXT, $logTag TEXT, $logLevel TEXT, $message TEXT, $stackTrace TEXT);"
        )
        logInfo("SqliteRemoteLogsStorage", "$databaseName created")
      } catch (e: Exception) {
        logError("SqliteRemoteLogsStorage", "Exception occurred while creating remote logging database $databaseName", e)
      }
    }
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    CoroutineScope(Dispatchers.IO).launch {
      db.execSQL("DROP TABLE IF EXISTS $tableName")
      onCreate(db)
    }
  }

  override fun insertLog(priorityLevel: String, tag: String, log: String, throwable: Throwable?) {
    CoroutineScope(Dispatchers.IO).launch {
      val values: ContentValues = ContentValues()
          .also {
            with(it) {
              put(userUUID, PreferenceManager.getDefaultSharedPreferences(context).getString(REMOTE_LOGGER_USER_UUID, ""))
              put(timestamp, System.currentTimeMillis())
              put(osName, "ANDROID")
              put(osVersion, Build.VERSION.SDK_INT)
              put(appVersion, BuildConfig.VERSION_CODE)
              put(logTag, tag)
              put(logLevel, priorityLevel)
              put(message, log)
              put(stackTrace, Log.getStackTraceString(throwable))
            }
          }

      if (::database.isInitialized) {
        try {
          database.insert(tableName, null, values)
          logInfo("SqliteRemoteLogsStorage", "$values inserted!")
        } catch (e: Exception) {
          logError("SqliteRemoteLogsStorage", "Exception while inserting into $tableName!", null)
        }
      } else {
        logError("SqliteRemoteLogsStorage", "DATABASE IS UNINITIALISED!", null)
      }
    }
  }

  override suspend fun getLogs(): List<RemoteLog> {
    check(::database.isInitialized)
    val deviceLogList = mutableListOf<RemoteLog>()

    val cursor = database.query(tableName, null, null, null, null, null, null, null)
    if (cursor == null || cursor.isClosed) {
      logInfo("SqliteRemoteLogsStorage", "Cursor is either null or closed")
      return emptyList()
    }

    try {
      if (cursor.moveToFirst()) {
        do {
          if (cursor.isClosed) {
            break
          }

          deviceLogList.add(RemoteLog(
              cursor.getInt(0),
              cursor.getString(1),
              cursor.getLong(2),
              cursor.getString(3),
              cursor.getString(4),
              cursor.getString(5),
              cursor.getString(6),
              cursor.getString(7),
              cursor.getString(8),
              cursor.getString(9)
          ))
        } while (cursor.moveToNext())
      }
    } catch (e: Exception) {
      e.printStackTrace()
      logError("SqliteRemoteLogsStorage", "Exception occurred while reading logs from $tableName", e)
    } finally {
      cursor.close()
    }

    return deviceLogList
  }

  override fun deleteLog(logId: Long) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        database.delete(tableName, "$id = ?", arrayOf(logId.toString()))
      } catch (e: Exception) {
        logError("SqliteRemoteLogsStorage", "Log with id $logId not present in $tableName", e)
      }
    }
  }

  override fun purge(): Int {
    return database
        .delete(tableName ,"1", null)
  }

  override fun getCount(): Long {
    return if (::database.isInitialized) {
      try {
        DatabaseUtils.queryNumEntries(database, tableName)
      } catch (e: Exception) {
        logError("SqliteRemoteLogsStorage", "Exception occurred while fetching logs count from $tableName", e)
        super.getCount()
      }
    } else {
      logError("SqliteRemoteLogsStorage", "DATABASE IS UNINITIALISED!", null)
      super.getCount()
    }
  }

  override fun teardown() {
    if (::database.isInitialized && database.isOpen) {
      database.close()
    }
  }
}
