package com.mszlu.blog.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CommentVO {
    // 防止前端 精度损失 把id转为string
    // 分布式id 比较长，传到前端 会有精度损失，必须转为string类型 进行传输，就不会有问题了
//    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    private UserVO author;

    private String content;

    private List<CommentVO> childrens;

//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    private Date createDate;
    private String createDate;

    private Integer level;

    private UserVO toUser;
}
