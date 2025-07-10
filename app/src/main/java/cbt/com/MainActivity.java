package cbt.com;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private WebView mWebView;
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.210 Mobile Safari/537.36";
    private static final String APP_URL = "https://ct-uat.infinitisoftware.net/";

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeWebView();
    }

    private void initializeWebView() {
        mWebView = findViewById(R.id.activity_main_webview);
        WebSettings webSettings = mWebView.getSettings();

        // Essential settings only
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUserAgentString(USER_AGENT);

        // Add JavaScript interface
        mWebView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");

        // Simplified download handler
        mWebView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (url.startsWith("blob:")) {
                handleBlobUrl(url, fileName);
            } else {
                startDownload(url, userAgent, fileName, mimeType);
            }
        });

        mWebView.setWebViewClient(new WebViewClient());
        mWebView.loadUrl(APP_URL);
    }

    private void handleBlobUrl(String blobUrl, String fileName) {
        String js = "(function() {" +
                "var xhr = new XMLHttpRequest();" +
                "xhr.open('GET', '" + blobUrl + "', true);" +
                "xhr.responseType = 'blob';" +
                "xhr.onload = function() {" +
                "  if (this.status === 200) {" +
                "    var reader = new FileReader();" +
                "    reader.onload = function(e) {" +
                "      AndroidInterface.downloadFile(e.target.result, '" + fileName + "');" +
                "    };" +
                "    reader.readAsDataURL(this.response);" +
                "  }" +
                "};" +
                "xhr.send();" +
                "})()";

        mWebView.evaluateJavascript(js, null);
    }

    private void startDownload(String url, String userAgent, String fileName, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimeType);
            request.addRequestHeader("User-Agent", userAgent);
            request.setTitle(fileName);
            request.setDescription("Downloading file...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                showToast("Download started: " + fileName);
            } else {
                showToast("Download service unavailable");
            }
        } catch (Exception e) {
            showToast("Download failed: " + e.getMessage());
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void downloadFile(String base64Data, String fileName) {
            executor.execute(() -> {
                try {
                    String base64Content = base64Data.split(",")[1];
                    byte[] fileData = Base64.decode(base64Content, Base64.DEFAULT);

                    // Write file to external storage directly
                    java.io.File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    java.io.File file = new java.io.File(downloadsDir, fileName);
                    
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                    fos.write(fileData);
                    fos.close();
                    
                    mainHandler.post(() -> showToast("Downloaded: " + fileName));
                } catch (Exception e) {
                    mainHandler.post(() -> showToast("Download failed: " + e.getMessage()));
                }
            });
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".pdf")) return "application/pdf";
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
            if (fileName.endsWith(".png")) return "image/png";
            return "application/octet-stream";
        }
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(
                MainActivity.this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }
}