package com.ipanda.android

import android.annotation.SuppressLint
import android.app.Activity
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
import com.ipanda.domain.Constants
import java.io.ByteArrayInputStream

class WebViewActivity : Activity() {

    private lateinit var webView: WebView
    private var customViewContainer: FrameLayout? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run {
            finish()
            return
        }

        // Root container
        val root = FrameLayout(this)

        // Header with Back Button
        val header = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(56)
            )
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
        }

        val backButton = android.widget.ImageButton(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(48),
                dpToPx(48)
            ).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                marginStart = dpToPx(8)
            }
            setImageResource(android.R.drawable.ic_menu_revert) // Simple back icon
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setColorFilter(android.graphics.Color.WHITE)
            setOnClickListener { finish() }
        }
        header.addView(backButton)

        // Main WebView
        webView = WebView(this).apply {
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            params.topMargin = dpToPx(56)
            layoutParams = params
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
