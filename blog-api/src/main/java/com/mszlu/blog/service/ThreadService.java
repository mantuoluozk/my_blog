package com.mszlu.blog.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mszlu.blog.dao.mapper.ArticleMapper;
import com.mszlu.blog.dao.pojo.Article;
import com.mszlu.blog.vo.ArticleVO;
import com.mszlu.blog.vo.Result;
import com.mszlu.blog.vo.params.PageParams;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@Transactional
public class ThreadService {

    @Resource
    private ArticleMapper articleMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @PostConstruct
    public void initViewCount(){
        //为了 保证 启动项目的时候，redis中的浏览量 如果没有，读取数据库的数据，进行初始化
        //便于更新的时候 自增
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<>());
        for (Article article : articles) {
            String viewCountStr = (String) redisTemplate.opsForHash().get("view_count", String.valueOf(article.getId()));
            if (viewCountStr == null){
                //初始化
                redisTemplate.opsForHash().put("view_count", String.valueOf(article.getId()),String.valueOf(article.getViewCounts()));
            }
        }
    }
    @PostConstruct
    public void initCommentCount(){
        //为了 保证 启动项目的时候，redis中的浏览量 如果没有，读取数据库的数据，进行初始化
        //便于更新的时候 自增
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<>());
        for (Article article : articles) {
            String viewCountStr = (String) redisTemplate.opsForHash().get("comment_count", String.valueOf(article.getId()));
            if (viewCountStr == null){
                //初始化
                redisTemplate.opsForHash().put("comment_count", String.valueOf(article.getId()),String.valueOf(article.getCommentCounts()));
            }
        }
    }
    // 此操作在线程池中进行 不会影响原有的主线程
    @Async("taskExecutor")
    public void updateArticleViewCount(ArticleMapper articleMapper, Article article) {
//        Article articleUpdate = new Article();
//        // 阅读次数加1
//        articleUpdate.setViewCounts(article.getViewCounts() + 1);
//        LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
//        // 根据文章id更新
//        queryWrapper.eq(Article::getId, article.getId());
//        // 保证线程安全
//        queryWrapper.eq(Article::getViewCounts, article.getViewCounts());
//        // update article set view_count=100 where view_count=99 and id=11
//        // 执行更新操作
//        articleMapper.update(articleUpdate, queryWrapper);


        //采用redis进行浏览量的增加
        //hash结构 key 浏览量标识 field 文章id  后面1 表示自增加1
        redisTemplate.opsForHash().increment("view_count",String.valueOf(article.getId()),1);

        //定时任务在ViewCountHandler中
        //还有一种方式是，redis自增之后，直接发送消息到消息队列中，由消息队列进行消费 来同步数据库，比定时任务要好一些
    }
    @Async("taskExecutor")
    public void updateArticleCommentCount(Long article_id){
        redisTemplate.opsForHash().increment("comment_count",String.valueOf(article_id),1);
        deleteListAndViewRedis(article_id);//删除当前文章缓存和文章列表缓存
    }

    /**
     * 新文章发布时调用，否则首页列表使用的是缓存中的，不会显示刚发布的文章
     */
    @Async("taskExecutor")
    public void deleteArticleList(){
        Set<String> keys = redisTemplate.keys("listArticle*");
        keys.forEach(s -> {
            redisTemplate.delete(s);
            log.info("删除了文章列表的缓存:{}",s);
        });
        Set<String> keys2 = redisTemplate.keys("news_article*");
        keys2.forEach(s -> {
            redisTemplate.delete(s);
            log.info("删除了最新文章列表的缓存:{}",s);
        });
    }

    /**
     * 有评论和浏览调用
     * 1.直接删除缓存文章列表缓存（因为文章列表的参数从前端传过来，我们不知道，所以直接删除）
     * 2.只删除**当前**文章详情页缓存
     */

    @Async("taskExecutor")
    public void deleteListAndViewRedis(Long articleId){
        String params = DigestUtils.md5Hex(articleId.toString());
        String redisKey = "view_article::ArticleController::findArticleById::"+params;
        redisTemplate.delete(redisKey);
        log.info("删除了当前文章的缓存:{}",redisKey);

        Set<String> keys = redisTemplate.keys("listArticle*");
        keys.forEach(s -> {
            redisTemplate.delete(s);
            log.info("删除了文章列表的缓存:{}",s);
        });
    }
}
