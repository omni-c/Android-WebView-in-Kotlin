package imrankst1221.website.`in`.webview

import android.webkit.JavascriptInterface
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

class StockfishBridge {
    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null

    fun startEngine(binaryPath: String) {
        thread(start = true, priority = Thread.MIN_PRIORITY) {
            try {
                process = ProcessBuilder(binaryPath).start()
                reader = BufferedReader(InputStreamReader(process!!.inputStream))
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
                
                sendUci("uci")
                sendUci("setoption name Use NNUE value true")
                sendUci("setoption name Threads value 6") 
                sendUci("setoption name Hash value 1024")   
                sendUci("isready")
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun sendUci(cmd: String) {
        writer?.write(cmd + "\n")
        writer?.flush()
    }

    @JavascriptInterface
    fun calculateMove(fen: String): String {
        sendUci("position fen $fen")
        sendUci("go depth 20") 
        
        var line: String?
        try {
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
