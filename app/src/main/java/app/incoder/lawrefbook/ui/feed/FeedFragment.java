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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import app.incoder.lawrefbook.R;
import app.incoder.lawrefbook.sqlite.Sqlite3Dao;
import app.incoder.lawrefbook.storage.Category;
import app.incoder.lawrefbook.storage.Law;

/**
 * Feed
 *
 * @author : Jerry xu
 * @since : 2022/4/30 01:45
 */
public class FeedFragment extends Fragment {

    private static final String CATEGORY = "category";
    private Category mCategory;
    private List<Law> laws; // 所有法律数据（未过滤）
    private List<Law> filteredLaws; // 过滤后的法律数据
    private List<Category> mSubCategories; // 第二层分类
    private String mSelectedSubCategoryId; // 选中的第二层分类ID
    private FeedAdapter mAdapter;

    public FeedFragment() {
        // Required empty public constructor
    }

    public static FeedFragment newInstance(Category category) {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        args.putSerializable(CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    public void changeLawRef(List<Law> data) {
        // 更新所有法律数据
        laws = data;
        // 保存旧的过滤结果用于 DiffUtil
        List<Law> oldFilteredLaws = new ArrayList<>(filteredLaws != null ? filteredLaws : new ArrayList<>());
        // 根据选中的子分类过滤数据
        filterLawsBySubCategory();
        
        if (mAdapter == null) {
            mAdapter = new FeedAdapter(requireActivity());
            mAdapter.setSubCategorySelectedListener(subCategoryId -> {
                mSelectedSubCategoryId = subCategoryId;
                List<Law> oldLaws = new ArrayList<>(filteredLaws != null ? filteredLaws : new ArrayList<>());
                filterLawsBySubCategory();
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FeedDiffCallBack(filteredLaws, oldLaws));
                diffResult.dispatchUpdatesTo(mAdapter);
                mAdapter.setData(mCategory, filteredLaws);
            });
        } else {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FeedDiffCallBack(filteredLaws, oldFilteredLaws));
            diffResult.dispatchUpdatesTo(mAdapter);
        }
        mAdapter.setData(mCategory, filteredLaws);
    }

    /**
     * 根据选中的子分类过滤法律数据
     */
    private void filterLawsBySubCategory() {
        if (laws == null) {
            filteredLaws = new ArrayList<>();
            return;
        }

        if (mSubCategories == null || mSubCategories.isEmpty() || mSelectedSubCategoryId == null) {
            // 如果没有子分类，显示所有数据
            filteredLaws = laws;
        } else {
            // 根据选中的子分类ID过滤
            // 需要找到选中的子分类对应的 folder，然后匹配 law 的路径
            for (Category subCategory : mSubCategories) {
                if (subCategory.getId().equals(mSelectedSubCategoryId)) {
                    break;
                }
            }
            
            filteredLaws = new ArrayList<>();
            int selectedSubCategoryIdHash = mSelectedSubCategoryId.hashCode();
            for (Law law : laws) {
                // 通过比较 categoryId 的 hashCode 来判断是否属于选中的子分类
                // 在创建 Law 对象时，categoryId 被设置为分类 ID 的 hashCode
                if (law.getCategoryId() != null && law.getCategoryId() == selectedSubCategoryIdHash) {
                    filteredLaws.add(law);
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mCategory = (Category) getArguments().getSerializable(CATEGORY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView mRecyclerView = view.findViewById(R.id.rv_content);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // 加载第二层分类
        mSubCategories = Sqlite3Dao.getSubCategories(requireContext(), mCategory.getId());
        
        // 加载所有法律数据（包括所有子分类的法律）
        laws = Sqlite3Dao.getAllLawsByParentCategory(requireContext(), mCategory.getId());
        
        // 默认选中第一个子分类
        if (mSubCategories != null && !mSubCategories.isEmpty()) {
            mSelectedSubCategoryId = mSubCategories.get(0).getId();
        }
        
        // 初始化过滤后的数据
        filterLawsBySubCategory();
        
        mAdapter = new FeedAdapter(requireContext());
        mAdapter.setSubCategorySelectedListener(subCategoryId -> {
            mSelectedSubCategoryId = subCategoryId;
            List<Law> oldFilteredLaws = new ArrayList<>(filteredLaws != null ? filteredLaws : new ArrayList<>());
            filterLawsBySubCategory();
            // 更新适配器数据
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FeedDiffCallBack(filteredLaws, oldFilteredLaws));
            diffResult.dispatchUpdatesTo(mAdapter);
            mAdapter.setData(mCategory, filteredLaws);
        });
        
        // 设置子分类列表
        mAdapter.setSubCategories(mSubCategories);
        mAdapter.setData(mCategory, filteredLaws);
        
        MaterialDividerItemDecoration divider = new MaterialDividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL);
        mRecyclerView.addItemDecoration(divider);
        mRecyclerView.setAdapter(mAdapter);
    }
}