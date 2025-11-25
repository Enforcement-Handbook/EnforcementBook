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

package app.incoder.lawrefbook;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import app.incoder.lawrefbook.model.Article;
import app.incoder.lawrefbook.model.Content;
import app.incoder.lawrefbook.model.Extended;
import app.incoder.lawrefbook.model.Lawre;
import app.incoder.lawrefbook.model.Toc;
import app.incoder.lawrefbook.model.Type;
import app.incoder.lawrefbook.ui.content.DocumentViewActivity;

/**
 * LawRefBookRepository
 *
 * @author : Jerry xu
 * @since : 2022/4/30 09:06
 */
public class LawRefBookRepository {

    public static String getContext(String fileName, Context context) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open(fileName), StandardCharsets.UTF_8));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    public static List<Lawre> getData(String category, Context context) {
        String json = getContext("Laws/data.json", context);
        JSONArray jsonArray;
        List<Lawre> jetPackModelList = new ArrayList<>();
        try {
            JSONObject keyBean = new JSONObject(json);
            jsonArray = (JSONArray) keyBean.get(category);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                Lawre bean = new Lawre();
                bean.setCategory(object.getString("category"));
                bean.setFolder(object.getString("folder"));
                bean.setId(object.getString("id"));
                bean.setGroup(object.getString("group"));
                bean.setIsSubFolder(object.getBoolean("isSubFolder"));
                JSONArray laws = object.getJSONArray("laws");
                List<Lawre.LawsBean> lawsBeanList = new ArrayList<>(laws.length());
                for (int j = 0; j < laws.length(); j++) {
                    JSONObject lawsBeanObject = laws.getJSONObject(i);
                    Lawre.LawsBean lawsBean = new Lawre.LawsBean();
                    lawsBean.setLevel(lawsBeanObject.getString("level"));
                    lawsBean.setName(lawsBeanObject.getString("name"));
                    lawsBean.setId(lawsBeanObject.getString("id"));
                    lawsBeanList.add(lawsBean);
                }
                bean.setLaws(lawsBeanList);
                JSONArray links = object.getJSONArray("links");
                List<String> stringList = new ArrayList<>(links.length());
                for (int k = 0; k < links.length(); k++) {
                    String string = links.getString(k);
                    stringList.add(string);
                }
                bean.setLinks(stringList);
                jetPackModelList.add(bean);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jetPackModelList;
    }

    public static List<Lawre> getData(Context context) {
        String json = getContext("Laws/data.json", context);
        List<Lawre> jetPackModelList = new ArrayList<>();
        try {
            JSONArray keyBean = new JSONArray(json);
            for (int i = 0; i < keyBean.length(); i++) {
                JSONObject object = keyBean.getJSONObject(i);
                Lawre bean = new Lawre();
                bean.setCategory(object.getString("category"));
                bean.setFolder(object.getString("folder"));
                bean.setId(object.getString("id"));
                if (!object.isNull("group")) {
                    bean.setGroup(object.getString("group"));
                }
                if (!object.isNull("isSubFolder")) {
                    bean.setIsSubFolder(object.getBoolean("isSubFolder"));
                }
                if (!object.isNull("laws")) {
                    JSONArray laws = object.getJSONArray("laws");
                    List<Lawre.LawsBean> lawsBeanList = new ArrayList<>(laws.length());
                    for (int j = 0; j < laws.length(); j++) {
                        JSONObject lawsBeanObject = laws.getJSONObject(j);
                        Lawre.LawsBean lawsBean = new Lawre.LawsBean();
                        lawsBean.setLevel(lawsBeanObject.getString("level"));
                        lawsBean.setName(lawsBeanObject.getString("name"));
                        if (!lawsBeanObject.isNull("filename")) {
                            lawsBean.setFilename(lawsBeanObject.getString("filename"));
                        }
                        lawsBean.setId(lawsBeanObject.getString("id"));
                        lawsBeanList.add(lawsBean);
                    }
                    bean.setLaws(lawsBeanList);
                }
                if (!object.isNull("links")) {
                    JSONArray links = object.getJSONArray("links");
                    List<String> stringList = new ArrayList<>(links.length());
                    for (int k = 0; k < links.length(); k++) {
                        String string = links.getString(k);
                        stringList.add(string);
                    }
                    bean.setLinks(stringList);
                }
                jetPackModelList.add(bean);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jetPackModelList;
    }

    public static List<String> getCatalog(Context context) {
        AssetManager assetManager = context.getAssets();
        List<String> catalogList = new ArrayList<>();
        try {
            String[] laws = assetManager.list("Laws");
            List<String> list = Arrays.asList(laws);
            for (int i = 0; i < list.size(); i++) {
                String name = list.get(i);
                if (name.matches("[\u4E00-\u9FA5]+")) {
                    catalogList.add(name);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return catalogList;
    }

    public static Article getArticle(Context context, String path) {
        Article article = new Article();
        Extended extended = new Extended();
        List<Toc> tocList = new ArrayList<>();
        List<String> history = new ArrayList<>();
        List<Content> articleContent = new ArrayList<>();
        int count = 0;
        try {
            AssetManager assetManager = context.getAssets();
            BufferedReader bf = new BufferedReader(new InputStreamReader(assetManager.open(path), StandardCharsets.UTF_8));
            String line;
            boolean body = false;
            int tocIndex = 0;
            int tocId = 0;
            // key = realLevel, value = parentId
            Map<Integer, Integer> indexMap = new HashMap<>(8);
            // 记录上一次标题的 parentId，初始化默认没有，即定义为 -1
            Integer compareLevelParentId = -1;
            StringBuilder sb = new StringBuilder();
            while ((line = bf.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
//                if ("<!-- INFO END -->".equals(line)) {
//                    body = true;
//                    continue;
//                }
                Content content = new Content();
                if (true) {
                    if (line.matches("^#+")) {
                        // 行内容 ## 的标题，丢弃
                        continue;
                    }
                    if (line.matches("^#+ .*")) {
                        tocIndex++;
                        tocId++;
                        String headline = line.replaceAll("^#+ ", "");
                        int level = line.split("#").length;
                        Toc toc = new Toc();
                        toc.setId(tocId);
                        int realLevel = level - 2;
                        Integer currentLevelParentId = indexMap.get(realLevel);
                        // 利用 map 特性记录最新 top 的 level 和 id
                        if (!compareLevelParentId.equals(currentLevelParentId)) {
                            // 第一层级，清除记录
                            if (realLevel == 1) {
                                indexMap.clear();
                            }
                            if (currentLevelParentId == null || compareLevelParentId < currentLevelParentId) {
                                indexMap.put(realLevel, tocId - 1);
                                compareLevelParentId = tocId - 1;
                            } else {
                                indexMap.put(realLevel, currentLevelParentId);
                                compareLevelParentId = currentLevelParentId;
                            }
                        }
                        Integer realLevelParentId = indexMap.get(realLevel);
                        toc.setParentId(realLevelParentId != null ? realLevelParentId : -1);
                        toc.setPosition(tocIndex);
                        toc.setTitle(headline);
                        toc.setTitleLevel(realLevel);
                        tocList.add(toc);
                        if (headline.matches("^(第[一二三四五六七八九十零百千万]).*?") && realLevel == 1) {
                            content.setType(Type.SECTION_TYPE.getCode());
                        } else if (realLevel > 1) {
                            content.setType(Type.NODE_TYPE.getCode());
                        }
                        content.setRule(headline);
                        articleContent.add(content);
                    } else {
                        if (line.matches("^$")) {
                            // empty line
                            continue;
                        } else if (line.matches("(第[一二三四五六七八九十零百千万]*条)( *)([\\s\\S]*)")) {
                            content.setType(Type.CONTENT_TYPE.getCode());
                            if (sb.length() > 0) {
                                content.setRule(sb.toString());
                                tocIndex++;
                                articleContent.add(content);
                                // clear sb
                                sb.delete(0, sb.length());
                            }
                            sb.append(line);
                        } else {
                            sb.append("\n").append(line);
                        }
                        // 预解析，查看是否为最后一行内容
                        if (bf.readLine() == null) {
                            Content lastContent = new Content();
                            lastContent.setType(Type.CONTENT_TYPE.getCode());
                            lastContent.setRule(sb.toString());
                            tocIndex++;
                            articleContent.add(lastContent);
                        }
                    }
                } else {
                    if (line.matches("^#+ .*")) {
                        if (article.getTitle() != null && !article.getTitle().trim().isEmpty()) {
                            String temp = line.replaceAll("^#+ ", "");
                            article.setTitle(article.getTitle() + temp);
                        } else {
                            article.setTitle(line.replaceAll("^#+ ", ""));
                        }
                    } else {
                        history.add(line);
                    }
                }
                count = count + line.length();
            }
            extended.setWordsCount(count + "");
            extended.setCorrectHistory(history);
            article.setContents(articleContent);
            article.setToc(tocList);
            article.setInfo(extended);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return article;
    }

    /**
     * 在 WebView 中加载文件内容
     * 用于在 ContentActivity 中显示非 MD 文件（DOCX、DOC、WPS 等）
     */
    public static void loadFileInWebView(Context context, WebView webView, String path, String fileType, String title) {
        try {
            String extension = fileType != null ? fileType.toLowerCase() : "";
            switch (extension) {
                case "docx": {
                    // DOCX 文件：使用 Apache POI 解析并转换为 HTML
                    String html = convertDocxToHtml(context, path, title);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                    break;
                }
                case "doc": {
                    // DOC 文件：使用 Apache POI 解析并转换为 HTML
                    String html = convertDocToHtml(context, path, title);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                    break;
                }
                case "wps":
                    // WPS 文件：尝试作为 DOC 处理，或显示提示
                    try {
                        String html = convertDocToHtml(context, path, title);
                        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                    } catch (Exception e) {
                        String html = generateDocumentHtml(title, extension,
                                "WPS 文件格式暂不支持直接预览。\n文件路径: " + path);
                        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                    }
                    break;
                default: {
                    // 其他格式：显示提示信息
                    String html = generateDocumentHtml(title, extension,
                            "此文件格式暂不支持在应用内预览。\n文件路径: " + path);
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showErrorHtml(webView, title, "加载文件失败: " + e.getMessage());
        }
    }

    /**
     * 将 DOCX 文件转换为 HTML
     * DOCX 文件本质上是 ZIP 文件，包含 XML 格式的文档内容
     */
    private static String convertDocxToHtml(Context context, String path, String title) {
        ZipInputStream zipInputStream = null;
        try {
            File tempFile = copyAssetToTempFile(context, path);
            if (tempFile == null || !tempFile.exists()) {
                return generateDocumentHtml(title, "docx", "文件不存在或无法读取");
            }

            zipInputStream = new ZipInputStream(new FileInputStream(tempFile));
            ZipEntry entry;
            StringBuilder textBuilder = new StringBuilder();

            // 遍历 ZIP 文件中的条目，查找 word/document.xml
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().equals("word/document.xml")) {
                    // 读取 document.xml 并提取文本
                    textBuilder.append(extractTextFromDocxXml(zipInputStream));
                    break;
                }
                zipInputStream.closeEntry();
            }

            String text = textBuilder.toString();
            if (text.isEmpty()) {
                return generateDocumentHtml(title, "docx", "无法从 DOCX 文件中提取文本内容");
            }

            // 将文本转换为 HTML
            return convertTextToHtml(title, "DOCX", text);
        } catch (Exception e) {
            e.printStackTrace();
            return generateDocumentHtml(title, "docx", "解析 DOCX 文件失败: " + e.getMessage());
        } finally {
            try {
                if (zipInputStream != null) zipInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从 DOCX 的 document.xml 中提取文本内容
     */
    private static String extractTextFromDocxXml(ZipInputStream zipInputStream) {
        StringBuilder textBuilder = new StringBuilder();
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));

            int eventType = parser.getEventType();
            boolean inTextElement = false;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    // DOCX 中的文本通常在 <w:t> 标签中
                    if ("t".equals(tagName) || "w:t".equals(tagName)) {
                        inTextElement = true;
                    }
                } else if (eventType == XmlPullParser.TEXT && inTextElement) {
                    String text = parser.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        textBuilder.append(text);
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    String tagName = parser.getName();
                    if ("t".equals(tagName) || "w:t".equals(tagName)) {
                        inTextElement = false;
                        // 段落结束，添加换行
                        textBuilder.append(" ");
                    } else if ("p".equals(tagName) || "w:p".equals(tagName)) {
                        // 段落结束，添加换行
                        textBuilder.append("\n");
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果 XML 解析失败，尝试简单的文本提取
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zipInputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    // 简单的文本提取：移除 XML 标签
                    String text = line.replaceAll("<[^>]+>", " ").trim();
                    if (!text.isEmpty()) {
                        textBuilder.append(text).append(" ");
                    }
                }
                reader.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return textBuilder.toString();
    }

    /**
     * 将 DOC 文件转换为 HTML
     * DOC 文件是二进制格式，解析比较复杂
     * 这里使用简单的文本提取方法（可能不完整，但可以提取部分文本）
     */
    private static String convertDocToHtml(Context context, String path, String title) {
        FileInputStream fis = null;
        try {
            File tempFile = copyAssetToTempFile(context, path);
            if (tempFile == null || !tempFile.exists()) {
                return generateDocumentHtml(title, "doc", "文件不存在或无法读取");
            }

            // DOC 文件是二进制格式，尝试提取可读的文本内容
            fis = new FileInputStream(tempFile);
            StringBuilder textBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // 尝试从二进制数据中提取可打印的 ASCII 字符
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];
                    // 提取可打印的 ASCII 字符（32-126）和常见的中文字符范围
                    if ((b >= 32 && b <= 126) || (b < 0)) {
                        // 对于多字节字符，需要更复杂的处理
                        // 这里使用简单的 ASCII 提取
                        if (b >= 32 && b <= 126) {
                            textBuilder.append((char) b);
                        }
                    } else if (b == 10 || b == 13) {
                        // 保留换行符
                        textBuilder.append("\n");
                    } else {
                        textBuilder.append(" ");
                    }
                }
            }

            String text = textBuilder.toString();
            // 清理文本：移除过多的连续空格和特殊字符
            text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            if (text.isEmpty() || text.length() < 10) {
                return generateDocumentHtml(title, "doc",
                        "无法从 DOC 文件中提取有效文本内容。\n" +
                                "DOC 文件是二进制格式，需要专门的库来解析。\n" +
                                "建议将 DOC 文件转换为 DOCX 格式后再查看。");
            }

            // 将文本转换为 HTML
            return convertTextToHtml(title, "DOC", text);
        } catch (Exception e) {
            e.printStackTrace();
            return generateDocumentHtml(title, "doc",
                    "解析 DOC 文件失败: " + e.getMessage() + "\n" +
                            "DOC 文件是二进制格式，解析较为复杂。\n" +
                            "建议将 DOC 文件转换为 DOCX 格式后再查看。");
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将纯文本转换为格式化的 HTML
     */
    private static String convertTextToHtml(String title, String fileType, String text) {
        if (text == null || text.trim().isEmpty()) {
            return generateDocumentHtml(title, fileType, "文件内容为空");
        }

        // 转义 HTML 特殊字符（需要先转义 &，避免影响其他替换）
        text = text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\r\n", "<br>")
                .replace("\n", "<br>")
                .replace("\r", "<br>");

        // 将多个连续的 <br> 合并为段落
        text = text.replaceAll("(<br>\\s*){3,}", "<p></p>");
        // 将单个 <br> 替换为段落分隔
        text = "<p>" + text.replaceAll("<br>", "</p><p>") + "</p>";
        // 清理空段落
        text = text.replaceAll("<p>\\s*</p>", "");

        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body{font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, 'Microsoft YaHei', sans-serif; padding: 20px; line-height: 1.8; color: #333; max-width: 900px; margin: 0 auto; background: #fff;}" +
                "h1{color: #1976d2; margin-bottom: 20px; border-bottom: 2px solid #1976d2; padding-bottom: 10px; font-size: 24px;}" +
                "p{color: #333; margin: 12px 0; text-align: justify; font-size: 16px;}" +
                ".file-info{background: #f5f5f5; padding: 12px; border-radius: 5px; margin-bottom: 20px; font-size: 14px; color: #666;}" +
                "@media (prefers-color-scheme: dark) {" +
                "body{background: #121212; color: #e0e0e0;}" +
                "h1{color: #90caf9; border-bottom-color: #90caf9;}" +
                "p{color: #e0e0e0;}" +
                ".file-info{background: #1e1e1e; color: #b0b0b0;}" +
                "}" +
                "</style></head><body>" +
                "<h1>" + escapeHtml(title != null ? title : "文档") + "</h1>" +
                "<div class='file-info'>文件类型: " + fileType + "</div>" +
                "<div>" + text + "</div>" +
                "</body></html>";
    }

    /**
     * HTML 转义工具方法
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 生成文档显示的 HTML
     */
    private static String generateDocumentHtml(String title, String fileType, String message) {
        return "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body{font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif; padding: 20px; line-height: 1.6; color: #333;}" +
                "h2{color: #1976d2; margin-bottom: 10px;}" +
                "p{color: #666; margin: 10px 0;}" +
                ".info{background: #f5f5f5; padding: 15px; border-radius: 5px; margin: 15px 0;}" +
                "</style></head><body>" +
                "<h2>" + (title != null ? title : "文档") + "</h2>" +
                "<div class='info'>" +
                "<p><strong>文件类型:</strong> " + (fileType != null ? fileType.toUpperCase() : "未知") + "</p>" +
                "<p>" + message + "</p>" +
                "</div>" +
                "</body></html>";
    }

    /**
     * 显示错误信息的 HTML
     */
    private static void showErrorHtml(WebView webView, String title, String errorMessage) {
        String html = generateDocumentHtml(title, null, errorMessage);
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    /**
     * 打开非 MD 文件（docx, wps, pdf 等）
     * 使用简单的方式：对于 PDF 使用 WebView，对于其他格式使用 Intent 打开外部应用
     * 注意：此方法已废弃，现在所有文件都通过 ContentActivity 打开
     */
    @Deprecated
    public static void openFile(Context context, String path, String title, String fileExtension) {
        try {
            // 将文件从 Assets 复制到临时目录
            File tempFile = copyAssetToTempFile(context, path);
            if (tempFile == null) {
                Toast.makeText(context, "无法打开文件", Toast.LENGTH_SHORT).show();
                return;
            }

            String extension = fileExtension.toLowerCase();
            if (extension.equals("pdf")) {
                // PDF 文件：使用 WebView 在应用内显示
                Intent intent = new Intent(context, DocumentViewActivity.class);
                intent.putExtra(DocumentViewActivity.FILE_PATH, tempFile.getAbsolutePath());
                intent.putExtra(DocumentViewActivity.FILE_TITLE, title);
                intent.putExtra(DocumentViewActivity.FILE_TYPE, "pdf");
                context.startActivity(intent);
            } else if (extension.equals("docx") || extension.equals("doc") || extension.equals("wps")) {
                // DOCX/WPS 文件：使用 Intent 打开外部应用，或者使用 WebView 显示提示
                // 方案1：使用 Intent 打开外部应用
                try {
                    Uri fileUri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", tempFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, getMimeType(extension));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(Intent.createChooser(intent, "选择应用打开文件"));
                } catch (Exception e) {
                    // 如果没有外部应用可以打开，使用 WebView 显示提示
                    Intent intent = new Intent(context, DocumentViewActivity.class);
                    intent.putExtra(DocumentViewActivity.FILE_PATH, tempFile.getAbsolutePath());
                    intent.putExtra(DocumentViewActivity.FILE_TITLE, title);
                    intent.putExtra(DocumentViewActivity.FILE_TYPE, extension);
                    context.startActivity(intent);
                }
            } else {
                // 其他格式：尝试使用 Intent 打开
                try {
                    Uri fileUri = FileProvider.getUriForFile(context,
                            context.getPackageName() + ".fileprovider", tempFile);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, getMimeType(extension));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(Intent.createChooser(intent, "选择应用打开文件"));
                } catch (Exception e) {
                    Toast.makeText(context, "无法打开此类型的文件", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "打开文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将 Assets 中的文件复制到临时目录
     */
    private static File copyAssetToTempFile(Context context, String assetPath) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open(assetPath);

            // 创建临时文件
            File tempDir = new File(context.getCacheDir(), "temp_files");
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }

            String fileName = assetPath.substring(assetPath.lastIndexOf("/") + 1);
            File tempFile = new File(tempDir, fileName);

            OutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取文件的 MIME 类型
     */
    private static String getMimeType(String extension) {
        switch (extension.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":
                return "application/msword";
            case "wps":
                return "application/vnd.ms-works";
            default:
                return "application/octet-stream";
        }
    }

}