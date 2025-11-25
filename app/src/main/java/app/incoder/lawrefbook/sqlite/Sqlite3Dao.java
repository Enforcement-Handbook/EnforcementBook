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

package app.incoder.lawrefbook.sqlite;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import app.incoder.lawrefbook.storage.Category;
import app.incoder.lawrefbook.storage.Law;

/**
 * Sqlite3Dao
 *
 * @author : Jerry xu
 * @since : 2022/6/5 18:16
 */
public class Sqlite3Dao {

    private volatile SQLiteDatabase mLite;

    /**
     * 获取单例
     *
     * @return Sqlite3Dao
     */
    public static Sqlite3Dao getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * 在访问数据库时创建单例
     */
    private static class SingletonHolder {
        private static final Sqlite3Dao INSTANCE = new Sqlite3Dao();
    }

    public Sqlite3Dao() {

    }

    public SQLiteDatabase getSqlite(Context context) {
        if (mLite == null) {
            synchronized (Sqlite3Dao.class) {
                if (mLite == null) {
                    mLite = new Sqlite3Helper(context).getReadableDatabase();
                }
            }
        }
        return mLite;
    }

    public static List<Category> categoryList(Context context) {
        // 从文件系统读取分类列表
        return getCategoriesFromAssets(context);
    }

    public static List<Law> lawList(Context context, String categoryId) {
        // 从文件系统读取法律列表
        return getLawsFromAssets(context, categoryId);
    }

    /**
     * 根据父分类ID查询子分类列表（第二层分类）
     * 从文件系统读取，只显示2层，超过2层则跳过
     *
     * @param context 上下文
     * @param parentId 父分类ID（文件夹名称）
     * @return 子分类列表
     */
    public static List<Category> getSubCategories(Context context, String parentId) {
        return getSubCategoriesFromAssets(context, parentId);
    }

    /**
     * 查询所有法律数据（不按分类过滤），用于在第二层分类选择时进行过滤
     * 从文件系统读取父分类及其所有子分类下的法律文件
     *
     * @param context 上下文
     * @param parentCategoryId 父分类ID（文件夹名称）
     * @return 所有法律列表
     */
    public static List<Law> getAllLawsByParentCategory(Context context, String parentCategoryId) {
        return getAllLawsFromAssets(context, parentCategoryId);
    }

    private static List<Category> getCategory(SQLiteDatabase sqlite) {
        Cursor category = sqlite.rawQuery("SELECT * FROM category ORDER BY `order`", null);
        List<Category> result = new ArrayList<>();
        if (null != category) {
            if (category.moveToFirst()) {
                do {
                    String id = category.getString(category.getColumnIndexOrThrow("id"));
                    String name = category.getString(category.getColumnIndexOrThrow("name"));
                    String folder = category.getString(category.getColumnIndexOrThrow("folder"));
                    Integer subFolder = category.getInt(category.getColumnIndexOrThrow("isSubFolder"));
                    String group = category.getString(category.getColumnIndexOrThrow("group"));
                    Integer order = category.getInt(category.getColumnIndexOrThrow("order"));
                    Category roomCategory = Category.builder()
                            .id(id)
                            .name(name)
                            .folder(folder)
                            .isSubFolder(subFolder)
                            .group(group)
                            .order(order)
                            .build();
                    result.add(roomCategory);
                } while (category.moveToNext());
            }
            category.close();
        }
        return result;
    }

    private static List<Law> getLaw(SQLiteDatabase sqlite, String categoryIds) {
        Cursor law;
        if (categoryIds != null) {
            law = sqlite.rawQuery("SELECT * FROM law WHERE category_id = ? ORDER BY `order`", new String[]{categoryIds});
        } else {
            law = sqlite.rawQuery("SELECT * FROM law ORDER BY `order`", null);
        }
        List<Law> result = new ArrayList<>();
        if (null != law) {
            if (law.moveToFirst()) {
                do {
                    String id = law.getString(law.getColumnIndexOrThrow("id"));
                    String level = law.getString(law.getColumnIndexOrThrow("level"));
                    String name = law.getString(law.getColumnIndexOrThrow("name"));
                    String filename = law.getString(law.getColumnIndexOrThrow("filename"));
                    String publish = law.getString(law.getColumnIndexOrThrow("publish"));
                    String expired = law.getString(law.getColumnIndexOrThrow("expired"));
                    Integer categoryId = law.getInt(law.getColumnIndexOrThrow("category_id"));
                    Integer order = law.getInt(law.getColumnIndexOrThrow("order"));
                    String subtitle = law.getString(law.getColumnIndexOrThrow("subtitle"));
                    String validFrom = law.getString(law.getColumnIndexOrThrow("valid_from"));
                    Law roomLaw = Law.builder()
                            .id(id)
                            .level(level)
                            .name(name)
                            .filename(filename)
                            .publish(publish)
                            .expired(expired)
                            .categoryId(categoryId)
                            .order(order)
                            .subtitle(subtitle)
                            .validFrom(validFrom)
                            .build();
                    result.add(roomLaw);
                } while (law.moveToNext());
            }
            law.close();
        }
        return result;
    }

    /**
     * 查询子分类（第二层分类）
     * 只查询 isSubFolder = 1 且 group = parentId 的分类，如果层级超过2层则跳过
     */
    private static List<Category> getSubCategory(SQLiteDatabase sqlite, String parentId) {
        Cursor category = sqlite.rawQuery(
                "SELECT * FROM category WHERE isSubFolder = 1 AND `group` = ? ORDER BY `order`",
                new String[]{parentId});
        List<Category> result = new ArrayList<>();
        if (null != category) {
            if (category.moveToFirst()) {
                do {
                    String id = category.getString(category.getColumnIndexOrThrow("id"));
                    String name = category.getString(category.getColumnIndexOrThrow("name"));
                    String folder = category.getString(category.getColumnIndexOrThrow("folder"));
                    Integer subFolder = category.getInt(category.getColumnIndexOrThrow("isSubFolder"));
                    String group = category.getString(category.getColumnIndexOrThrow("group"));
                    Integer order = category.getInt(category.getColumnIndexOrThrow("order"));
                    
                    // 检查是否有更深层级，如果有则跳过（只显示2层）
                    Cursor checkDeepLevel = sqlite.rawQuery(
                            "SELECT COUNT(*) FROM category WHERE isSubFolder = 1 AND `group` = ?",
                            new String[]{id});
                    boolean hasDeepLevel = false;
                    if (checkDeepLevel != null && checkDeepLevel.moveToFirst()) {
                        int count = checkDeepLevel.getInt(0);
                        hasDeepLevel = count > 0;
                        checkDeepLevel.close();
                    }
                    
                    // 如果层级超过2层，则跳过
                    if (!hasDeepLevel) {
                        Category roomCategory = Category.builder()
                                .id(id)
                                .name(name)
                                .folder(folder)
                                .isSubFolder(subFolder)
                                .group(group)
                                .order(order)
                                .build();
                        result.add(roomCategory);
                    }
                } while (category.moveToNext());
            }
            category.close();
        }
        return result;
    }

    /**
     * 查询父分类下的所有法律数据（包括所有子分类的法律）
     */
    private static List<Law> getAllLaws(SQLiteDatabase sqlite, String parentCategoryId) {
        // 先获取所有子分类ID
        List<String> categoryIds = new ArrayList<>();
        categoryIds.add(parentCategoryId); // 包含父分类本身
        
        Cursor subCategories = sqlite.rawQuery(
                "SELECT id FROM category WHERE isSubFolder = 1 AND `group` = ?",
                new String[]{parentCategoryId});
        if (null != subCategories) {
            if (subCategories.moveToFirst()) {
                do {
                    String subId = subCategories.getString(subCategories.getColumnIndexOrThrow("id"));
                    categoryIds.add(subId);
                } while (subCategories.moveToNext());
            }
            subCategories.close();
        }
        
        // 构建查询条件
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM law WHERE category_id IN (");
        for (int i = 0; i < categoryIds.size(); i++) {
            if (i > 0) {
                queryBuilder.append(",");
            }
            queryBuilder.append("?");
        }
        queryBuilder.append(") ORDER BY `order`");
        
        Cursor law = sqlite.rawQuery(queryBuilder.toString(), categoryIds.toArray(new String[0]));
        List<Law> result = new ArrayList<>();
        if (null != law) {
            if (law.moveToFirst()) {
                do {
                    String id = law.getString(law.getColumnIndexOrThrow("id"));
                    String level = law.getString(law.getColumnIndexOrThrow("level"));
                    String name = law.getString(law.getColumnIndexOrThrow("name"));
                    String filename = law.getString(law.getColumnIndexOrThrow("filename"));
                    String publish = law.getString(law.getColumnIndexOrThrow("publish"));
                    String expired = law.getString(law.getColumnIndexOrThrow("expired"));
                    Integer categoryId = law.getInt(law.getColumnIndexOrThrow("category_id"));
                    Integer order = law.getInt(law.getColumnIndexOrThrow("order"));
                    String subtitle = law.getString(law.getColumnIndexOrThrow("subtitle"));
                    String validFrom = law.getString(law.getColumnIndexOrThrow("valid_from"));
                    Law roomLaw = Law.builder()
                            .id(id)
                            .level(level)
                            .name(name)
                            .filename(filename)
                            .publish(publish)
                            .expired(expired)
                            .categoryId(categoryId)
                            .order(order)
                            .subtitle(subtitle)
                            .validFrom(validFrom)
                            .build();
                    result.add(roomLaw);
                } while (law.moveToNext());
            }
            law.close();
        }
        return result;
    }

    /**
     * 从 Assets 文件夹读取第一层分类列表
     */
    private static List<Category> getCategoriesFromAssets(Context context) {
        List<Category> result = new ArrayList<>();
        try {
            AssetManager assetManager = context.getAssets();
            String[] folders = assetManager.list("Laws");
            if (folders != null) {
                List<String> folderList = new ArrayList<>(Arrays.asList(folders));
                // 按照序号排序
                folderList.sort(new NumberPrefixComparator());
                int order = 0;
                for (String folderName : folderList) {
                    // 跳过非文件夹项（如 README.md）
                    if (!folderName.contains(".")) {
                        Category category = Category.builder()
                                .id(folderName)
                                .name(folderName)
                                .folder("Laws/" + folderName)
                                .isSubFolder(0)
                                .group(null)
                                .order(order++)
                                .build();
                        result.add(category);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 从 Assets 文件夹读取第二层分类列表（子分类）
     * 只显示2层，超过2层则跳过
     */
    private static List<Category> getSubCategoriesFromAssets(Context context, String parentFolderName) {
        List<Category> result = new ArrayList<>();
        try {
            AssetManager assetManager = context.getAssets();
            String parentPath = "Laws/" + parentFolderName;
            String[] items = assetManager.list(parentPath);
            if (items != null) {
                List<String> itemList = new ArrayList<>(Arrays.asList(items));
                // 按照序号排序
                itemList.sort(new NumberPrefixComparator());
                int order = 0;
                for (String itemName : itemList) {
                    // 检查是否是文件夹
                    String itemPath = parentPath + "/" + itemName;
                    String[] subItems = assetManager.list(itemPath);
                    if (subItems != null && subItems.length > 0) {
                        // 检查是否有更深层级（第三层），如果有则跳过
                        boolean hasDeepLevel = false;
                        for (String subItem : subItems) {
                            String subItemPath = itemPath + "/" + subItem;
                            String[] deepItems = assetManager.list(subItemPath);
                            if (deepItems != null && deepItems.length > 0) {
                                // 检查是否包含文件夹（而不是文件）
                                for (String deepItem : deepItems) {
                                    String deepItemPath = subItemPath + "/" + deepItem;
                                    String[] veryDeepItems = assetManager.list(deepItemPath);
                                    if (veryDeepItems != null && veryDeepItems.length > 0) {
                                        hasDeepLevel = true;
                                        break;
                                    }
                                }
                                if (hasDeepLevel) break;
                            }
                        }
                        
                        // 如果层级超过2层，则跳过
                        if (!hasDeepLevel) {
                            Category category = Category.builder()
                                    .id(itemName)
                                    .name(itemName)
                                    .folder(parentPath + "/" + itemName)
                                    .isSubFolder(1)
                                    .group(parentFolderName)
                                    .order(order++)
                                    .build();
                            result.add(category);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 从 Assets 文件夹读取指定分类下的法律文件列表
     */
    private static List<Law> getLawsFromAssets(Context context, String categoryId) {
        List<Law> result = new ArrayList<>();
        try {
            AssetManager assetManager = context.getAssets();
            String[] items = assetManager.list("Laws/" + categoryId);
            if (items != null) {
                List<String> itemList = new ArrayList<>(Arrays.asList(items));
                // 按照序号排序
                itemList.sort(new NumberPrefixComparator());
                int order = 0;
                for (String itemName : itemList) {
                    // 检查是否是文件（有扩展名）
                    if (itemName.contains(".")) {
                        String name = itemName.substring(0, itemName.lastIndexOf("."));
                        String extension = itemName.substring(itemName.lastIndexOf(".") + 1);
                        
                        // 只处理支持的文件类型
                        if (extension.equalsIgnoreCase("docx") || 
                            extension.equalsIgnoreCase("doc") || 
                            extension.equalsIgnoreCase("wps") ||
                            extension.equalsIgnoreCase("md")) {
                            
                            // 使用 categoryId 的 hashCode 作为 categoryId，但需要确保一致性
                            int categoryIdInt = categoryId.hashCode();
                            Law law = Law.builder()
                                    .id(itemName)
                                    .name(name)
                                    .filename(itemName)
                                    .categoryId(categoryIdInt)
                                    .order(order++)
                                    .build();
                            result.add(law);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 从 Assets 文件夹读取父分类及其所有子分类下的所有法律文件
     */
    private static List<Law> getAllLawsFromAssets(Context context, String parentCategoryId) {
        List<Law> result = new ArrayList<>();
        try {
            AssetManager assetManager = context.getAssets();
            String parentPath = "Laws/" + parentCategoryId;
            
            // 先获取父分类下的直接文件
            String[] parentItems = assetManager.list(parentPath);
            if (parentItems != null) {
                List<String> parentItemList = new ArrayList<>(Arrays.asList(parentItems));
                // 按照序号排序
                parentItemList.sort(new NumberPrefixComparator());
                for (String itemName : parentItemList) {
                    if (itemName.contains(".")) {
                        String extension = itemName.substring(itemName.lastIndexOf(".") + 1);
                        if (extension.equalsIgnoreCase("docx") || 
                            extension.equalsIgnoreCase("doc") || 
                            extension.equalsIgnoreCase("wps") ||
                            extension.equalsIgnoreCase("md")) {
                            String name = itemName.substring(0, itemName.lastIndexOf("."));
                            int categoryIdInt = parentCategoryId.hashCode();
                            Law law = Law.builder()
                                    .id(itemName)
                                    .name(name)
                                    .filename(itemName)
                                    .categoryId(categoryIdInt)
                                    .order(0)
                                    .build();
                            result.add(law);
                        }
                    }
                }
            }
            
            // 获取所有子分类下的文件
            List<Category> subCategories = getSubCategoriesFromAssets(context, parentCategoryId);
            for (Category subCategory : subCategories) {
                String subPath = subCategory.getFolder();
                String[] subItems = assetManager.list(subPath);
                if (subItems != null) {
                    List<String> subItemList = new ArrayList<>(Arrays.asList(subItems));
                    // 按照序号排序
                    subItemList.sort(new NumberPrefixComparator());
                    int order = 0;
                    for (String itemName : subItemList) {
                        if (itemName.contains(".")) {
                            String extension = itemName.substring(itemName.lastIndexOf(".") + 1);
                            if (extension.equalsIgnoreCase("docx") || 
                                extension.equalsIgnoreCase("doc") || 
                                extension.equalsIgnoreCase("wps") ||
                                extension.equalsIgnoreCase("md")) {
                                String name = itemName.substring(0, itemName.lastIndexOf("."));
                                int categoryIdInt = subCategory.getId().hashCode();
                                Law law = Law.builder()
                                        .id(itemName)
                                        .name(name)
                                        .filename(itemName)
                                        .categoryId(categoryIdInt)
                                        .order(order++)
                                        .build();
                                result.add(law);
                            }
                        }
                    }
                }
            }
            
            // 按 order 排序
            result.sort((l1, l2) -> {
                if (l1.getOrder() != null && l2.getOrder() != null) {
                    return l1.getOrder().compareTo(l2.getOrder());
                }
                return 0;
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 按照文件名/文件夹名前面的序号进行排序的比较器
     * 支持中文数字（一、二、三...）和阿拉伯数字（1、2、3...）
     */
    private static class NumberPrefixComparator implements Comparator<String> {
        // 中文数字映射
        private static final String[] CHINESE_NUMBERS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十",
            "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十"
        };

        @Override
        public int compare(String s1, String s2) {
            Integer num1 = extractNumber(s1);
            Integer num2 = extractNumber(s2);
            
            // 如果都能提取到数字，按数字排序
            if (num1 != null && num2 != null) {
                int result = num1.compareTo(num2);
                if (result != 0) {
                    return result;
                }
            } else if (num1 != null) {
                return -1; // 有数字的排在前面
            } else if (num2 != null) {
                return 1; // 有数字的排在前面
            }
            
            // 如果数字相同或都没有数字，按字典序排序
            return s1.compareTo(s2);
        }

        /**
         * 从字符串中提取序号
         * 支持格式：一、二、三... 或 1. 2. 3... 或 1、2、3...
         */
        private Integer extractNumber(String str) {
            if (str == null || str.isEmpty()) {
                return null;
            }

            // 尝试提取中文数字（如：一、二、三...）
            for (int i = 0; i < CHINESE_NUMBERS.length; i++) {
                if (str.startsWith(CHINESE_NUMBERS[i] + "、") || 
                    str.startsWith(CHINESE_NUMBERS[i] + ".") ||
                    str.startsWith(CHINESE_NUMBERS[i] + " ")) {
                    return i;
                }
            }

            // 尝试提取阿拉伯数字（如：1. 2. 3... 或 1、2、3...）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+)[、.\\s]");
            java.util.regex.Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                try {
                    return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }

            return null;
        }
    }

}
