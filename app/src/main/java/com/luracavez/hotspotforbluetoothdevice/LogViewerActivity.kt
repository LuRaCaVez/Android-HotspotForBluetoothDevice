package com.luracavez.hotspotforbluetoothdevice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

class LogViewerActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private var isReading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = androidx.core.widget.NestedScrollView(this)
        logTextView = TextView(this).apply {
            setPadding(32, 32, 32, 32)
            textSize = 12f
            typeface = android.graphics.Typeface.MONOSPACE
            setBackgroundColor(android.graphics.Color.BLACK)
            setTextColor(android.graphics.Color.GREEN)
        }
        scrollView.addView(logTextView)
        setContentView(scrollView)

        startLogReader()
    }

    private fun startLogReader() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pid = android.os.Process.myPid()
            val command = "logcat -v time --pid=$pid | grep Manager"

            try {
                val process = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                while (isReading) {
                    val line = reader.readLine()
                    if (line != null) {
                        withContext(Dispatchers.Main) {
                            logTextView.append(line + "\n")
                        }
                    }
                    yield()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    logTextView.append("Error reading logs: ${e.message}")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        isReading = false
    }
}