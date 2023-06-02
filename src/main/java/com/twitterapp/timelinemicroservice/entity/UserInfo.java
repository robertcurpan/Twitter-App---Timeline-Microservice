package com.twitterapp.timelinemicroservice.entity;

import com.twitterapp.timelinemicroservice.extrastructures.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;


@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserInfo implements Serializable {

    private UserTypeEnum userType;
    private Date lastLogin;
}
