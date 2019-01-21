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
import com.gurpreetsk.remotelogger.internal.logError
import com.gurpreetsk.remotelogger.internal.logInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

object RemoteJobExecutor {
  fun execute(
      url: String,
      storageType: RemoteLogsStorage
  ) {
    try {
      GlobalScope.launch {
        val logs: List<RemoteLog> = withContext(Dispatchers.Default) { storageType.getLogs() }
        if (logs.isNotEmpty()) {
          pushLogsToServer(url, logs) { storageType.purge() }
        }
      }
    } catch (e: Exception) {
      logError("RemoteJobExecutor", "Remote network call failed.", e)
    }
  }

  @Throws(MalformedURLException::class, IOException::class)
  private fun pushLogsToServer(
      remoteUrl: String,
      logs: List<RemoteLog>,
      purgeLogsStorage: () -> Int
  ) {
    if (remoteUrl.isBlank()) {
      throw MalformedURLException()
    }

    // 1. Declare a URL Connection
    val url = URL(remoteUrl)
    val urlConnection = url.openConnection() as HttpURLConnection
    urlConnection.requestMethod = "POST"

    // 2. Open InputStream to connection
    urlConnection.connect()

    // 3. Make the POST request
    val outputStream = BufferedOutputStream(urlConnection.outputStream)
    val requestBody  = getRequestBodyJson(logs).toString()

    val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
    writer.write(requestBody)
    writer.flush()
    writer.close()
    outputStream.close()

    urlConnection.connect()
    logInfo("RemoteJobExecutor", "Request body to server: $requestBody")

    // 4. Get the response
    try {
      val inputStream = BufferedReader(InputStreamReader(urlConnection.inputStream))

      // Read Server Response
      val builder = StringBuilder()
      var readLine = inputStream.readLine()
      while (readLine != null) {
        builder.append(readLine + "\n")
        readLine = inputStream.readLine()
      }

      logInfo("RemoteJobExecutor", "Server response: $builder")

      if (urlConnection.responseCode == 200) {
        purgeLogsStorage()
      } else {
        inputStream.close()
      }
    } catch (e: Exception) {
      logError("RemoteJobExecutor", "Error getting the server response", e)
    }
  }

  private fun getRequestBodyJson(logs: List<RemoteLog>): JSONObject {
    val jsonArray = JSONArray()
    logs.forEach {
      jsonArray.put(
          with (JSONObject()) {
            put("id", it.id)
            put("userUUID", it.userUUID)
            put("osName", it.osName)
            put("osVersion", it.osVersion)
            put("appVersion", it.appVersion)
            put("logTag", it.logTag)
            put("logLevel", it.logLevel)
            put("message", it.message)
            put("stacktrace", it.stackTrace)
          }
      )
    }

    val logsJson = JSONObject()
    logsJson.put("logs", jsonArray)

    return logsJson
  }
}
