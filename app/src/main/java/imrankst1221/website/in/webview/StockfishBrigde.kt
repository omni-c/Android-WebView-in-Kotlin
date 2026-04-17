package com.mac.isaac.webview

import android.webkit.JavascriptInterface
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class StockfishBridge {
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    fun startEngine(binaryPath: String) {
        try {
            process = ProcessBuilder(binaryPath).start()
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
        } catch (e: Exception) { e.printStackTrace() }
    }

    @JavascriptInterface
    fun calculateMove(fen: String): String {
        try {
            writer?.write("position fen $fen\n")
            writer?.write("go depth 15\n")
            writer?.flush()
            var line: String?
            while (reader?.readLine().also { line = it } != null) {
                if (line!!.startsWith("bestmove")) {
                    val parts = line!!.split(" ")
                    if (parts.size > 1 && parts[1] != "(none)") return parts[1]
                    return "none"
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return "none"
    }
}
