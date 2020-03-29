package com.gurpreetsk.remotelogger

import android.content.ContentValues
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.gurpreetsk.remotelogger.SqliteRemoteLogsStorageTests.Column.*
import com.gurpreetsk.remotelogger.internal.RemoteLog
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SqliteRemoteLogsStorageTests {

    private val tableName = "remote_logs"
    private enum class Column(val value: String) {
        ID("log_local_id"),
        USER_UUID("user_identifier"),
        TIMESTAMP("timestamp"),
        OS_NAME("os_name"),
        OS_VERSION("os_version"),
        APP_VERSION("app_version"),
        LOG_TAG("log_tag"),
        LOG_LEVEL("log_level"),
        MESSAGE("message"),
        STACKTRACE("stack_trace"),
    }
    private val genericLogMessage = "Room is so much better :/"

    private lateinit var storage: SqliteRemoteLogsStorage
    private lateinit var database: SQLiteDatabase

    @Before
    fun setup() {
        val databaseName = "RemoteLoggerDb"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        storage = SqliteRemoteLogsStorage(context, databaseName, 1)

        database = storage.writableDatabase
        database.execSQL(
                """CREATE TABLE IF NOT EXISTS $tableName (
                                ${ID.value} INTEGER PRIMARY KEY AUTOINCREMENT, 
                                ${USER_UUID.value} TEXT, 
                                ${TIMESTAMP.value} INTEGER, 
                                ${OS_NAME.value} TEXT, 
                                ${OS_VERSION.value} TEXT, 
                                ${APP_VERSION.value} TEXT, 
                                ${LOG_TAG.value} TEXT, 
                                ${LOG_LEVEL.value} TEXT, 
                                ${MESSAGE.value} TEXT, 
                                ${STACKTRACE.value} TEXT
                        );
                """
        )
    }

    @Test
    fun insertLogIntoTable() {
        // Act
        storage.setup()
        storage.insertLog("DEBUG", "DEBUG", genericLogMessage, null)

        // Assert
        assertThat(DatabaseUtils.queryNumEntries(database, tableName))
                .isEqualTo(1)
    }

    @Test
    fun neverInsertLogIntoTableIfDatabaseIsNotInitialised() {
        // Act
        storage.insertLog("DEBUG", "DEBUG", genericLogMessage, null)

        // Assert
        assertThat(DatabaseUtils.queryNumEntries(database, tableName))
                .isEqualTo(0)
    }

    @Test
    fun getLogsFromDatabase() {
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
                        genericLogMessage,
                        ""
                )
        )
        assertThat(logs)
                .isEqualTo(expectedLogs)
    }

    @Test
    fun getEmptyListFromDatabaseIfNoLogsArePresent() {
        // Act
        storage.setup()

        val logs = runBlocking {
            storage.getLogs()
        }

        // Assert
        assertThat(logs)
                .isEqualTo(emptyList<RemoteLog>())
    }

    @Test
    fun deleteLogWithGivenId() {
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

    @Test
    fun deleteAllDatabaseContents() {
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

    @Test
    fun getTotalLogsPresentInDatabase() {
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
                    put(ID.value, 1)
                    put(USER_UUID.value, "Gurpreet")
                    put(TIMESTAMP.value, "123456789")
                    put(OS_NAME.value, "ANDROID")
                    put(OS_VERSION.value, 28)
                    put(APP_VERSION.value, 1)
                    put(LOG_TAG.value, "DEBUG")
                    put(LOG_LEVEL.value, "DEBUG")
                    put(MESSAGE.value, genericLogMessage)
                    put(STACKTRACE.value, "")
                }
            }

    @After
    fun teardown() {
        database.delete(tableName, null, null)
    }
}
