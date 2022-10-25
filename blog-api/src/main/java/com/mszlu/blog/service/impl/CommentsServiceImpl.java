package com.mszlu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mszlu.blog.dao.mapper.ArticleMapper;
import com.mszlu.blog.dao.mapper.CommentMapper;
import com.mszlu.blog.dao.pojo.Article;
import com.mszlu.blog.dao.pojo.Comment;
import com.mszlu.blog.dao.pojo.SysUser;
import com.mszlu.blog.service.CommentsService;
import com.mszlu.blog.service.SysUserService;
import com.mszlu.blog.service.ThreadService;
import com.mszlu.blog.utils.UserThreadLocal;
import com.mszlu.blog.vo.CommentVO;
import com.mszlu.blog.vo.Result;
import com.mszlu.blog.vo.UserVO;
import com.mszlu.blog.vo.params.CommentParam;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class CommentsServiceImpl implements CommentsService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public Result commentsByArticleId(Long id) {
        /**
         * 1. 根据文章id 查询 评论列表 从 comment 表中查询
         * 2. 根据作者的id 查询作者的信息
         * 3. 判断如果level=1 则去查询是否有子评论
         * 4. 如果有 根据评论id 进行查询 （parent_id）
         */
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        // 设置查询条件
        queryWrapper.eq(Comment::getArticleId, id);
        queryWrapper.eq(Comment::getLevel, 1);
        queryWrapper.orderByDesc(Comment::getCreateDate);
        // 执行查询
        List<Comment> commentsList = commentMapper.selectList(queryWrapper);
        List<CommentVO> commentVOList = copyList(commentsList);
        return Result.success(commentVOList);
    }
    @Autowired
    private ThreadService threadService;
    @Override
    public Result comment(CommentParam commentParam) {
        SysUser sysUser = UserThreadLocal.get();
        Comment comment = new Comment();
        comment.setArticleId(commentParam.getArticleId());
        comment.setAuthorId(sysUser.getId());
        comment.setContent(commentParam.getContent());
        comment.setCreateDate(System.currentTimeMillis());
        Long parent = commentParam.getParent();
        if (parent == null || parent == 0) {
            comment.setLevel(1);
        }else{
            comment.setLevel(2);
        }
        comment.setParentId(parent == null ? 0 : parent);
        Long toUserId = commentParam.getToUserId();
        comment.setToUid(toUserId == null ? 0 : toUserId);
        this.commentMapper.insert(comment);
//        UpdateWrapper<Article> updateWrapper = Wrappers.update();
//        updateWrapper.eq("id",comment.getArticleId());
//        updateWrapper.setSql(true,"comment_counts=comment_counts+1");
//        this.articleMapper.update(null,updateWrapper); // 增加评论数量
//        改用redis缓存自增+定时任务
        threadService.updateArticleCommentCount(commentParam.getArticleId()); //自增redis缓存中的评论数量


        CommentVO commentVO = copy(comment);
        return Result.success(commentVO);//这里传参数是为了刷新评论
    }

    private List<CommentVO> copyList(List<Comment> commentsList) {
        List<CommentVO> commentVOList = new ArrayList<>();
        for (Comment comment : commentsList) {
            commentVOList.add(copy(comment));
        }
        return commentVOList;
    }

    private CommentVO copy(Comment comment) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        commentVO.setId(String.valueOf(comment.getId()));
        Long authorId = comment.getAuthorId();
        UserVO author = this.sysUserService.findUserVOById(authorId);
        // 保存作者信息
        commentVO.setAuthor(author);
        // 查询子评论
        Integer level = comment.getLevel();
        if(level == 1){
            Long id = comment.getId();
            List<CommentVO> commentVOList = findCommentByParentId(id);
            commentVO.setChildrens(commentVOList);
        }
        // to User 给谁评论
        if(level > 1){
            Long toUid = comment.getToUid();
            UserVO toUserVO = this.sysUserService.findUserVOById(toUid);
            commentVO.setToUser(toUserVO);
        }
        return commentVO;
    }

    private List<CommentVO> findCommentByParentId(Long id) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, id);
        queryWrapper.eq(Comment::getLevel, 2);
        // 执行查询
        return copyList(commentMapper.selectList(queryWrapper));
    }
}
