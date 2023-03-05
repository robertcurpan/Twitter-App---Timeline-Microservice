package com.twitterapp.timelinemicroservice.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode @ToString
public class Tweet implements Serializable {

    private String tweetId;
    private Integer userId;
    private String username;
    private String text;
    private Date timestamp;
    private Integer likesCount;
    private Integer sharesCount;
}