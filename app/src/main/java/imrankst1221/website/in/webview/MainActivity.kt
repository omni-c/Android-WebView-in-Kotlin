package imrankst1221.website.`in`.webview

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var engineBridge: StockfishBridge

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // TẠO WEBVIEW TOÀN MÀN HÌNH - XÓA BỎ GIAO DIỆN CŨ CỦA TEMPLATE
        webView = WebView(this)
        setContentView(webView)

        // CHÉP VÀ KÍCH HOẠT STOCKFISH NATIVE
        engineBridge = StockfishBridge()
        val binaryPath = filesDir.absolutePath + "/stockfish"
        copyStockfishToStorage(binaryPath)
        engineBridge.startEngine(binaryPath)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        
        webView.addJavascriptInterface(engineBridge, "AndroidBot")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                injectBotScript()
            }
        }
        webView.loadUrl("https://lichess.org/")
    }

    private fun copyStockfishToStorage(destPath: String) {
        try {
            val destFile = File(destPath)
            if (!destFile.exists()) {
                assets.open("stockfish").use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                destFile.setExecutable(true)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun injectBotScript() {
        val js = """
            javascript:(function() {
                let lastFen = null;
                function isMyTurn() { return document.body.innerText.includes('Đến lượt bạn') || (document.querySelector('cg-board') && document.querySelector('cg-board').classList.contains('manipulable')); }
                function drawArrow(move) {
                    let board = document.querySelector('cg-board'); if(!board) return;
                    let svg = document.getElementById('sf-svg');
                    if(!svg) { svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg'); svg.id='sf-svg'; svg.style.cssText='position:absolute;top:0;left:0;width:100%;height:100%;pointer-events:none;z-index:9999;'; board.appendChild(svg); }
                    if(!move || !isMyTurn()) { svg.innerHTML=''; return; }
                    if(move==='e1h1') move='e1g1'; if(move==='e1a1') move='e1c1'; if(move==='e8h8') move='e8g8'; if(move==='e8a8') move='e8c8';
                    let wrap = document.querySelector('.cg-wrap'); let isBlackOri = wrap && wrap.classList.contains('orientation-black');
                    let files = 'abcdefgh'; let f1 = files.indexOf(move[0]), r1 = 8 - parseInt(move[1]); let f2 = files.indexOf(move[2]), r2 = 8 - parseInt(move[3]);
                    if(isBlackOri) { f1 = 7-f1; r1 = 7-r1; f2 = 7-f2; r2 = 7-r2; }
                    let sqW = 100/8;
                    svg.innerHTML = '<defs><marker id="ah" markerWidth="4" markerHeight="5" refX="2.5" refY="2.5" orient="auto"><polygon points="0 0.5, 4 2.5, 0 4.5" fill="#4f803c" stroke="#000" stroke-width="0.6" stroke-linejoin="round"/></marker></defs>' +
                    '<line x1="'+(f1*sqW+sqW/2)+'%" y1="'+(r1*sqW+sqW/2)+'%" x2="'+(f2*sqW+sqW/2)+'%" y2="'+(r2*sqW+sqW/2)+'%" stroke="#000" stroke-width="3.5%" stroke-linecap="butt"/>' +
                    '<line x1="'+(f1*sqW+sqW/2)+'%" y1="'+(r1*sqW+sqW/2)+'%" x2="'+(f2*sqW+sqW/2)+'%" y2="'+(r2*sqW+sqW/2)+'%" stroke="#4f803c" stroke-width="2.2%" stroke-linecap="butt" marker-end="url(#ah)"/>';
                }
                setInterval(() => {
                    drawArrow(window.currentMove);
                    if(!isMyTurn()) { lastFen = null; window.currentMove = null; return; }
                    let b = document.querySelector('cg-board'); let w = document.querySelector('.cg-wrap');
                    if(!b || !w) return;
                    let isB = w.classList.contains('orientation-black'); let ps = b.querySelectorAll('piece');
                    let arr = Array(8).fill(null).map(()=>Array(8).fill(null)); let sq = b.clientWidth/8;
                    ps.forEach(p => { let m = p.style.transform.match(/translate(?:3d)?\(([-.\d]+)(px|%)[,\s]+([-.\d]+)(px|%)/); if(m){ let x=Math.round(parseFloat(m[1])/(m[2]==='%'?12.5:sq)); let y=Math.round(parseFloat(m[3])/(m[4]==='%'?12.5:sq)); if(isB){x=7-x; y=7-y;} let c=p.classList.contains('knight')?'N':p.classList.contains('bishop')?'B':p.classList.contains('rook')?'R':p.classList.contains('queen')?'Q':p.classList.contains('king')?'K':'P'; if(p.classList.contains('black')) c=c.toLowerCase(); if(y>=0&&y<8&&x>=0&&x<8) arr[y][x]=c; } });
                    let fen = ''; for(let r=0;r<8;r++){let e=0;for(let c=0;c<8;c++){if(arr[r][c]){if(e>0){fen+=e;e=0;}fen+=arr[r][c];}else e++;}if(e>0)fen+=e;if(r<7)fen+='/';}
                    fen += (isB?' b':' w') + ' KQkq - 0 1';
                    if(fen !== lastFen) {
                        lastFen = fen;
                        let move = window.AndroidBot.calculateMove(fen);
                        if(move && move !== 'none') { window.currentMove = move; }
                    }
                }, 150);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}
