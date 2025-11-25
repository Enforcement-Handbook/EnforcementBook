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

package app.incoder.lawrefbook.ui.feed;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Arrays;
import java.util.List;

import app.incoder.lawrefbook.LawRefBookRepository;
import app.incoder.lawrefbook.R;
import app.incoder.lawrefbook.model.Article;
import app.incoder.lawrefbook.storage.Category;
import app.incoder.lawrefbook.storage.Law;
import app.incoder.lawrefbook.ui.content.ContentActivity;

/**
 * FeedAdapter
 *
 * @author : Jerry xu
 * @since : 2022/5/1 10:53
 */
public class FeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Law> mLaw;
    private final Context mContext;
    private Category mCategory;
    private List<Category> mSubCategories;
    private String mSelectedSubCategoryId;
    private OnSubCategorySelectedListener mSubCategoryListener;
    private final List<String> ignorePublish = Arrays.asList("刑法", "宪法", "案例/劳动人事", "案例/民法典", "案例/消费购物", "案例/行政协议诉讼", "民法典");

    /**
     * 根据 categoryId 获取对应的 folder 路径
     * categoryId 是分类 ID 的 hashCode
     */
    private String getFolderByCategoryId(Integer categoryId) {
        if (categoryId == null) {
            return mCategory != null ? mCategory.getFolder() : "";
        }

        // 先检查是否是父分类
        if (mCategory != null && mCategory.getId().hashCode() == categoryId) {
            return mCategory.getFolder();
        }

        // 检查是否是子分类
        if (mSubCategories != null) {
            for (Category subCategory : mSubCategories) {
                if (subCategory.getId().hashCode() == categoryId) {
                    return subCategory.getFolder();
                }
            }
        }

        // 如果找不到，返回父分类的 folder
        return mCategory != null ? mCategory.getFolder() : "";
    }

    public static final int VIEW_TYPE_HEADER = 2;
    public static final int VIEW_TYPE_ITEM = 1;
    public static final int VIEW_TYPE_EMPTY = 0;

    /**
     * 子分类选择监听器
     */
    public interface OnSubCategorySelectedListener {
        void onSubCategorySelected(String subCategoryId);
    }

    public void setSubCategorySelectedListener(OnSubCategorySelectedListener listener) {
        this.mSubCategoryListener = listener;
    }

    public void setData(Category category, List<Law> data) {
        mLaw = data;
        this.mCategory = category;
        notifyDataSetChanged();
    }

    /**
     * 设置子分类列表
     */
    public void setSubCategories(List<Category> subCategories) {
        this.mSubCategories = subCategories;
        // 默认选中第一个子分类
        if (subCategories != null && !subCategories.isEmpty()) {
            mSelectedSubCategoryId = subCategories.get(0).getId();
        }
        notifyDataSetChanged();
    }

    /**
     * 设置选中的子分类ID
     */
    public void setSelectedSubCategoryId(String subCategoryId) {
        this.mSelectedSubCategoryId = subCategoryId;
        notifyItemChanged(0); // 更新 Header
    }

    public FeedAdapter(Context context) {
        this.mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_HEADER) {
            view = inflater.inflate(R.layout.header_subcategory_selector, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_EMPTY) {
            view = inflater.inflate(R.layout.empty_view, parent, false);
            return new FeedViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.item_feed, parent, false);
            return new FeedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            bindHeaderViewHolder((HeaderViewHolder) holder);
        } else if (holder instanceof FeedViewHolder) {
            bindFeedViewHolder((FeedViewHolder) holder, position);
        }
    }

    private void bindHeaderViewHolder(HeaderViewHolder holder) {
        if (mSubCategories == null || mSubCategories.isEmpty()) {
            holder.chipGroup.setVisibility(View.GONE);
            return;
        }

        holder.chipGroup.setVisibility(View.VISIBLE);
        holder.chipGroup.removeAllViews();

        // 使用 ContextThemeWrapper 应用 Material Design Chip Choice 样式
        // 获取 Material Components Chip Choice 样式资源 ID
        int chipStyleResId = com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice;
        ContextThemeWrapper themedContext = new ContextThemeWrapper(mContext, chipStyleResId);

        for (Category subCategory : mSubCategories) {
            // 使用带样式的 Context 创建 Chip
            // Chip 构造函数: Chip(Context context, AttributeSet attrs, int defStyleAttr)
            Chip chip = new Chip(themedContext, null, chipStyleResId);
            chip.setText(subCategory.getName());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setChecked(subCategory.getId().equals(mSelectedSubCategoryId));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked && mSubCategoryListener != null) {
                    mSelectedSubCategoryId = subCategory.getId();
                    mSubCategoryListener.onSubCategorySelected(subCategory.getId());
                }
            });

            holder.chipGroup.addView(chip);
        }
    }

    private void bindFeedViewHolder(FeedViewHolder holder, int position) {
        // 如果有 Header，需要调整 position
        int actualPosition = hasHeader() ? position - 1 : position;

        if (mLaw != null && !mLaw.isEmpty() && actualPosition >= 0 && actualPosition < mLaw.size()) {
            Law law = mLaw.get(actualPosition);
            holder.mTitle.setText(law.getName());
            String path = getString(law);
            holder.itemView.setOnClickListener(v -> {
                // 检查文件类型
                String fileExtension = getFileExtensionFromPath(path);

                Intent intent = new Intent(mContext, ContentActivity.class)
                        .putExtra(ContentActivity.Path, path)
                        .putExtra(ContentActivity.Folder, mCategory.getFolder())
                        .putExtra(ContentActivity.ArticleId, law.getId())
                        .putExtra(ContentActivity.Title, law.getName())
                        .putExtra(ContentActivity.FileType, fileExtension);

                if (fileExtension.equalsIgnoreCase("md")) {
                    // MD 文件：解析内容
                    Article article = LawRefBookRepository.getArticle(v.getContext(), path);
                    if (article == null) {
                        Toast.makeText(v.getContext(), v.getContext().getResources().getString(R.string.unable_to_parse), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    intent.putExtra(ContentActivity.Article, article);
                } else {
                    // 其他文件类型：传递 null Article，ContentActivity 会使用 WebView 显示
                    intent.putExtra(ContentActivity.Article, (Article) null);
                }

                mContext.startActivity(intent);
            });
        }
    }

    private boolean hasHeader() {
        return mSubCategories != null && !mSubCategories.isEmpty();
    }

    private String getString(Law law) {
        String path;
        // 根据 law 的 categoryId 获取对应的 folder
        String folder = getFolderByCategoryId(law.getCategoryId());

        // 获取文件扩展名（从 filename 或 name 中提取）
        String extension = getFileExtension(law);

        if (law.getFilename() == null) {
            if (ignorePublish.contains(folder)) {
                path = folder + "/" + law.getName() + extension;
            } else {
                if (law.getPublish() == null) {
                    path = folder + "/" + law.getName() + extension;
                } else {
                    path = folder + "/" + law.getName() + "(" + law.getPublish() + ")" + extension;
                }
            }
        } else {
            // filename 已经包含扩展名，直接使用
            path = folder + "/" + law.getFilename();
        }
        return path;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(Law law) {
        if (law.getFilename() != null && law.getFilename().contains(".")) {
            // filename 已经包含扩展名，提取出来
            return "";
        }
        // 默认使用 .md，但实际应该从文件名中判断
        // 由于从文件系统读取时，filename 已经包含扩展名，所以这里返回空字符串
        return "";
    }

    /**
     * 从路径中获取文件扩展名
     */
    private String getFileExtensionFromPath(String path) {
        if (path != null && path.contains(".")) {
            return path.substring(path.lastIndexOf(".") + 1);
        }
        return "md"; // 默认
    }

    @Override
    public int getItemViewType(int position) {
        // 第一个位置是 Header（如果有子分类）
        if (hasHeader() && position == 0) {
            return VIEW_TYPE_HEADER;
        }

        int actualPosition = hasHeader() ? position - 1 : position;
        if (mLaw == null || mLaw.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        }
        return VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        int headerCount = hasHeader() ? 1 : 0;
        if (mLaw == null || mLaw.isEmpty()) {
            return headerCount + 1; // Header + Empty
        }
        return headerCount + mLaw.size(); // Header + Items
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        ChipGroup chipGroup;
        HorizontalScrollView scrollView;

        @SuppressLint("ClickableViewAccessibility")
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            chipGroup = itemView.findViewById(R.id.chip_group_subcategory);
            scrollView = itemView.findViewById(R.id.scroll_view_chip_group);

            // 拦截触摸事件，防止传递给 ViewPager2
            if (scrollView != null) {
                scrollView.setOnTouchListener((v, event) -> {
                    // 当在 header 上滑动时，阻止父 View 拦截事件
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                        case android.view.MotionEvent.ACTION_MOVE:
                            // 请求父 View 不要拦截触摸事件
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            break;
                        case android.view.MotionEvent.ACTION_UP:
                        case android.view.MotionEvent.ACTION_CANCEL:
                            // 释放时允许父 View 拦截
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                            break;
                    }
                    // 让 HorizontalScrollView 处理触摸事件
                    return false;
                });
            }
        }
    }

    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.title);
        }
    }
}
