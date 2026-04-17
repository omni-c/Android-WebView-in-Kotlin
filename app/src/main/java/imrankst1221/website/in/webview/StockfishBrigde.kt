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
        thread(start = true, priority = Thread.MIN_PRIORITY) { // Đặt độ ưu tiên thấp nhất để không tranh giành với màn hình hiển thị
            try {
                process = ProcessBuilder(binaryPath).start()
                reader = BufferedReader(InputStreamReader(process!!.inputStream))
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
                
                // BẬT CHẾ ĐỘ THÚ DỮ NHƯNG CÓ KIỂM SOÁT
                sendUci("uci")
                // Đa số điện thoại có 8 nhân. Cho SF18 dùng 6 nhân, CHỪA LẠI 2 NHÂN để hệ điều hành vuốt chạm không bị lag.
                sendUci("setoption name Threads value 6") 
                // Bơm 256MB RAM (hoặc 512MB) để nó tính toán các biến thể cực sâu mà không phải tính lại
                sendUci("setoption name Hash value 256")   
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
        
        // BÍ QUYẾT TỐI THƯỢNG: Không dùng "go depth" nữa. Dùng "go movetime".
        // Cho nó suy nghĩ tối đa trong 400 mili-giây (0.4 giây) rồi bắt buộc trả kết quả. 
        // Với 6 nhân CPU, 400ms là đủ để SF18 nhảy vọt lên Depth 20-22 một cách dễ dàng.
        sendUci("go movetime 400") 
        
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
