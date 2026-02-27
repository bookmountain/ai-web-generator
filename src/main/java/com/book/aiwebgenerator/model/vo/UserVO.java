package com.book.aiwebgenerator.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class UserVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userProfile;
    private String userRole;
    private LocalDateTime createTime;

}
