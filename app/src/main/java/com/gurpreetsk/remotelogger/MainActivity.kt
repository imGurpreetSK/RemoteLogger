package com.gurpreetsk.remotelogger

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.main_activity.dummyTextView
import java.lang.NullPointerException

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
  }

  override fun onStart() {
    super.onStart()
    RemoteLogger.i("MainActivity", "Info log")
    RemoteLogger.d("MainActivity", "Debug log")
    RemoteLogger.v("MainActivity", "Verbose log")
    RemoteLogger.e("MainActivity", "Error log", NullPointerException("Expected behaviour"))

    dummyTextView.text = "Logged"
  }
}
