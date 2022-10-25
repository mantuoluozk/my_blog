package com.mszlu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mszlu.blog.dao.dos.Archives;
import com.mszlu.blog.dao.mapper.ArticleBodyMapper;
import com.mszlu.blog.dao.mapper.ArticleMapper;
import com.mszlu.blog.dao.mapper.ArticleTagMapper;
import com.mszlu.blog.dao.pojo.Article;
import com.mszlu.blog.dao.pojo.ArticleBody;
import com.mszlu.blog.dao.pojo.ArticleTag;
import com.mszlu.blog.dao.pojo.SysUser;
import com.mszlu.blog.service.*;
import com.mszlu.blog.utils.UserThreadLocal;
import com.mszlu.blog.vo.*;
import com.mszlu.blog.vo.params.ArticleParam;
import com.mszlu.blog.vo.params.PageParams;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ArticleServiceImpl implements ArticleService {
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private TagService tagService;
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Override
    public Result listArticle(PageParams pageParams) {
        Page<Article> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
        IPage<Article> articleIPage = articleMapper.listArticle(
                page,
                pageParams.getCategoryId(),
                pageParams.getTagId(),
                pageParams.getYear(),
                pageParams.getMonth());
        List<Article> records = articleIPage.getRecords();
        for (Article record : records) {
            String viewCount = (String) redisTemplate.opsForHash().get("view_count", String.valueOf(record.getId()));
            String commentCount = (String) redisTemplate.opsForHash().get("comment_count", String.valueOf(record.getId()));
            if (viewCount != null){
                record.setViewCounts(Integer.parseInt(viewCount));
            }
            if (commentCount != null){
                record.setCommentCounts(Integer.parseInt(commentCount));
            }
        }
        return Result.success(copyList(records, true, true));
    }

//    @Override
//    public Result listArticle(PageParams pageParams) {
//        /**
//         * 1.分页查询article数据库表
//         */
//        Page<Article> page = new Page<>(pageParams.getPage(), pageParams.getPageSize());
//        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
//        if(pageParams.getCategoryId() != null){
//            // and category_id=#{categoryId}
//            queryWrapper.eq(Article::getCategoryId, pageParams.getCategoryId());
//        }
//        List<Long> articleIdList = new ArrayList<>();
//        if(pageParams.getTagId() != null){
//            // article 中并没有tag字段 因为一个文章有多个tag
//            // article_tag   article_id 1 : n tag_id
//            LambdaQueryWrapper<ArticleTag> articleTagLambdaQueryWrapper = new LambdaQueryWrapper<>();
//            articleTagLambdaQueryWrapper.eq(ArticleTag::getTagId, pageParams.getTagId());
//            List<ArticleTag> articleTags = articleTagMapper.selectList(articleTagLambdaQueryWrapper);
//            for(ArticleTag articleTag : articleTags){
//                articleIdList.add(articleTag.getArticleId());
//            }
//            if(articleIdList.size() > 0){
//                // and id in (1, 2, 3)
//                queryWrapper.in(Article::getId, articleIdList);
//            }
//        }
//        // 置顶排序 时间倒序 ;
//        queryWrapper.orderByDesc(Article::getWeight, Article::getCreateDate);
//        Page<Article> articlePage = articleMapper.selectPage(page, queryWrapper);
//        //System.out.println(articlePage);
//        List<Article> articleList = articlePage.getRecords();
//        // 将Article转换为ArticleVO
//        List<ArticleVO> articleVOList = copyList(articleList, true, true);
//        return Result.success(articleVOList);
//    }

    @Override
    public Result hotArticle(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getViewCounts);
        queryWrapper.select(Article::getId, Article::getTitle);
        queryWrapper.last("limit "+limit);
        //select id, title from article order by view_counts desc limit 5
        List<Article> articles = articleMapper.selectList(queryWrapper);
        return Result.success(copyList(articles, false, false));
    }

    @Override
    public Result newArticles(int limit) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getCreateDate);
        queryWrapper.select(Article::getId, Article::getTitle);
        queryWrapper.last("limit "+limit);
        //select id, title from article order by create_date desc limit 5
        List<Article> articles = articleMapper.selectList(queryWrapper);
        return Result.success(copyList(articles, false, false));
    }

    @Override
    public Result listArchives() {
        List<Archives> archivesList = articleMapper.listArchives();
        return Result.success(archivesList);
    }

    @Autowired
    private ThreadService threadService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Result findArticleById(Long articleId) {
        /**
         * 1.根据id查询文章信息
         * 2.根据bodyId和 categoryid 去做关联查询
         */
        Article article = this.articleMapper.selectById(articleId);
        ArticleVO articleVO = copy(article, true, true, true, true);
        // 查看完文章增加阅读数
        // 数据库在做更新操作时会写锁，阻塞其他的操作，性能会降低，增加该接口此次的耗时，一旦更新失败不能影响查看文章的操作
        // 线程池，可以把更新操作，扔到线程池中去执行，与主线程取消联系
        // threadService.updateArticleViewCount(articleMapper, article); 该操作放在cache中，这样每次走缓存代码就会加1

        String viewCount = (String) redisTemplate.opsForHash().get("view_count", String.valueOf(articleId));
        String commentCount = (String) redisTemplate.opsForHash().get("comment_count", String.valueOf(articleId));

        if (viewCount != null){
            articleVO.setViewCounts(Integer.parseInt(viewCount));
        }
        if (commentCount != null){
            articleVO.setCommentCounts(Integer.parseInt(commentCount));
        }
        return Result.success(articleVO);
    }

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public Result publish(ArticleParam articleParam) {
        // 此接口加入到登录拦截当中
        SysUser sysUser = UserThreadLocal.get();
        /**
         * 1.发布文章 目的 构建Article对象
         * 2.作者id 当前登录用户
         * 3.标签  将标签加入到关联列表当中
         * 4.body 内容存储 article bodyId
         */
        Article article = new Article();
        boolean isEdit = false;
        if(articleParam.getId() != null){ // 修改文章
            article = new Article();
            article.setId(articleParam.getId());
            article.setTitle(articleParam.getTitle());
            article.setSummary(articleParam.getSummary());
            article.setCategoryId(Long.parseLong(articleParam.getCategory().getId()));
            articleMapper.updateById(article);
            isEdit = true;
        }else{                             // 发布新文章
            article = new Article();
            article.setAuthorId(sysUser.getId());
            article.setWeight(Article.Article_Common);
            article.setViewCounts(0);
            article.setTitle(articleParam.getTitle());
            article.setSummary(articleParam.getSummary());
            article.setCommentCounts(0);
            article.setCreateDate(System.currentTimeMillis());
            article.setCategoryId(Long.parseLong(articleParam.getCategory().getId()));
            //插入之后会生成文章id
            this.articleMapper.insert(article);
        }

        //tag
        /**
         * 这种写法有问题，每次循环都会删除上一轮删除的tag,导致最后只有一个最后一个tag
         */
//        List<TagVO> tags = articleParam.getTags();
//        if(tags != null){
//            for (TagVO tag : tags){
//                Long articleId = article.getId();
//                if(isEdit){
//                    //先删除
//                    LambdaQueryWrapper<ArticleTag> queryWrapper = Wrappers.lambdaQuery();
//                    queryWrapper.eq(ArticleTag::getArticleId,articleId);
//                    articleTagMapper.delete(queryWrapper);
//                }
//                ArticleTag articleTag = new ArticleTag();
//                articleTag.setTagId(Long.parseLong(tag.getId()));
//                articleTag.setArticleId(articleId);
//                articleTagMapper.insert(articleTag);
//            }
//        }
        List<TagVO> tags = articleParam.getTags();
        if(tags != null){
            Long articleId = article.getId();
            if(isEdit){
            //先删除
                LambdaQueryWrapper<ArticleTag> queryWrapper = Wrappers.lambdaQuery();
                queryWrapper.eq(ArticleTag::getArticleId,articleId);
                articleTagMapper.delete(queryWrapper);
            }
            for (TagVO tag : tags){
                ArticleTag articleTag = new ArticleTag();
                articleTag.setTagId(Long.parseLong(tag.getId()));
                articleTag.setArticleId(articleId);
                articleTagMapper.insert(articleTag);
            }
        }

        // body 文章内容
        if(isEdit){
            ArticleBody articleBody = new ArticleBody();
            articleBody.setArticleId(article.getId());
            articleBody.setContent(articleParam.getBody().getContent());
            articleBody.setContentHtml(articleParam.getBody().getContentHtml());
            LambdaUpdateWrapper<ArticleBody> updateWrapper = Wrappers.lambdaUpdate();
            updateWrapper.eq(ArticleBody::getArticleId,article.getId());
            articleBodyMapper.update(articleBody, updateWrapper);
        }else{
            ArticleBody articleBody = new ArticleBody();
            articleBody.setArticleId(article.getId());
            articleBody.setContent(articleParam.getBody().getContent());
            articleBody.setContentHtml(articleParam.getBody().getContentHtml());
            articleBodyMapper.insert(articleBody);//插入后才能得到id
            article.setBodyId(articleBody.getId());
            articleMapper.updateById(article);//文章先插入再更新
        }

        Map<String, String> map = new HashMap<>();
        map.put("id", article.getId().toString());
        if(isEdit){
            // 发送一条消息给rocketmq 当前文章更新了，更新一下缓存吧
            ArticleMessage articleMessage = new ArticleMessage();
            articleMessage.setArticleId(article.getId());
            rocketMQTemplate.convertAndSend("blog-update-article",articleMessage);
            System.out.println("消息发送成功");
        }else{
            threadService.deleteArticleList();//删除redis中listArticles缓存
        }
        return Result.success(map);
    }

    @Override
    public Result searchArticle(String search) {
        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Article::getViewCounts);
        queryWrapper.select(Article::getId,Article::getTitle);
        queryWrapper.like(Article::getTitle,search);
        //select id,title from article order by view_counts desc limit 5
        List<Article> articles = articleMapper.selectList(queryWrapper);

        return Result.success(copyList(articles,false,false));
    }

    private List<ArticleVO> copyList(List<Article> records, boolean isTag, boolean isAuthor) {
        List<ArticleVO> articleVoList = new ArrayList<>();
        for (Article record : records) {
            articleVoList.add(copy(record, isTag, isAuthor, false,false));
        }
        return articleVoList;
    }

    private List<ArticleVO> copyList(List<Article> records, boolean isTag, boolean isAuthor, boolean isBody, boolean isCategory) {
        List<ArticleVO> articleVoList = new ArrayList<>();
        for (Article record : records) {
            articleVoList.add(copy(record, isTag, isAuthor, isBody, isCategory));
        }
        return articleVoList;
    }

    @Autowired
    private CategoryService categoryService;
    private ArticleVO copy(Article article, boolean isTag, boolean isAuthor, boolean isBody, boolean isCategory) {
        ArticleVO articleVO = new ArticleVO();
        articleVO.setId(String.valueOf(article.getId()));
        BeanUtils.copyProperties(article, articleVO);
        articleVO.setCreateDate(new DateTime(article.getCreateDate()).toString("yyyy-MM-dd HH:mm"));

        //并不是所有接口都需要标签，作者信息
        if(isTag){
            Long articleId = article.getId();
            articleVO.setTags(tagService.findTagsByArticleId(articleId));
        }
        if(isAuthor){
            Long authorId = article.getAuthorId();
            SysUser sysUser = sysUserService.findUserById(authorId);
            UserVO userVO = new UserVO();
            userVO.setAvatar(sysUser.getAvatar());
            userVO.setNickname(sysUser.getNickname());
            userVO.setId(sysUser.getId().toString());
            articleVO.setAuthor(userVO);
        }
        if(isBody){
            Long bodyId = article.getBodyId();
            articleVO.setBody(findArticleBodyById(bodyId));
        }
        if(isCategory){
            Long categoryId = article.getCategoryId();
            articleVO.setCategory(categoryService.findCategoryById(categoryId));
        }
        return articleVO;
    }

    @Autowired
    private ArticleBodyMapper articleBodyMapper;
    private ArticleBodyVO findArticleBodyById(Long bodyId) {
        ArticleBody articleBody = articleBodyMapper.selectById(bodyId);
        ArticleBodyVO articleBodyVO = new ArticleBodyVO();
        articleBodyVO.setContent(articleBody.getContent());
        return articleBodyVO;
    }
}
