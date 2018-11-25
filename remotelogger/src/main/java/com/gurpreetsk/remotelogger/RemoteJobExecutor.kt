package com.gurpreetsk.remotelogger

import com.gurpreetsk.remotelogger.internal.logError
import com.gurpreetsk.remotelogger.internal.logInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONArray
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
      purge: () -> Int
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

    val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
    writer.write(JSONArray(logs).toString())
    writer.flush()
    writer.close()
    outputStream.close()

    urlConnection.connect()

    // 4. Get the response
    val inputStream = BufferedReader(InputStreamReader(urlConnection.inputStream))

    // Read Server Response
    val sb = StringBuilder()
    var readLine = inputStream.readLine()
    while (readLine != null) {
      sb.append(readLine + "\n")
      readLine = inputStream.readLine()
    }

    logInfo("RemoteJobExecutor", sb.toString())

    if (urlConnection.responseCode == 200) {
      purge.invoke()
    }
  }
}
