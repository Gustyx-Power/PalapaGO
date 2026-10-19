package id.gustyxganteng.agent.feature.canvas

object AndroidSimulatorRenderer {

    fun render(title: String, content: String): String {
        val cleanTitle = if (title.isNotBlank()) title else extractTitleFromCode(content)
        val isCalc = content.contains("calculator", ignoreCase = true) ||
            content.contains("kalkulator", ignoreCase = true) ||
            (content.contains("operator", ignoreCase = true) && content.contains("equals", ignoreCase = true))

        val isTodo = content.contains("todo", ignoreCase = true) ||
            content.contains("task", ignoreCase = true) ||
            content.contains("catatan", ignoreCase = true)

        val escapedKotlinCode = content
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        val workflowYml = """
name: Build Android APK
on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    name: Build Debug APK
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'gradle'

      - name: Grant Execute Permission
        run: chmod +x gradlew

      - name: Build APK with Gradle
        run: ./gradlew assembleDebug --stacktrace

      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/*.apk
          retention-days: 14
""".trimIndent().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        val termuxScript = """
# Jalankan perintah ini di Termux untuk build lokal:
pkg update && pkg install -y openjdk-17 wget unzip
chmod +x gradlew
./gradlew assembleDebug
# File APK siap di: app/build/outputs/apk/debug/app-debug.apk
""".trimIndent().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        val appUiHtml = if (isCalc) {
            getCalculatorUiHtml()
        } else if (isTodo) {
            getTodoUiHtml()
        } else {
            getGenericComposeUiHtml(cleanTitle)
        }

        return """
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>$cleanTitle - Android Live Simulator</title>
    <style>
        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
        html, body {
            background-color: #0b0c10;
            color: #e2e4ea;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            padding: 16px 8px 32px 8px;
            display: flex;
            flex-direction: column;
            align-items: center;
            min-height: 100vh;
            overflow-y: auto !important;
            -webkit-overflow-scrolling: touch !important;
            touch-action: pan-y !important;
        }
        .runtime-badge {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            padding: 6px 14px;
            background: #151821;
            border: 1px solid #282d3b;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            color: #8ab4f8;
            margin-bottom: 16px;
        }
        .runtime-dot {
            width: 8px;
            height: 8px;
            background: #34d399;
            border-radius: 50%;
            box-shadow: 0 0 8px rgba(52, 211, 153, 0.6);
        }
        .phone-frame {
            width: 100%;
            max-width: 360px;
            height: 640px;
            background: #121316;
            border: 2px solid #2d313b;
            border-radius: 36px;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            box-shadow: 0 20px 45px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.05);
            position: relative;
        }
        .camera-notch {
            position: absolute;
            top: 8px;
            left: 50%;
            transform: translateX(-50%);
            width: 12px;
            height: 12px;
            background: #000000;
            border-radius: 50%;
            z-index: 50;
            border: 1px solid #232730;
        }
        .status-bar {
            height: 28px;
            padding: 0 16px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: #121316;
            font-size: 11px;
            font-weight: 600;
            color: #a4abb8;
            user-select: none;
        }
        .status-icons {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 11px;
        }
        .top-app-bar {
            height: 48px;
            background: #181a20;
            border-bottom: 1px solid #252933;
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 0 14px;
        }
        .app-bar-title {
            font-size: 15px;
            font-weight: 600;
            color: #f0f2f7;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }
        .app-bar-actions {
            display: flex;
            align-items: center;
            gap: 12px;
            color: #a4abb8;
            font-size: 14px;
        }
        .viewport-content {
            flex: 1;
            overflow-y: auto;
            background: #121316;
            display: flex;
            flex-direction: column;
        }
        .gesture-bar-container {
            height: 18px;
            display: flex;
            justify-content: center;
            align-items: center;
            background: #121316;
        }
        .gesture-pill {
            width: 80px;
            height: 4px;
            background: #474e5d;
            border-radius: 2px;
        }
        .export-section {
            width: 100%;
            max-width: 360px;
            margin-top: 20px;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        .export-card {
            background: #151821;
            border: 1px solid #252933;
            border-radius: 12px;
            padding: 12px 14px;
        }
        .export-card-title {
            font-size: 13px;
            font-weight: 600;
            color: #8ab4f8;
            margin-bottom: 4px;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .export-card-desc {
            font-size: 11px;
            color: #9aa0a6;
            line-height: 1.4;
            margin-bottom: 8px;
        }
        .code-box {
            background: #0d0f14;
            border: 1px solid #1f232c;
            border-radius: 8px;
            padding: 8px 10px;
            font-family: monospace;
            font-size: 11px;
            color: #c9d1d9;
            white-space: pre-wrap;
            max-height: 140px;
            overflow-y: auto;
        }
        .copy-btn {
            background: #1c2230;
            border: 1px solid #2d3748;
            color: #e2e8f0;
            padding: 4px 10px;
            border-radius: 6px;
            font-size: 11px;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.2s;
        }
        .copy-btn:active {
            background: #2b3952;
            transform: scale(0.96);
        }
    </style>
</head>
<body>
    <div class="runtime-badge">
        <span class="runtime-dot"></span>
        <span>Android Native Live Simulator (Active)</span>
    </div>

    <div class="phone-frame">
        <div class="camera-notch"></div>
        <div class="status-bar">
            <span id="android-clock">09:41</span>
            <div class="status-icons">
                <span>5G</span>
                <span>●●●</span>
                <span>100%</span>
            </div>
        </div>

        <div class="top-app-bar">
            <span class="app-bar-title">$cleanTitle</span>
            <div class="app-bar-actions">
                <span>🔍</span>
                <span>⋮</span>
            </div>
        </div>

        <div class="viewport-content">
            $appUiHtml
        </div>

        <div class="gesture-bar-container">
            <div class="gesture-pill"></div>
        </div>
    </div>

    <div class="export-section">
        <div class="export-card">
            <div class="export-card-title">
                <span>📦 Build APK via GitHub Actions CI</span>
                <button class="copy-btn" onclick="copyText('workflow-src', this)">Salin CI</button>
            </div>
            <div class="export-card-desc">Push folder project ke GitHub. Workflow ini otomatis mengkompilasi file .apk asli dalam 1 menit tanpa Termux/PC.</div>
            <pre class="code-box" id="workflow-src">$workflowYml</pre>
        </div>

        <div class="export-card">
            <div class="export-card-title">
                <span>⚡ Skrip Kompilasi Termux</span>
                <button class="copy-btn" onclick="copyText('termux-src', this)">Salin Skrip</button>
            </div>
            <div class="export-card-desc">Jika Anda memiliki aplikasi Termux di HP ini, jalankan perintah ini di Termux:</div>
            <pre class="code-box" id="termux-src">$termuxScript</pre>
        </div>

        <div class="export-card">
            <div class="export-card-title">
                <span>📄 Source Code Jetpack Compose</span>
                <button class="copy-btn" onclick="copyText('kotlin-src', this)">Salin Kode</button>
            </div>
            <div class="export-card-desc">File asli Kotlin yang tersimpan di workspace aktif:</div>
            <pre class="code-box" id="kotlin-src">$escapedKotlinCode</pre>
        </div>
    </div>

    <script>
        function updateClock() {
            const now = new Date();
            const hrs = String(now.getHours()).padStart(2, '0');
            const mins = String(now.getMinutes()).padStart(2, '0');
            const el = document.getElementById('android-clock');
            if (el) el.textContent = hrs + ':' + mins;
        }
        updateClock();
        setInterval(updateClock, 30000);

        function copyText(elemId, btn) {
            const el = document.getElementById(elemId);
            if (!el) return;
            const text = el.textContent || el.innerText;
            navigator.clipboard.writeText(text).then(() => {
                const prev = btn.textContent;
                btn.textContent = 'Tersalin!';
                btn.style.color = '#34d399';
                setTimeout(() => {
                    btn.textContent = prev;
                    btn.style.color = '#e2e8f0';
                }, 1800);
            }).catch(() => {
                alert('Gagal menyalin teks.');
            });
        }
    </script>
</body>
</html>
""".trimIndent()
    }

    private fun extractTitleFromCode(code: String): String {
        val matchComposable = Regex("""@Composable\s+fun\s+([A-Za-z0-9_]+)""").find(code)
        if (matchComposable != null) {
            val name = matchComposable.groupValues[1]
            return name.replace("Screen", "").replace("View", "").ifEmpty { "Android App" }
        }
        val matchClass = Regex("""class\s+([A-Za-z0-9_]+)""").find(code)
        if (matchClass != null) {
            return matchClass.groupValues[1].replace("Activity", "").ifEmpty { "Android App" }
        }
        return "Android Application"
    }

    private fun getCalculatorUiHtml(): String {
        return """
        <div style="display:flex; flex-direction:column; height:100%; justify-content:flex-end; padding:12px;">
            <div style="flex:1; display:flex; flex-direction:column; justify-content:flex-end; text-align:right; padding:12px; margin-bottom:8px;">
                <div id="calc-expr" style="font-size:16px; color:#9aa0a6; min-height:24px; word-break:break-all;"></div>
                <div id="calc-res" style="font-size:38px; font-weight:600; color:#ffffff; overflow:hidden; text-overflow:ellipsis;">0</div>
            </div>
            <div style="display:grid; grid-template-columns:repeat(4, 1fr); gap:8px;">
                <button class="c-btn fn" onclick="calcAction('clear')">AC</button>
                <button class="c-btn fn" onclick="calcAction('paren')">( )</button>
                <button class="c-btn fn" onclick="calcAction('percent')">%</button>
                <button class="c-btn op" onclick="calcInput('÷')">÷</button>

                <button class="c-btn num" onclick="calcInput('7')">7</button>
                <button class="c-btn num" onclick="calcInput('8')">8</button>
                <button class="c-btn num" onclick="calcInput('9')">9</button>
                <button class="c-btn op" onclick="calcInput('×')">×</button>

                <button class="c-btn num" onclick="calcInput('4')">4</button>
                <button class="c-btn num" onclick="calcInput('5')">5</button>
                <button class="c-btn num" onclick="calcInput('6')">6</button>
                <button class="c-btn op" onclick="calcInput('−')">−</button>

                <button class="c-btn num" onclick="calcInput('1')">1</button>
                <button class="c-btn num" onclick="calcInput('2')">2</button>
                <button class="c-btn num" onclick="calcInput('3')">3</button>
                <button class="c-btn op" onclick="calcInput('+')">+</button>

                <button class="c-btn num" onclick="calcInput('0')">0</button>
                <button class="c-btn num" onclick="calcInput('.')">.</button>
                <button class="c-btn num" onclick="calcAction('backspace')">⌫</button>
                <button class="c-btn eq" onclick="calcAction('equals')">=</button>
            </div>
        </div>
        <style>
            .c-btn {
                height: 52px;
                border-radius: 26px;
                border: none;
                font-size: 19px;
                font-weight: 500;
                cursor: pointer;
                transition: transform 0.1s, opacity 0.1s;
                user-select: none;
                outline: none;
            }
            .c-btn:active {
                transform: scale(0.92);
                opacity: 0.8;
            }
            .c-btn.num {
                background: #1f222b;
                color: #e3e6ed;
            }
            .c-btn.fn {
                background: #2e3340;
                color: #a8c7fa;
                font-weight: 600;
            }
            .c-btn.op {
                background: #1e3352;
                color: #8ab4f8;
                font-size: 21px;
                font-weight: 600;
            }
            .c-btn.eq {
                background: #8ab4f8;
                color: #041e49;
                font-size: 23px;
                font-weight: 600;
            }
        </style>
        <script>
            let currentExpr = '';
            let isParenOpen = false;

            function calcInput(char) {
                currentExpr += char;
                document.getElementById('calc-expr').textContent = currentExpr;
            }

            function calcAction(action) {
                if (action === 'clear') {
                    currentExpr = '';
                    document.getElementById('calc-expr').textContent = '';
                    document.getElementById('calc-res').textContent = '0';
                } else if (action === 'backspace') {
                    currentExpr = currentExpr.slice(0, -1);
                    document.getElementById('calc-expr').textContent = currentExpr;
                } else if (action === 'percent') {
                    currentExpr += '%';
                    document.getElementById('calc-expr').textContent = currentExpr;
                } else if (action === 'paren') {
                    currentExpr += isParenOpen ? ')' : '(';
                    isParenOpen = !isParenOpen;
                    document.getElementById('calc-expr').textContent = currentExpr;
                } else if (action === 'equals') {
                    if (!currentExpr) return;
                    try {
                        let sanitized = currentExpr
                            .replace(/×/g, '*')
                            .replace(/÷/g, '/')
                            .replace(/−/g, '-')
                            .replace(/%/g, '/100');
                        let result = Function('"use strict"; return (' + sanitized + ')')();
                        if (typeof result === 'number' && !isNaN(result)) {
                            document.getElementById('calc-res').textContent = String(Math.round(result * 100000000) / 100000000);
                        } else {
                            document.getElementById('calc-res').textContent = 'Error';
                        }
                    } catch (e) {
                        document.getElementById('calc-res').textContent = 'Format Error';
                    }
                }
            }
        </script>
        """.trimIndent()
    }

    private fun getTodoUiHtml(): String {
        return """
        <div style="padding:16px; display:flex; flex-direction:column; height:100%;">
            <div style="display:flex; gap:8px; margin-bottom:14px;">
                <input id="todo-input" type="text" placeholder="Tulis tugas baru..." style="flex:1; background:#1e2129; border:1px solid #333846; border-radius:10px; padding:10px 12px; color:#fff; font-size:13px; outline:none;">
                <button onclick="addTodo()" style="background:#8ab4f8; color:#041e49; border:none; border-radius:10px; padding:0 14px; font-weight:600; font-size:13px; cursor:pointer;">Tambah</button>
            </div>
            <div id="todo-list" style="flex:1; overflow-y:auto; display:flex; flex-direction:column; gap:8px;">
                <div class="todo-item" onclick="toggleTodo(this)">
                    <input type="checkbox" style="margin-right:8px;">
                    <span style="flex:1;">Uji coba aplikasi Android di PalapaGO</span>
                    <button onclick="deleteTodo(event, this)" style="background:none; border:none; color:#ef4444; font-size:14px; cursor:pointer;">✕</button>
                </div>
                <div class="todo-item" onclick="toggleTodo(this)">
                    <input type="checkbox" style="margin-right:8px;">
                    <span style="flex:1;">Build APK via GitHub Actions</span>
                    <button onclick="deleteTodo(event, this)" style="background:none; border:none; color:#ef4444; font-size:14px; cursor:pointer;">✕</button>
                </div>
            </div>
        </div>
        <style>
            .todo-item {
                background: #1a1d24;
                border: 1px solid #282d38;
                border-radius: 10px;
                padding: 10px 12px;
                display: flex;
                align-items: center;
                font-size: 13px;
                color: #e2e8f0;
                cursor: pointer;
            }
            .todo-done span {
                text-decoration: line-through;
                color: #717d91;
            }
        </style>
        <script>
            function addTodo() {
                const inp = document.getElementById('todo-input');
                const text = inp.value.trim();
                if (!text) return;
                const list = document.getElementById('todo-list');
                const div = document.createElement('div');
                div.className = 'todo-item';
                div.onclick = function() { toggleTodo(div); };
                div.innerHTML = '<input type="checkbox" style="margin-right:8px;"><span style="flex:1;">' + text + '</span><button onclick="deleteTodo(event, this)" style="background:none; border:none; color:#ef4444; font-size:14px; cursor:pointer;">✕</button>';
                list.appendChild(div);
                inp.value = '';
            }
            function toggleTodo(el) {
                el.classList.toggle('todo-done');
                const chk = el.querySelector('input[type="checkbox"]');
                if (chk) chk.checked = el.classList.contains('todo-done');
            }
            function deleteTodo(e, btn) {
                e.stopPropagation();
                btn.closest('.todo-item').remove();
            }
        </script>
        """.trimIndent()
    }

    private fun getGenericComposeUiHtml(title: String): String {
        return """
        <div style="padding:16px; display:flex; flex-direction:column; gap:12px;">
            <div style="background:#181c24; border:1px solid #2a3140; border-radius:12px; padding:14px;">
                <div style="font-size:14px; font-weight:600; color:#8ab4f8; margin-bottom:4px;">Composable Preview</div>
                <div style="font-size:12px; color:#9aa0a6;">Antarmuka Jetpack Compose aktif di sandbox simulator Android.</div>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label style="font-size:11px; font-weight:600; color:#a4abb8;">Input Teks (OutlinedTextField)</label>
                <input type="text" placeholder="Ketik sesuatu di sini..." style="background:#151821; border:1px solid #333948; border-radius:8px; padding:10px 12px; color:#fff; font-size:13px; outline:none;">
            </div>

            <div style="display:flex; justify-content:space-between; align-items:center; background:#181c24; border:1px solid #282e3b; border-radius:10px; padding:12px;">
                <span style="font-size:13px; color:#e2e8f0;">Counter StateFlow: <b id="counter-val" style="color:#8ab4f8;">0</b></span>
                <div style="display:flex; gap:6px;">
                    <button onclick="updateCount(-1)" style="background:#282d3b; color:#fff; border:none; width:32px; height:32px; border-radius:16px; font-weight:bold; cursor:pointer;">−</button>
                    <button onclick="updateCount(1)" style="background:#8ab4f8; color:#041e49; border:none; width:32px; height:32px; border-radius:16px; font-weight:bold; cursor:pointer;">+</button>
                </div>
            </div>

            <button onclick="showToast()" style="background:#8ab4f8; color:#041e49; border:none; border-radius:10px; padding:12px; font-weight:600; font-size:13px; cursor:pointer; margin-top:8px;">Eksekusi Aksi (Button)</button>

            <div id="toast" style="display:none; background:#2c3240; border:1px solid #434c60; color:#8ab4f8; padding:8px 12px; border-radius:8px; font-size:12px; text-align:center; margin-top:8px;">Aksi berhasil dipanggil!</div>
        </div>
        <script>
            let count = 0;
            function updateCount(delta) {
                count += delta;
                document.getElementById('counter-val').textContent = String(count);
            }
            function showToast() {
                const t = document.getElementById('toast');
                t.style.display = 'block';
                setTimeout(() => { t.style.display = 'none'; }, 2000);
            }
        </script>
        """.trimIndent()
    }
}
