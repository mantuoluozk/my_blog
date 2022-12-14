package com.mszlu.blog.service;

import com.mszlu.blog.vo.CategoryVO;
import com.mszlu.blog.vo.Result;

public interface CategoryService {
    CategoryVO findCategoryById(Long categoryId);

    Result findAll();

    Result findAllDetail();

    Result categoryDetailById(Long id);
}
