package com.example.userservice.dto;

import com.example.userservice.jpa.UserEntity;
import com.example.userservice.vo.RequestUser;
import com.example.userservice.vo.ResponseOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@ToString
public class UserDto {
    private String email;
    private String name;
    private String pwd;
    private String userId;
    private Date createdAt;

    private List<ResponseOrder> orders = new ArrayList<>();

    private String encryptedPwd;

    public UserDto(RequestUser user) {
        this.email = user.getEmail();
        this.name = user.getName();
        this.pwd = user.getPwd();
    }

    public UserDto(UserEntity userEntity) {
        this.email = userEntity.getEmail();
        this.name = userEntity.getName();
        this.userId = userEntity.getUserId();
    }
}
