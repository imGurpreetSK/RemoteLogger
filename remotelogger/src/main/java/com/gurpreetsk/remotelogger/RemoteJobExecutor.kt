package com.gurpreetsk.remotelogger

import com.gurpreetsk.remotelogger.internal.logError
import com.gurpreetsk.remotelogger.internal.logInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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
        val logs = async { storageType.getLogs() }.await()
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
      val sb = StringBuilder()
      var readLine = inputStream.readLine()
      while (readLine != null) {
        sb.append(readLine + "\n")
        readLine = inputStream.readLine()
      }

      logInfo("RemoteJobExecutor", "Server response: $sb")

      if (urlConnection.responseCode == 200) {
        purgeLogsStorage.invoke()
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
