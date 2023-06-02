package com.twitterapp.timelinemicroservice.repository;


import com.twitterapp.timelinemicroservice.entity.UserInfo;
import com.twitterapp.timelinemicroservice.extrastructures.UserTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RedisUsersInfoRepository {

    private static final String HASH_KEY = "UsersInfo";
    private static final String USER_INFO_KEY_PREFIX = "user";

    @Autowired
    private HashOperations<String, String, UserInfo> usersInfoHash;


    public UserInfo getUserInfo(Integer userId) {
        String userInfoKey = USER_INFO_KEY_PREFIX + userId.toString();
        return usersInfoHash.get(HASH_KEY, userInfoKey);
    }

    public void updateUserType(Integer userId, UserTypeEnum newUserType) {
        UserInfo userInfo = getUserInfo(userId);
        UserInfo newUserInfo = new UserInfo(newUserType, userInfo.getLastLogin());
        addUserInfo(userId, newUserInfo);
    }

    public void addUserInfo(Integer userId, UserInfo userInfo) {
        String userInfoKey = USER_INFO_KEY_PREFIX + userId.toString();
        usersInfoHash.put(HASH_KEY, userInfoKey, userInfo);
    }
}
