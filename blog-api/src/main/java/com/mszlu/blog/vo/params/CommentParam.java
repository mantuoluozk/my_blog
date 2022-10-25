package com.mszlu.blog.vo.params;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class CommentParam {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long articleId;

    private String content;

    private Long parent;

    private Long toUserId;
}
