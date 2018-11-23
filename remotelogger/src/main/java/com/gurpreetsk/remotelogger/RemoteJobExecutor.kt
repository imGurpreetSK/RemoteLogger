package com.gurpreetsk.remotelogger

import com.gurpreetsk.remotelogger.internal.logError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class RemoteJobExecutor {
  fun execute(
      url: String,
      storageType: RemoteLogsStorage
  ) {
    try {
      GlobalScope.launch {
        makeRequest(url, async { storageType.getLogs() }.await())
            .also { storageType.purge() }
      }
    } catch (e: Exception) {
      logError("RemoteJobExecutor", "Remote network call failed.", e)
    }
  }

  private fun makeRequest(
      remoteUrl: String,
      logs: List<RemoteLog>
  ) {
    if (remoteUrl.isBlank()) {
      throw MalformedURLException("Blank url")
    }

    // 1. Declare a URL Connection
    val url  = URL(remoteUrl)
    val urlConnection = url.openConnection() as HttpURLConnection

    // 2. Open InputStream to connection
    urlConnection.connect()

    // 3. Download and decode the string response using builder
    val outputStream = BufferedOutputStream(urlConnection.outputStream)

    val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
    writer.write(JSONArray(logs).toString())
    writer.flush()
    writer.close()
    outputStream.close()

    urlConnection.connect()
  }
}
