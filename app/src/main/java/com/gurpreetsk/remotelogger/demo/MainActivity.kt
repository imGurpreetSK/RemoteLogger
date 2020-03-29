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
package com.gurpreetsk.remotelogger.demo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gurpreetsk.remotelogger.RemoteLogger
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        addLogsButton.setOnClickListener {
            logAndUpdateDummyTextView()
        }
    }

    @SuppressLint("SetTextI18n") // Sample application.
    fun logAndUpdateDummyTextView() {
        RemoteLogger.i("MainActivity", "Info log")
        RemoteLogger.d("MainActivity", "Debug log")
        RemoteLogger.v("MainActivity", "Verbose log")
        RemoteLogger.e("MainActivity", "Error log", NullPointerException("Expected behaviour"))

        dummyTextView.text = dummyTextView.text.toString() + "Logs inserted\n"
    }
}
