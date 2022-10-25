package com.mszlu.blog.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class LoginUserVO {

//    @JsonSerialize(using = ToStringSerializer.class)
    private String id;

    private String account;

    private String nickname;

    private String avatar;

}
