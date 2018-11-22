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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SqliteRemoteLogsStorage(
    private val context: Context,
    databaseName: String,
    databaseVersion: Int
) : SQLiteOpenHelper(context, databaseName, null, databaseVersion), RemoteLogsStorage {
  private lateinit var database: SQLiteDatabase

  override fun setup() {
    GlobalScope.launch {
      database = writableDatabase
    }
  }

  private val tableName: String = "remote-logs"
  private val id: String = "log-local-id"
  private val userUUID: String = "user-identifier"
  private val timestamp: String = "timestamp"
  private val osName: String = "os-name"
  private val osVersion: String = "os-version"
  private val appVersion: String = "app-version"
  private val logTag: String = "log-tag"
  private val logLevel: String = "log-level"
  private val message: String = "message"
  private val stackTrace: String = "stack-trace"

  override fun onCreate(db: SQLiteDatabase) {
    GlobalScope.launch {
      try {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS "
                + tableName
                + " ("
                + id + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + userUUID + " TEXT "
                + timestamp + " INTEGER "
                + osName + " TEXT "
                + osVersion + " TEXT "
                + appVersion + " TEXT "
                + logTag + " TEXT "
                + logLevel + " TEXT "
                + message + " TEXT "
                + stackTrace + " TEXT"
                + ");"
        )
        logInfo("SqliteRemoteLogsStorage", "$databaseName created")
      } catch (e: Exception) {
        logError("SqliteRemoteLogsStorage", "Exception occurred while creating remote logging database $databaseName", e)
      }
    }
  }

  override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    GlobalScope.launch {
      db.execSQL("DROP TABLE IF EXISTS $tableName")
      onCreate(db)
    }
  }

  override fun insertLog(priorityLevel: String, tag: String, log: String, throwable: Throwable?) {
    GlobalScope.launch {
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

      try {
        if (::database.isInitialized) {
          database.insert(tableName, null, values)
          logInfo("SqliteRemoteLogsStorage", "$values inserted!")
        } else {
          logError("SqliteRemoteLogsStorage", "DATABASE IS UNINITIALISED!", null)
        }
      } catch (e: Exception) {
        logError("SqliteRemoteLogsStorage", "Exception occurred while inserting log into $tableName", e)
      }
    }
  }

  override fun deleteLog(logId: Long) {
    GlobalScope.launch {
      database.delete(tableName, "$id = ?", arrayOf(logId.toString()))
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
}
