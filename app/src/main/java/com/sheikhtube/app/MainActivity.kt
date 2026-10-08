package com.sheikhtube.app

import android.app.*
import android.app.PictureInPictureParams
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.webkit.*
import android.widget.*
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var progress: ProgressBar
    private lateinit var settingsButton: ImageButton
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private var updateDownloadId: Long = -1L
    private var audioMode = false
    private lateinit var audioPanel: LinearLayout
    private var pipPrepared = false

    private val homeUrl = "https://m.youtube.com/"
    private val releaseApi = "https://api.github.com/repos/ytprm5984-create/SheikhTube/releases/latest"
    private val blockedHosts = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "scorecardresearch.com", "taboola.com",
        "outbrain.com", "adnxs.com", "criteo.com", "quantserve.com",
        "amazon-adsystem.com", "adsrvr.org", "rubiconproject.com", "pubmatic.com", "openx.net",
        "amazon-adsystem.com", "adsrvr.org", "rubiconproject.com",
        "pubmatic.com", "openx.net", "casalemedia.com", "moatads.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        web = findViewById(R.id.web)
        progress = findViewById(R.id.progress)
        settingsButton = findViewById(R.id.settings)

        createAudioPanel()
        configureWebView()
        settingsButton.setOnClickListener { showSettings() }
        findViewById<ImageButton>(R.id.audio).setOnClickListener { toggleAudioMode() }

        if (savedInstanceState == null) web.loadUrl(homeUrl) else web.restoreState(savedInstanceState)
        checkForUpdates(false)
    }

    private fun configureWebView() {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
        web.setBackgroundColor(Color.BLACK)
        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
            userAgentString = userAgentString.replace("; wv", "")
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val host = request?.url?.host.orEmpty().lowercase()
                if (blockedHosts.any { host == it || host.endsWith(".$it") }) {
                    return WebResourceResponse("text/plain", "utf-8", null)
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host.orEmpty().lowercase()
                return if (host.endsWith("youtube.com") || host == "youtu.be" || host.endsWith("googlevideo.com")) {
                    false
                } else {
                    try { startActivity(Intent(Intent.ACTION_VIEW, uri)); true } catch (_: Exception) { false }
                }
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null || view == null) { callback?.onCustomViewHidden(); return }
                customView = view
                customCallback = callback
                val decor = window.decorView as ViewGroup
                decor.addView(view, ViewGroup.LayoutParams(-1, -1))
                web.visibility = View.GONE
                settingsButton.visibility = View.GONE
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }

            override fun onHideCustomView() { hideCustomView() }
        }
    }

    private fun hideCustomView() {
        val view = customView ?: return
        (window.decorView as ViewGroup).removeView(view)
        customView = null
        customCallback?.onCustomViewHidden()
        customCallback = null
        web.visibility = View.VISIBLE
        settingsButton.visibility = View.VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    private fun videoJs(code: String) {
        web.evaluateJavascript("(function(){var v=document.querySelector('video');if(v){" + code + "}})();", null)
    }

    private fun createAudioPanel() {
        audioPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(24), dp(22), dp(24))
            setBackgroundColor(Color.rgb(13, 15, 20))
            visibility = View.GONE
        }
        audioPanel.addView(text("🎧  SHEIKH TUBE AUDIO", 24, Color.WHITE, true))
        audioPanel.addView(text("\nAudio-focused controls\nPlayback depends on the website", 15, Color.LTGRAY, false))
        audioPanel.addView(button("⏮  Previous") { web.evaluateJavascript("history.back()", null) })
        audioPanel.addView(button("⏯  Play / Pause") { videoJs("if(v.paused){v.play()}else{v.pause()}") })
        audioPanel.addView(button("⏭  Next") { web.evaluateJavascript("history.forward()", null) })
        audioPanel.addView(button("▶  Return to Video") { toggleAudioMode() })
        findViewById<ViewGroup>(R.id.root).addView(audioPanel, ViewGroup.LayoutParams(-1, -1))
    }

    private fun toggleAudioMode() {
        audioMode = !audioMode
        audioPanel.visibility = if (audioMode) View.VISIBLE else View.GONE
        settingsButton.visibility = if (audioMode) View.GONE else View.VISIBLE
        findViewById<ImageButton>(R.id.audio).visibility = if (audioMode) View.GONE else View.VISIBLE
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode || audioMode) return
        web.evaluateJavascript("(function(){var v=document.querySelector('video');return !!(v&&!v.paused&&!v.ended)})()") { playing ->
            if (playing == "true") {
                // Preserve the player in its original DOM. Enlarge its rendering surface for PiP.
                pipPrepared = true
                web.evaluateJavascript("(function(){var v=document.querySelector('video');if(!v)return;v.dataset.stOldStyle=v.getAttribute('style')||'';v.style.cssText+=';position:fixed!important;top:0!important;left:0!important;width:100vw!important;height:100vh!important;object-fit:contain!important;background:black!important;z-index:2147483647!important';})()", null)
                settingsButton.visibility = View.GONE
                findViewById<ImageButton>(R.id.audio).visibility = View.GONE
                Handler(Looper.getMainLooper()).postDelayed({
                    try { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16,9)).build()) }
                    catch (_: Exception) { restorePip() }
                }, 220)
            }
        }
    }

    private fun restorePip() {
        if (pipPrepared) {
            web.evaluateJavascript("(function(){var v=document.querySelector('video');if(v){v.setAttribute('style',v.dataset.stOldStyle||'');delete v.dataset.stOldStyle}})()", null)
            pipPrepared = false
        }
        settingsButton.visibility = View.VISIBLE
        findViewById<ImageButton>(R.id.audio).visibility = View.VISIBLE
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) restorePip()
    }

    private fun showSettings() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(12))
            setBackgroundColor(Color.rgb(12, 15, 17))
        }
        val photo = ImageView(this).apply {
            setImageResource(R.drawable.developer_photo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(-1, dp(210))
        }
        container.addView(photo)
        container.addView(text("DEVELOPER", 13, Color.rgb(0, 200, 83), true))
        container.addView(text("Sheikh Sojib", 28, Color.WHITE, true))
        container.addView(text("Creator of Sheikh Tube", 16, Color.LTGRAY, false))
        container.addView(text("\nWhatsApp  •  01823315984", 18, Color.WHITE, true).apply {
            setPadding(0, dp(10), 0, dp(10)); setOnClickListener { openWhatsApp() }
        })
        container.addView(button("💬  Contact Developer") { openWhatsApp() })
        container.addView(text("\nSheikh Tube  •  Version ${BuildConfig.VERSION_NAME}", 17, Color.WHITE, true))
        container.addView(button("Check for Updates") { checkForUpdates(true) })
        container.addView(button("What's New") { showWhatsNew() })
        container.addView(button("Clear Cache") {
            web.clearCache(true); Toast.makeText(this, "Cache cleared", Toast.LENGTH_SHORT).show()
        })
        container.addView(text("\nAd & tracker protection: Always ON\nPiP: Enabled when supported\nAudio controls: Available\nAuto Update: Always ON", 14, Color.LTGRAY, false))

        AlertDialog.Builder(this).setTitle("About & Developer").setView(container)
            .setNegativeButton("Close", null).show()
    }

    private fun text(s: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = s; textSize = size.toFloat(); setTextColor(color); gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { action() }
    }

    private fun openWhatsApp() {
        val uri = Uri.parse("https://wa.me/8801823315984")
        try { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        catch (_: Exception) { Toast.makeText(this, "WhatsApp could not be opened", Toast.LENGTH_SHORT).show() }
    }

    private fun showWhatsNew() {
        AlertDialog.Builder(this).setTitle("What's New in Sheikh Tube V2.1.1")
            .setMessage("• Clean YouTube-first interface\n• URL/GO bar removed\n• Fullscreen video improvements\n• Picture-in-Picture support\n• Always-on ad/tracker host protection\n• Popup protection\n• Automatic GitHub update checks\n• Branded update screen\n• Developer card & WhatsApp contact\n• Loading and stability improvements")
            .setPositiveButton("OK", null).show()
    }

    private fun checkForUpdates(manual: Boolean) {
        thread {
            try {
                val c = URL(releaseApi).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                c.setRequestProperty("User-Agent", "SheikhTube-Android")
                val body = c.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.optString("tag_name").removePrefix("v")
                val notes = json.optString("body", "New improvements and fixes.")
                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", true)) { apkUrl = a.optString("browser_download_url"); break }
                }
                val newer = compareVersions(tag, BuildConfig.VERSION_NAME) > 0
                runOnUiThread {
                    if (newer && !apkUrl.isNullOrBlank()) showUpdateDialog(tag, notes, apkUrl!!)
                    else if (manual) Toast.makeText(this, "Sheikh Tube is up to date", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                if (manual) runOnUiThread { Toast.makeText(this, "Could not check for updates", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun compareVersions(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }; val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = (x.getOrElse(i){0}).compareTo(y.getOrElse(i){0}); if (d != 0) return d
        }
        return 0
    }

    private fun showUpdateDialog(version: String, notes: String, apkUrl: String) {
        AlertDialog.Builder(this)
            .setIcon(R.drawable.developer_photo)
            .setTitle("Sheikh Tube Update Available")
            .setMessage("New version v$version is ready 🎉\n\nWHAT'S NEW\n${notes.take(900)}")
            .setPositiveButton("UPDATE NOW") { _, _ -> downloadUpdate(apkUrl, version) }
            .setNegativeButton("LATER", null).show()
    }

    private fun downloadUpdate(url: String, version: String) {
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val fileName = "SheikhTube-v$version.apk"
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("Sheikh Tube v$version")
            .setDescription("Downloading update…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
        updateDownloadId = dm.enqueue(req)
        Toast.makeText(this, "Update downloading…", Toast.LENGTH_LONG).show()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == updateDownloadId) {
                    try { unregisterReceiver(this) } catch (_: Exception) {}
                    installDownloadedApk(fileName)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_NOT_EXPORTED)
        else @Suppress("DEPRECATION") registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installDownloadedApk(fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Allow Sheikh Tube to install updates, then tap Update Now again.", Toast.LENGTH_LONG).show()
            return
        }
        val file = File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(i)
    }

    override fun onSaveInstanceState(outState: Bundle) { web.saveState(outState); super.onSaveInstanceState(outState) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> hideCustomView()
            web.canGoBack() -> web.goBack()
            else -> super.onBackPressed()
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
