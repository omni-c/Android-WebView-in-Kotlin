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
        // Vẫn giữ mức ưu tiên thấp để không làm lag màn hình Lichess
        thread(start = true, priority = Thread.MIN_PRIORITY) { 
            try {
                process = ProcessBuilder(binaryPath).start()
                reader = BufferedReader(InputStreamReader(process!!.inputStream))
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
                
                sendUci("uci")
                // 1. ÉP KÍCH HOẠT MẠNG NƠ-RON (Nguồn gốc của 4000+ Elo)
                sendUci("setoption name Use NNUE value true")
                
                // 2. BUNG SỨC MẠNH CPU (Dùng 6 nhân, chừa 2 nhân cho điện thoại mượt)
                sendUci("setoption name Threads value 6") 
                
                // 3. BƠM 1GB RAM (1024MB) ĐỂ NHỚ CÁC BIẾN THỂ SIÊU SÂU
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
        
        // 4. CHỐT HẠ ĐỘ SÂU QUÁI VẬT: Depth 20
        // Tùy độ phức tạp của bàn cờ, nó sẽ mất từ 1.5 giây đến 3.5 giây để nhả nước đi.
        // Ở độ sâu này, nó nhìn thấu mọi cạm bẫy và đánh không có đối thủ.
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
