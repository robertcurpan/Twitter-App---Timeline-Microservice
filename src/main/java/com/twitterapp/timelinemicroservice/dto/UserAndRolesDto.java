package com.twitterapp.timelinemicroservice.dto;

import com.twitterapp.timelinemicroservice.extrastructures.RolesEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserAndRolesDto {

    private Integer userId;
    private String username;
    private List<RolesEnum> userRoles;
}
