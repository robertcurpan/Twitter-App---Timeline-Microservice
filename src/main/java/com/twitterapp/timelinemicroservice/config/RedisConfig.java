package com.twitterapp.timelinemicroservice.config;

import com.twitterapp.timelinemicroservice.entity.EditedTweet;
import com.twitterapp.timelinemicroservice.entity.UserInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName("localhost");
        config.setPort(6379);

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, UserInfo> redisTemplateForUserInfo() {
        RedisTemplate<String, UserInfo> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(UserInfo.class));

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, String> redisTemplateForDeletedTweets() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, EditedTweet> redisTemplateForEditedTweets() {
        RedisTemplate<String, EditedTweet> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(EditedTweet.class));

        return redisTemplate;
    }

    @Bean
    public HashOperations<String, String, UserInfo> usersInfoHash() { return redisTemplateForUserInfo().opsForHash(); }

    @Bean
    public HashOperations<String, String, String> deletedTweetsHash() { return redisTemplateForDeletedTweets().opsForHash(); }

    @Bean
    public HashOperations<String, String, EditedTweet> editedTweetsHash() { return redisTemplateForEditedTweets().opsForHash(); }
}
