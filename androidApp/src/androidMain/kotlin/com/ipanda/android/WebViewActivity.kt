package com.ipanda.android

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.ipanda.domain.Constants
import java.io.ByteArrayInputStream

class WebViewActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var header: FrameLayout
    private var customViewContainer: FrameLayout? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val hideHeaderRunnable = Runnable {
        header.animate()
            .alpha(0f)
            .translationY(-header.height.toFloat())
            .setDuration(300)
            .withEndAction { header.visibility = View.GONE }
    }

    private fun showHeader() {
        handler.removeCallbacks(hideHeaderRunnable)
        header.visibility = View.VISIBLE
        header.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .start()
        handler.postDelayed(hideHeaderRunnable, 5000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable immersive full-screen mode
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Support notch/display cutout
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }

        // Root container
        val root = FrameLayout(this)

        // Header with Back Button
        header = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            )
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
        }

        val backButton = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                marginStart = dpToPx(8)
            }
            setContent {
                IconButton(onClick = { finish() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        }
        header.addView(backButton)

        // Main WebView
        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true

            webViewClient = object : WebViewClient() {
                private val blockRegex = Regex(Constants.BLOCK_REGEX, RegexOption.IGNORE_CASE)

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: ""
                    if (blockRegex.containsMatchIn(url)) {
                        return true
                    }
                    return false
                }

                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                    val url = request?.url?.toString() ?: ""
                    if (blockRegex.containsMatchIn(url)) {
                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("Blocked".toByteArray()))
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                    if (customView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    customView = view
                    customViewCallback = callback
                    customViewContainer?.addView(view)
                    customViewContainer?.visibility = View.VISIBLE
                    webView.visibility = View.GONE
                    header.visibility = View.GONE
                }

                override fun onHideCustomView() {
                    customView = null
                    customViewCallback = null
                    customViewContainer?.removeAllViews()
                    customViewContainer?.visibility = View.GONE
                    webView.visibility = View.VISIBLE
                    header.visibility = View.VISIBLE
                }
            }
            loadUrl(url)
        }

        // Fullscreen container
        customViewContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
            visibility = View.GONE
        }

        root.addView(webView)
        root.addView(header)
        root.addView(customViewContainer)
        
        // Fix for "ViewTreeLifecycleOwner not found"
        root.setViewTreeLifecycleOwner(this)
        root.setViewTreeViewModelStoreOwner(this)
        root.setViewTreeSavedStateRegistryOwner(this)
        
        // Touch to show header
        webView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                showHeader()
            }
            false // Don't consume so webview still works
        }
        
        showHeader()
        setContentView(root)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onBackPressed() {
        when {
            customView != null -> {
                customViewCallback?.onCustomViewHidden()
            }
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder().build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        if (isInPictureInPictureMode) {
            header.visibility = View.GONE
        } else {
            header.visibility = View.VISIBLE
        }
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_URL = "extra_url"

        fun launch(context: Context, url: String) {
            val intent = Intent(context, WebViewActivity::class.java)
            intent.putExtra(EXTRA_URL, url)
            context.startActivity(intent)
        }
    }
}
