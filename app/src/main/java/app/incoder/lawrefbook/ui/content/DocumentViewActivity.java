/*
 * Copyright (C) 2022 The Jerry xu Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.incoder.lawrefbook.ui.content;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

import app.incoder.lawrefbook.R;

/**
 * DocumentViewActivity
 * 用于显示 PDF、DOCX、WPS 等文档文件
 * 
 * @author : Jerry xu
 * @since : 2024/12/19
 */
public class DocumentViewActivity extends AppCompatActivity {

    public static final String FILE_PATH = "file_path";
    public static final String FILE_TITLE = "file_title";
    public static final String FILE_TYPE = "file_type";

    private WebView mWebView;
    private String mFilePath;
    private String mFileTitle;
    private String mFileType;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mFilePath = getIntent().getStringExtra(FILE_PATH);
        mFileTitle = getIntent().getStringExtra(FILE_TITLE);
        mFileType = getIntent().getStringExtra(FILE_TYPE);

        if (mFileTitle != null) {
            setTitle(mFileTitle);
        }

        mWebView = findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.setWebViewClient(new WebViewClient());

        loadDocument();
    }

    private void loadDocument() {
        if (mFilePath == null || !new File(mFilePath).exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if ("pdf".equalsIgnoreCase(mFileType)) {
            // PDF 文件：尝试使用 WebView 加载
            // 注意：WebView 对 PDF 的支持有限，可能需要使用 PDF 库
            // 这里使用简单的 file:// 协议加载（某些 Android 版本可能不支持）
            try {
                String fileUrl = "file://" + mFilePath;
                mWebView.loadUrl(fileUrl);
            } catch (Exception e) {
                // 如果加载失败，显示提示信息
                String html = "<html><head><meta charset='UTF-8'><style>" +
                        "body{font-family: Arial; padding: 20px; text-align: center;}" +
                        "h2{color: #333;}" +
                        "p{color: #666; line-height: 1.6;}" +
                        "</style></head><body>" +
                        "<h2>PDF 文档</h2>" +
                        "<p>文件名: " + (mFileTitle != null ? mFileTitle : "未知") + "</p>" +
                        "<p>提示: PDF 文件需要使用外部应用打开。</p>" +
                        "<p>文件路径: " + mFilePath + "</p>" +
                        "</body></html>";
                mWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            }
        } else {
            // DOCX/WPS 等其他格式：显示提示信息
            String html = "<html><head><meta charset='UTF-8'><style>" +
                    "body{font-family: Arial; padding: 20px; text-align: center;}" +
                    "h2{color: #333;}" +
                    "p{color: #666; line-height: 1.6;}" +
                    "</style></head><body>" +
                    "<h2>文档预览</h2>" +
                    "<p>文件类型: " + (mFileType != null ? mFileType.toUpperCase() : "未知") + "</p>" +
                    "<p>文件名: " + (mFileTitle != null ? mFileTitle : "未知") + "</p>" +
                    "<p>提示: 此文件格式暂不支持在应用内预览，请使用外部应用打开。</p>" +
                    "</body></html>";
            mWebView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
        }
        super.onDestroy();
    }
}

