package com.twitterapp.timelinemicroservice.dto;

import com.twitterapp.extrastructures.RolesEnum;
import com.twitterapp.extrastructures.UserTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserAndRolesDto {

    private Integer userId;
    private String username;
    private UserTypeEnum userType;
    private List<RolesEnum> userRoles;
}
