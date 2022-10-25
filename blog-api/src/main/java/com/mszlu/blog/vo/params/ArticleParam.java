package com.mszlu.blog.vo.params;

import com.mszlu.blog.vo.CategoryVO;
import com.mszlu.blog.vo.TagVO;
import lombok.Data;

import java.util.List;

@Data
public class ArticleParam {

    private Long id;

    private ArticleBodyParam body;

    private CategoryVO category;

    private String summary;

    private List<TagVO> tags;

    private String title;

    private String search;
}
