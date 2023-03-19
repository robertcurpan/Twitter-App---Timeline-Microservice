package com.twitterapp.timelinemicroservice.service;


import com.twitterapp.extrastructures.UserTypeEnum;
import com.twitterapp.timelinemicroservice.entity.Tweet;
import com.twitterapp.timelinemicroservice.entity.UserInfo;
import com.twitterapp.timelinemicroservice.exception.exceptions.GenericException;
import com.twitterapp.timelinemicroservice.exception.exceptions.PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException;
import com.twitterapp.timelinemicroservice.repository.RedisTweetsRepository;
import com.twitterapp.timelinemicroservice.repository.RedisUsersInfoRepository;
import com.twitterapp.timelinemicroservice.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class TimelineService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RedisUsersInfoRepository redisUsersInfoRepository;
    @Autowired
    private RedisTweetsRepository redisTweetsRepository;


    public List<Tweet> getUserTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) throws GenericException {
        return getTweetsPageFromDatabaseForUserTimeline(jws, userId, pageNumber, pageSize);
    }

    private List<Tweet> getTweetsPageFromDatabaseForUserTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) throws GenericException {
        String url = "http://localhost:8083/api/tweets/" + userId.toString() + "/forUserTimeline?pageNumber={pageNumber}&pageSize={pageSize}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, Integer> requestParameters = new HashMap<>();
        requestParameters.put("pageNumber", pageNumber);
        requestParameters.put("pageSize", pageSize);

        try {
            ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
            List<Tweet> tweetsForHomeTimeline = response.getBody();
            return tweetsForHomeTimeline;
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }

    }

    public List<Tweet> getHomeTimeline(String jws, Integer userId, LocalDateTime timestamp, String lastDocumentSeenId, Integer pageNumber, Integer pageSize) throws PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException, GenericException {
        Date timestampDate = DateTimeUtil.convertLocalDateTimeToDate(timestamp);
        UserTypeEnum userType = redisUsersInfoRepository.getUserInfo(userId).getUserType();
        if(userType == null) {
            redisUsersInfoRepository.updateUserType(userId, UserTypeEnum.PASSIVE);
            userType = UserTypeEnum.PASSIVE;
        }

        if(userType == UserTypeEnum.ACTIVE || userType == UserTypeEnum.LIVE) {
            return getHomeTimelineForActiveOrLiveUser(jws, userId, userType, timestampDate, lastDocumentSeenId, pageNumber, pageSize);
        } else if (userType == UserTypeEnum.PASSIVE) {
            return getHomeTimelineForPassiveUser(jws, userId, pageNumber, pageSize);
        }

        return new ArrayList<>();
    }

    public List<Tweet> getHomeTimelineForActiveOrLiveUser(String jws, Integer userId, UserTypeEnum userType, Date timestampDate, String lastDocumentSeenId, Integer pageNumber, Integer pageSize) throws GenericException {
        try {
            return getTweetsPageFromCacheForHomeTimeline(jws, userId, pageNumber, pageSize);
        } catch (HttpClientErrorException exception) {
            HttpStatus statusCode = exception.getStatusCode();
            if(statusCode == HttpStatus.NOT_FOUND) {
                try {
                    List<Tweet> bulkOfTweetsFromDatabase = getBulkOfTweetsFromDatabaseForHomeTimeline(jws, userId, timestampDate, lastDocumentSeenId);
                    addTweetsToCacheReadListForUser(jws, userId, bulkOfTweetsFromDatabase);
                    return getTweetsPageFromCacheForHomeTimeline(jws, userId, pageNumber, pageSize);
                } catch (HttpClientErrorException newException) {
                    String exceptionMessage = newException.getResponseBodyAsString();
                    throw new GenericException(exceptionMessage);
                }
            }
        } finally {
            if(userType == UserTypeEnum.ACTIVE) {
                changeUserType(userId, UserTypeEnum.LIVE);
            }
        }

        return new ArrayList<>();
    }

    public List<Tweet> getHomeTimelineForPassiveUser(String jws, Integer userId, Integer pageNumber, Integer pageSize) throws GenericException, PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException {
        if(pageNumber != 0) throw new PassiveUserCanOnlyRetrieveTheFirstPageOfTimelineException();

        try {
            List<Tweet> firstBulkOfTweetsFromDatabase = getFirstBulkOfTweetsFromDatabaseForHomeTimeline(jws, userId);
            addTweetsToCacheWriteListForUser(jws, userId, firstBulkOfTweetsFromDatabase);
            return getTweetsPageFromCacheForHomeTimeline(jws, userId, 0, pageSize);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        } finally {
            changeUserType(userId, UserTypeEnum.LIVE);
        }
    }

    public List<Tweet> getTweetsPageFromCacheForHomeTimeline(String jws, Integer userId, Integer pageNumber, Integer pageSize) {
        String url = "http://localhost:8084/api/fanout/" + userId.toString() + "/?pageNumber={pageNumber}&pageSize={pageSize}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<List<Tweet>> entity = new HttpEntity<>(headers);

        Map<String, Integer> requestParameters = new HashMap<>();
        requestParameters.put("pageNumber", pageNumber);
        requestParameters.put("pageSize", pageSize);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
        List<Tweet> listOfTweets = response.getBody();
        List<Tweet> tweetsForHomeTimeline = new ArrayList<>();

        if(listOfTweets != null) {
            for(Tweet tweet : listOfTweets) {
                if(!redisTweetsRepository.isTweetDeleted(tweet.getTweetId())) {
                    if(redisTweetsRepository.isTweetEdited(tweet.getTweetId())) {
                        tweet.setText(redisTweetsRepository.getNewTextForEditedTweet(tweet.getTweetId()));
                    }
                    tweetsForHomeTimeline.add(tweet);
                }
            }
        }

        return tweetsForHomeTimeline;
    }

    public List<Tweet> getBulkOfTweetsFromDatabaseForHomeTimeline(String jws, Integer userId, Date timestampDate, String lastTweetSeenId) {
        String url = "http://localhost:8083/api/tweets/" + userId.toString() + "/forHomeTimeline?timestamp={timestamp}&lastDocumentSeenId={lastDocumentSeenId}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String lastDocumentSeenId = Objects.equals(lastTweetSeenId, "0") ? "" : lastTweetSeenId;
        Date dateTimestamp = Objects.equals(lastTweetSeenId, "0") ? new Date() : timestampDate;
        LocalDateTime timestamp = DateTimeUtil.convertDateToLocalDateTime(dateTimestamp);

        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("timestamp", timestamp);
        requestParameters.put("lastDocumentSeenId", lastDocumentSeenId);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
        List<Tweet> tweetsForHomeTimeline = response.getBody();
        return tweetsForHomeTimeline;
    }

    public List<Tweet> getFirstBulkOfTweetsFromDatabaseForHomeTimeline(String jws, Integer userId) {
        String url = "http://localhost:8083/api/tweets/" + userId.toString() + "/forHomeTimeline?timestamp={timestamp}&lastDocumentSeenId={lastDocumentSeenId}";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        LocalDateTime timestamp = DateTimeUtil.convertDateToLocalDateTime(new Date());
        String lastDocumentSeenId = "";

        Map<String, Object> requestParameters = new HashMap<>();
        requestParameters.put("timestamp", timestamp);
        requestParameters.put("lastDocumentSeenId", lastDocumentSeenId);

        ResponseEntity<List<Tweet>> response = restTemplate.exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Tweet>>(){}, requestParameters);
        List<Tweet> tweetsForHomeTimeline = response.getBody();
        return tweetsForHomeTimeline;
    }

    public void addTweetsToCacheWriteListForUser(String jws, Integer userId, List<Tweet> tweets) throws GenericException {
        String url = "http://localhost:8084/api/fanout/" + userId.toString() + "/writeList";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<List<Tweet>> entity = new HttpEntity<>(tweets, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }
    }

    public void addTweetsToCacheReadListForUser(String jws, Integer userId, List<Tweet> tweets) throws GenericException {
        String url = "http://localhost:8084/api/fanout/" + userId.toString() + "/readList";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + jws);
        HttpEntity<List<Tweet>> entity = new HttpEntity<>(tweets, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }
    }

    private void changeUserType(Integer userId, UserTypeEnum newUserType) throws GenericException {
        try {
            redisUsersInfoRepository.updateUserType(userId, newUserType);
        } catch (HttpClientErrorException exception) {
            String exceptionMessage = exception.getResponseBodyAsString();
            throw new GenericException(exceptionMessage);
        }
    }

    public void addUserInfo(Integer userId) {
        redisUsersInfoRepository.addUserInfo(userId, new UserInfo(UserTypeEnum.PASSIVE, null));
    }

    public UserInfo getUserInfo(Integer userId) {
        return redisUsersInfoRepository.getUserInfo(userId);
    }

}
