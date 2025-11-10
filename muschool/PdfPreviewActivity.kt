package com.example.muschool

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PDFPreviewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_preview)

        webView = findViewById(R.id.webViewPDF)
        progress = findViewById(R.id.progressPDF)

        val title = intent.getStringExtra("title") ?: "PDF Viewer"
        val url = intent.getStringExtra("url") ?: ""

        title?.let { supportActionBar?.title = it }

        if (url.isBlank()) {
            Toast.makeText(this, "Invalid PDF URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Normalize and embed PDF
        val finalUrl = preparePdfUrl(url)

        webView.settings.javaScriptEnabled = true
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = android.view.View.GONE
            }
        }

        progress.visibility = android.view.View.VISIBLE
        webView.loadUrl(finalUrl)
    }

    private fun preparePdfUrl(originalUrl: String): String {
        return when {
            // If it's a Google Drive share link
            originalUrl.contains("drive.google.com", ignoreCase = true) -> {
                val fileId = extractDriveFileId(originalUrl)
                if (fileId != null)
                    "https://drive.google.com/file/d/$fileId/preview"
                else originalUrl
            }
            // If it's a Cloudinary or direct link
            originalUrl.contains(".pdf", ignoreCase = true) -> {
                "https://docs.google.com/gview?embedded=true&url=$originalUrl"
            }
            else -> {
                "https://docs.google.com/gview?embedded=true&url=$originalUrl"
            }
        }
    }

    private fun extractDriveFileId(link: String): String? {
        val regex = Regex("[-\\w]{25,}")
        return regex.find(link)?.value
    }
}
